package app.cicada.security

import com.nulabinc.zxcvbn.Zxcvbn

data class PasswordStrengthResult(
    val score: Int,
    val label: String,
    val warning: String?,
    val suggestions: List<String>
)

object PasswordStrengthChecker {

    private val zxcvbn = Zxcvbn()

    private val labels = listOf(
        "Weak",
        "Fair",
        "Good",
        "Strong",
        "Very strong"
    )

    fun evaluate(password: String): PasswordStrengthResult? {
        if (password.isEmpty()) return null

        val result = zxcvbn.measure(password)
        val score = result.score.coerceIn(0, 4)

        return PasswordStrengthResult(
            score = score,
            label = labels[score],
            warning = result.feedback.warning
                ?.takeIf { it.isNotBlank() },
            suggestions = result.feedback.suggestions
        )
    }
}