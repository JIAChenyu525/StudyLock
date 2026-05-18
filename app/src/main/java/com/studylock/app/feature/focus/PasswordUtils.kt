package com.studylock.app.feature.focus

enum class PasswordStrength(val label: String, val score: Int) {
    WEAK("弱", 0),
    MEDIUM("中", 1),
    STRONG("强", 2)
}

object PasswordUtils {

    fun calculateStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK

        var score = 0

        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.length >= 16) score++

        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val typeCount = listOf(hasLower, hasUpper, hasDigit, hasSpecial).count { it }
        score += when {
            typeCount >= 4 -> 3
            typeCount >= 3 -> 2
            typeCount >= 2 -> 1
            else -> 0
        }

        val hasSequential = hasSequentialChars(password)
        val hasRepeated = hasRepeatedChars(password)
        if (hasSequential) score -= 1
        if (hasRepeated) score -= 1

        return when {
            score >= 5 -> PasswordStrength.STRONG
            score >= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    private fun hasSequentialChars(password: String): Boolean {
        val lower = password.lowercase()
        for (i in 0 until lower.length - 2) {
            val c1 = lower[i].code
            val c2 = lower[i + 1].code
            val c3 = lower[i + 2].code
            if (c2 == c1 + 1 && c3 == c2 + 1) return true
            if (c2 == c1 - 1 && c3 == c2 - 1) return true
        }
        return false
    }

    private fun hasRepeatedChars(password: String): Boolean {
        for (i in 0 until password.length - 2) {
            if (password[i] == password[i + 1] && password[i + 1] == password[i + 2]) {
                return true
            }
        }
        return false
    }

    fun getStrengthProgress(strength: PasswordStrength): Float {
        return when (strength) {
            PasswordStrength.WEAK -> 0.33f
            PasswordStrength.MEDIUM -> 0.66f
            PasswordStrength.STRONG -> 1.0f
        }
    }

    fun getStrengthColor(strength: PasswordStrength): androidx.compose.ui.graphics.Color {
        return when (strength) {
            PasswordStrength.WEAK -> androidx.compose.ui.graphics.Color(0xFFE57373)
            PasswordStrength.MEDIUM -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
            PasswordStrength.STRONG -> androidx.compose.ui.graphics.Color(0xFF81C784)
        }
    }
}
