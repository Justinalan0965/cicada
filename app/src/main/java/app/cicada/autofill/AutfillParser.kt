package app.cicada.autofill

import android.app.assist.AssistStructure
import android.util.Log
import android.view.autofill.AutofillId

data class ParsedAutofillRequest(
    val packageName: String?,
    val activityName: String?,
    val webDomain: String?,
    val usernameId: AutofillId?,
    val passwordId: AutofillId?
)

object AutofillParser {

    fun parse(
        structure: AssistStructure
    ): ParsedAutofillRequest {

        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var webDomain: String? = null

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

            if (webDomain == null) {
                webDomain = result.webDomain
            }
        }

        return ParsedAutofillRequest(
            packageName = structure.activityComponent?.packageName,
            activityName = structure.activityComponent?.className,
            webDomain = webDomain,
            usernameId = usernameId,
            passwordId = passwordId
        )
    }

    private fun findFields(
        node: AssistStructure.ViewNode
    ): FieldResult {

        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var webDomain: String? = node.webDomain

        /*
         * Native Android fields
         */
        val hints = node.autofillHints

        if (hints != null) {

            if (
                hints.any {
                    it.equals("username", ignoreCase = true) ||
                            it.equals("emailAddress", ignoreCase = true)
                }
            ) {
                usernameId = node.autofillId
            }

            if (
                hints.any {
                    it.equals("password", ignoreCase = true) ||
                            it.equals("currentPassword", ignoreCase = true)
                }
            ) {
                passwordId = node.autofillId
            }
        }

        /*
         * Web fields
         */
        val htmlInfo = node.htmlInfo

        if (htmlInfo != null) {

            val tag = htmlInfo.tag.lowercase()

            if (tag == "input") {
                val attributes = htmlInfo.attributes

                val type = attributes
                    ?.firstOrNull { it.first == "type" }
                    ?.second
                    ?.lowercase()

                val name = attributes
                    ?.firstOrNull { it.first == "name" }
                    ?.second
                    ?.lowercase()

                val autofillHint = attributes
                    ?.firstOrNull { it.first == "computed-autofill-hints" }
                    ?.second
                    ?.uppercase()

                if (
                    usernameId == null &&
                    (
                            autofillHint == "USERNAME" ||
                                    name == "username" ||
                                    name == "email" ||
                                    name == "identifier" ||
                                    type == "email"
                            )
                ) {
                    usernameId = node.autofillId
                }

                if (
                    passwordId == null &&
                    (
                            autofillHint == "PASSWORD" ||
                                    type == "password" ||
                                    name == "password"
                            )
                ) {
                    passwordId = node.autofillId
                }
            }
        }

        /*
         * Search children
         */
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

            if (webDomain == null) {
                webDomain = childResult.webDomain
            }
        }

        return FieldResult(
            usernameId = usernameId,
            passwordId = passwordId,
            webDomain = webDomain
        )
    }

    private const val TAG = "AutofillParser"

    fun dumpStructure(structure: AssistStructure) {
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode
            dumpNode(root, 0)
        }
    }

    private fun dumpNode(
        node: AssistStructure.ViewNode,
        depth: Int
    ) {
        val hints = node.autofillHints?.joinToString()
        val html = node.htmlInfo

        val safeAttributes = html?.attributes
            ?.filter {
                it.first in setOf(
                    "type", "name", "id", "autocomplete"
                )
            }
            ?.joinToString { "${it.first}=${it.second}" }

        Log.d(
            TAG,
            "${"  ".repeat(depth)}" +
                    "class=${node.className}, " +
                    "autofillType=${node.autofillType}, " +
                    "hints=$hints, " +
                    "htmlTag=${html?.tag}, " +
                    "htmlAttributes=$safeAttributes, " +
                    "webDomain=${node.webDomain}, " +
                    "inputType=${node.inputType}, " +
                    "hasAutofillId=${node.autofillId != null}, " +
                    "children=${node.childCount}"
        )

        for (i in 0 until node.childCount) {
            dumpNode(node.getChildAt(i), depth + 1)
        }
    }

    private data class FieldResult(
        val usernameId: AutofillId?,
        val passwordId: AutofillId?,
        val webDomain: String?
    )
}