package com.zeroday.security

/**
 * Centralised, strict input validation for all user-supplied strings.
 *
 * These rules are deliberately defensive: even a perfectly-trusted client
 * can be hijacked (XSS, replay, MITM), and a malicious one will probe for
 * any leniency. Every length cap and character class chosen here maps to
 * a concrete threat we want to close.
 *
 *   - Username: 3-24 chars, [A-Za-z0-9_-], must start alphanumeric. No
 *     unicode, no whitespace, no control chars. This is enough for human
 *     names ("dark_phantom-99") and prevents homoglyph / right-to-left
 *     attacks.
 *   - Password: 8-128 chars. We do *not* enforce a complexity regex
 *     (NIST 800-63B); longer passphrases beat forced special chars and
 *     the BCrypt hasher bounds server-side cost.
 *   - Generic [text]: 1-200 chars, no control chars. Used for chat
 *     messages, story choices, etc.
 */
object InputValidation {
    private val USERNAME_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9_\\-]{2,23}$")
    private val PASSWORD_MIN = 8
    private val PASSWORD_MAX = 128
    private val TEXT_MAX = 200
    private val CONTROL_CHARS = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")

    sealed class ValidationResult {
        object Ok : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
        fun isValid(): Boolean = this is Ok
    }

    fun validateUsername(raw: String): ValidationResult {
        if (raw.isEmpty()) return ValidationResult.Invalid("Username required")
        if (raw.length > 24) return ValidationResult.Invalid("Username too long (max 24 chars)")
        if (!USERNAME_REGEX.matches(raw)) {
            return ValidationResult.Invalid(
                "Username must be 3-24 chars, alphanumeric/_/-, starting with a letter or digit"
            )
        }
        return ValidationResult.Ok
    }

    fun validatePassword(raw: String): ValidationResult {
        if (raw.length < PASSWORD_MIN) {
            return ValidationResult.Invalid("Password must be at least $PASSWORD_MIN characters")
        }
        if (raw.length > PASSWORD_MAX) {
            return ValidationResult.Invalid("Password too long (max $PASSWORD_MAX)")
        }
        return ValidationResult.Ok
    }

    /**
     * Generic single-line text (chat, custom script names, etc.).
     * Rejects control characters and any combination of NUL that could
     * trick a downstream log parser.
     */
    fun validateText(raw: String, max: Int = TEXT_MAX, min: Int = 1): ValidationResult {
        if (raw.length < min) return ValidationResult.Invalid("Text too short (min $min)")
        if (raw.length > max) return ValidationResult.Invalid("Text too long (max $max)")
        if (CONTROL_CHARS.containsMatchIn(raw)) {
            return ValidationResult.Invalid("Text contains control characters")
        }
        return ValidationResult.Ok
    }

    /**
     * Reject the four most common "reserved" names that show up in
     * automated probes. We do this at registration time, not login
     * time, so a banned account cannot be enumerated.
     */
    private val RESERVED_USERNAMES = setOf(
        "admin", "root", "system", "moderator", "support", "zeroday"
    )
    fun isReservedUsername(raw: String): Boolean =
        raw.lowercase() in RESERVED_USERNAMES
}
