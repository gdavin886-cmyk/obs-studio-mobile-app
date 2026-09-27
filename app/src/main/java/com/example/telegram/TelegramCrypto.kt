package com.example.telegram

import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * MTProto 2.0 Cryptographic Primitives:
 * - SHA-1 and SHA-256
 * - AES-256 in IGE mode (Infinite Garble Extension)
 * - MTProto 2.0 Message Key and AES Key/IV derivation
 */
object TelegramCrypto {
    private val secureRandom = SecureRandom()

    fun sha1(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(data)
    }

    fun sha256(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data)
    }

    fun randomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    fun randomInt64(): Long {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        var result = 0L
        for (i in 0..7) {
            result = (result shl 8) or (bytes[i].toLong() and 0xFF)
        }
        return result
    }

    /**
     * Computes MTProto 2.0 msg_key:
     * msg_key_large = SHA256(substr(auth_key, 88+x, 32) + plaintext + random_padding)
     * msg_key = substr(msg_key_large, 8, 16)
     */
    fun computeMsgKey(authKey: ByteArray, plaintextWithPadding: ByteArray, isClientToServer: Boolean): ByteArray {
        val x = if (isClientToServer) 0 else 8
        val md = MessageDigest.getInstance("SHA-256")
        md.update(authKey, 88 + x, 32)
        md.update(plaintextWithPadding)
        val large = md.digest()
        val msgKey = ByteArray(16)
        System.arraycopy(large, 8, msgKey, 0, 16)
        return msgKey
    }

    /**
     * MTProto 2.0 AES Key & IV generation from auth_key and msg_key:
     * sha256_a = SHA256 (msg_key + substr (auth_key, x, 36))
     * sha256_b = SHA256 (substr (auth_key, 40+x, 16) + msg_key + substr (auth_key, 56+x, 16))
     * aes_key = substr (sha256_a, 0, 8) + substr (sha256_b, 8, 16) + substr (sha256_a, 24, 8)
     * aes_iv = substr (sha256_b, 0, 8) + substr (sha256_a, 8, 16) + substr (sha256_b, 24, 8)
     */
    fun computeAesKeyIv(authKey: ByteArray, msgKey: ByteArray, isClientToServer: Boolean): Pair<ByteArray, ByteArray> {
        val x = if (isClientToServer) 0 else 8
        val md = MessageDigest.getInstance("SHA-256")

        // sha256_a
        md.update(msgKey)
        md.update(authKey, x, 36)
        val sha256A = md.digest()
        md.reset()

        // sha256_b
        md.update(authKey, 40 + x, 16)
        md.update(msgKey)
        md.update(authKey, 56 + x, 16)
        val sha256B = md.digest()

        // aes_key
        val aesKey = ByteArray(32)
        System.arraycopy(sha256A, 0, aesKey, 0, 8)
        System.arraycopy(sha256B, 8, aesKey, 8, 16)
        System.arraycopy(sha256A, 24, aesKey, 24, 8)

        // aes_iv
        val aesIv = ByteArray(32)
        System.arraycopy(sha256B, 0, aesIv, 0, 8)
        System.arraycopy(sha256A, 8, aesIv, 8, 16)
        System.arraycopy(sha256B, 24, aesIv, 24, 8)

        return Pair(aesKey, aesIv)
    }

    /**
     * AES-256 IGE mode encryption:
     * c_i = m_i ^ AES_encrypt(m_{i-1} ^ c_{i-1})
     */
    fun aesIgeEncrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val blockSize = 16
        val paddedSize = if (plaintext.size % blockSize == 0) plaintext.size else ((plaintext.size / blockSize) + 1) * blockSize
        val input = if (paddedSize == plaintext.size) plaintext else {
            val p = ByteArray(paddedSize)
            System.arraycopy(plaintext, 0, p, 0, plaintext.size)
            p
        }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))

        val ciphertext = ByteArray(input.size)
        var iv1 = ByteArray(blockSize)
        var iv2 = ByteArray(blockSize)
        System.arraycopy(iv, 0, iv1, 0, blockSize)
        System.arraycopy(iv, blockSize, iv2, 0, blockSize)

        val block = ByteArray(blockSize)
        for (i in 0 until input.size step blockSize) {
            for (j in 0 until blockSize) {
                block[j] = (input[i + j].toInt() xor iv1[j].toInt()).toByte()
            }
            val encrypted = cipher.doFinal(block)
            for (j in 0 until blockSize) {
                ciphertext[i + j] = (encrypted[j].toInt() xor iv2[j].toInt()).toByte()
            }
            System.arraycopy(ciphertext, i, iv1, 0, blockSize)
            System.arraycopy(input, i, iv2, 0, blockSize)
        }
        return ciphertext
    }

    /**
     * AES-256 IGE mode decryption:
     * m_i = c_i ^ AES_decrypt(c_{i-1} ^ m_{i-1})
     */
    fun aesIgeDecrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val blockSize = 16
        val paddedSize = if (ciphertext.size % blockSize == 0) ciphertext.size else ((ciphertext.size / blockSize) + 1) * blockSize
        val input = if (paddedSize == ciphertext.size) ciphertext else {
            val p = ByteArray(paddedSize)
            System.arraycopy(ciphertext, 0, p, 0, ciphertext.size)
            p
        }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))

        val plaintext = ByteArray(input.size)
        var iv1 = ByteArray(blockSize)
        var iv2 = ByteArray(blockSize)
        System.arraycopy(iv, blockSize, iv1, 0, blockSize)
        System.arraycopy(iv, 0, iv2, 0, blockSize)

        val block = ByteArray(blockSize)
        for (i in 0 until input.size step blockSize) {
            for (j in 0 until blockSize) {
                block[j] = (input[i + j].toInt() xor iv1[j].toInt()).toByte()
            }
            val decrypted = cipher.doFinal(block)
            for (j in 0 until blockSize) {
                plaintext[i + j] = (decrypted[j].toInt() xor iv2[j].toInt()).toByte()
            }
            System.arraycopy(plaintext, i, iv1, 0, blockSize)
            System.arraycopy(input, i, iv2, 0, blockSize)
        }
        return plaintext
    }
}
