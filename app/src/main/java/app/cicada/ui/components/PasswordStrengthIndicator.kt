package app.cicada.ui.components

import app.cicada.security.PasswordStrengthResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PasswordStrengthIndicator(
    result: PasswordStrengthResult,
    modifier: Modifier = Modifier
) {
    val strengthColor = when (result.score) {
        0 -> Color(0xFFDC5555)
        1 -> Color(0xFFE88442)
        2 -> Color(0xFFD6A52B)
        3 -> Color(0xFF64A67A)
        else -> Color(0xFF2FA66D)
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Password strength",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = result.label,
                style = MaterialTheme.typography.bodySmall,
                color = strengthColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(5) { index ->
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (index <= result.score) {
                                strengthColor
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }

        if (result.score <= 2) {
            val feedback = buildList {
                result.warning?.let { add(it) }
                addAll(result.suggestions)
            }.distinct()

            if (feedback.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))

                feedback.forEach { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}