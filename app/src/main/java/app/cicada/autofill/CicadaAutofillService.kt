package app.cicada.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log

class CicadaAutofillService : AutofillService() {

    companion object {
        private const val TAG = "CicadaAutofill"
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        Log.d(TAG, "========== AUTOFILL REQUEST ==========")

        val structure = request
            .fillContexts
            .lastOrNull()
            ?.structure

        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val parsed = AutofillParser.parse(structure)

        Log.d(TAG, "Package: ${parsed.packageName}")
        Log.d(TAG, "Activity: ${parsed.activityName}")
        Log.d(TAG, "Username ID: ${parsed.usernameId}")
        Log.d(TAG, "Password ID: ${parsed.passwordId}")

        callback.onSuccess(null)

        Log.d(TAG, "========== END AUTOFILL REQUEST ==========")
    }

    private fun dumpNode(
        structure: AssistStructure,
        depth: Int
    ) {
        for (windowIndex in 0 until structure.windowNodeCount) {
            val window = structure.getWindowNodeAt(windowIndex)
            dumpViewNode(window.rootViewNode, depth)
        }
    }

    private fun dumpViewNode(
        node: AssistStructure.ViewNode,
        depth: Int
    ) {
        val indent = "  ".repeat(depth)

        Log.d(
            TAG,
            "$indent" +
                    "class=${node.className}, " +
                    "id=${node.idEntry}, " +
                    "autofillId=${node.autofillId}, " +
                    "autofillType=${node.autofillType}, " +
                    "hints=${node.autofillHints?.contentToString()}, " +
                    "text=${node.text}, " +
                    "hint=${node.hint}"
        )

        for (i in 0 until node.childCount) {
            dumpViewNode(
                node.getChildAt(i),
                depth + 1
            )
        }
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback
    ) {
        Log.d(TAG, "Save request received")
        callback.onSuccess()
    }
}