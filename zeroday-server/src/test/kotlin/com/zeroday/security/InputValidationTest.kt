package com.zeroday.security

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InputValidationTest {

    @Test
    fun usernameRejectsEmpty() {
        val r = InputValidation.validateUsername("")
        assertTrue(r is InputValidation.ValidationResult.Invalid)
    }

    @Test
    fun usernameRejectsTooShort() {
        val r = InputValidation.validateUsername("ab")
        assertTrue(r is InputValidation.ValidationResult.Invalid)
    }

    @Test
    fun usernameRejectsTooLong() {
        val r = InputValidation.validateUsername("a".repeat(25))
        assertTrue(r is InputValidation.ValidationResult.Invalid)
    }

    @Test
    fun usernameRejectsSpecialChars() {
        val r = InputValidation.validateUsername("hi there!")
        assertTrue(r is InputValidation.ValidationResult.Invalid)
    }

    @Test
    fun usernameRejectsUnicode() {
        val r = InputValidation.validateUsername("café")
        assertTrue(r is InputValidation.ValidationResult.Invalid)
    }

    @Test
    fun usernameAcceptsUnderscoreHyphen() {
        val r = InputValidation.validateUsername("dark_phantom-99")
        assertTrue(r is InputValidation.ValidationResult.Ok, r.toString())
    }

    @Test
    fun passwordRejectsTooShort() {
        val r = InputValidation.validatePassword("1234567")
        assertTrue(r is InputValidation.ValidationResult.Invalid)
    }

    @Test
    fun passwordAcceptsLongPassphrase() {
        // NIST 800-63B: long passphrases are fine without complexity.
        val r = InputValidation.validatePassword("correct horse battery staple")
        assertTrue(r is InputValidation.ValidationResult.Ok)
    }

    @Test
    fun passwordRejectsTooLong() {
        val r = InputValidation.validatePassword("a".repeat(129))
        assertTrue(r is InputValidation.ValidationResult.Invalid)
    }

    @Test
    fun textRejectsControlChars() {
        val r = InputValidation.validateText("hello\u0000world")
        assertTrue(r is InputValidation.ValidationResult.Invalid)
    }

    @Test
    fun textAcceptsNewline() {
        // \n is allowed (text can be multi-line)
        val r = InputValidation.validateText("hello\nworld")
        assertTrue(r is InputValidation.ValidationResult.Ok)
    }

    @Test
    fun reservedUsernameIsCaseInsensitive() {
        assertTrue(InputValidation.isReservedUsername("Admin"))
        assertTrue(InputValidation.isReservedUsername("ROOT"))
        assertTrue(!InputValidation.isReservedUsername("regular_user"))
    }
}
