package app.cicada.security

import android.R.attr.password
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CicadaClipboardManager(
    private val context : Context
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var clearJob: Job? = null

    fun copy(label : String, text : String) {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
    }

    fun copyPassword(scope: CoroutineScope, password : String, clearAfterMs : Long = 30_000L) {

        clearJob?.cancel()

        copy("Password", password)

        clearJob = scope.launch {
            delay(clearAfterMs)

            if(clipboardStillContains(password)) {
                clipboardManager.clearPrimaryClip()
            }
        }
    }

    fun clearClipboard() {
        clearJob?.cancel()
        clipboardManager.clearPrimaryClip()
    }

    private fun clipboardStillContains(
        expectedText: String
    ): Boolean {

        if (!clipboardManager.hasPrimaryClip()) {
            return false
        }

        val clip = clipboardManager.primaryClip
            ?: return false

        if (clip.itemCount == 0) {
            return false
        }

        val text = clip
            .getItemAt(0)
            .coerceToText(context)
            .toString()

        return text == expectedText
    }
}