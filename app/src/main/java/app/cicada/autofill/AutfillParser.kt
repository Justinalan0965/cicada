package app.cicada.autofill

import android.app.assist.AssistStructure
import android.view.autofill.AutofillId

data class ParsedAutofillRequest(
    val packageName: String?,
    val activityName: String?,
    val usernameId: AutofillId?,
    val passwordId: AutofillId?
)

object AutofillParser {

    fun parse(
        structure: AssistStructure
    ): ParsedAutofillRequest {

        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null

        for (windowIndex in 0 until structure.windowNodeCount) {

            val window = structure.getWindowNodeAt(windowIndex)

            val result = findFields(
                window.rootViewNode
            )

            if (usernameId == null) {
                usernameId = result.usernameId
            }

            if (passwordId == null) {
                passwordId = result.passwordId
            }
        }

        return ParsedAutofillRequest(
            packageName = structure.activityComponent?.packageName,
            activityName = structure.activityComponent?.className,
            usernameId = usernameId,
            passwordId = passwordId
        )
    }

    private fun findFields(
        node: AssistStructure.ViewNode
    ): FieldIds {

        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null

        val hints = node.autofillHints

        if (hints != null) {

            if (
                hints.contains("username") ||
                hints.contains("emailAddress")
            ) {
                usernameId = node.autofillId
            }

            if (
                hints.contains("password") ||
                hints.contains("currentPassword")
            ) {
                passwordId = node.autofillId
            }
        }

        for (i in 0 until node.childCount) {

            val childResult = findFields(
                node.getChildAt(i)
            )

            if (usernameId == null) {
                usernameId = childResult.usernameId
            }

            if (passwordId == null) {
                passwordId = childResult.passwordId
            }
        }

        return FieldIds(
            usernameId = usernameId,
            passwordId = passwordId
        )
    }

    private data class FieldIds(
        val usernameId: AutofillId?,
        val passwordId: AutofillId?
    )
}