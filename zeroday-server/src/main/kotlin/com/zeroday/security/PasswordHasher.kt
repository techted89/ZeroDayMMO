package com.zeroday.security

import org.mindrot.jbcrypt.BCrypt

/**
 * Password hashing service. Stored hashes are produced with BCrypt so the
 * server can verify a login attempt without ever holding the plaintext.
 */
interface PasswordHasher {
    fun hash(plaintext: String): String
    fun verify(plaintext: String, storedHash: String): Boolean
}

class BcryptPasswordHasher : PasswordHasher {
    override fun hash(plaintext: String): String = BCrypt.hashpw(plaintext, BCrypt.gensalt(11))
    override fun verify(plaintext: String, storedHash: String): Boolean =
        runCatching { BCrypt.checkpw(plaintext, storedHash) }.getOrDefault(false)
}
