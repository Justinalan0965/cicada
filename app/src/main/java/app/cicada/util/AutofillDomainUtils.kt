package app.cicada.util

import android.net.Uri
import java.net.IDN
import java.util.Locale

object AutofillDomainUtils {

    /**
     * Accepts a hostname or an HTTP(S) URL and returns a normalized host.
     *
     * Examples:
     *   https://www.instagram.com/login -> instagram.com
     *   Instagram.COM                  -> instagram.com
     *   https://instagram.com.evil.com -> instagram.com.evil.com
     *
     * Returns null for malformed or unsupported input.
     */
    fun normalizeWebsite(input: String?): String? {
        val value = input?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null

        // Reject values that are clearly not a host or HTTP(S) URL.
        if (value.any { it.isWhitespace() || it == '\\' || it == '@' }) {
            return null
        }

        val url = if (
            value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true)
        ) {
            value
        } else {
            "https://$value"
        }

        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host ?: return null

            if (scheme != "http" && scheme != "https") {
                return null
            }

            val normalizedHost = IDN.toASCII(host)
                .lowercase(Locale.ROOT)
                .removeSuffix(".")
                .removePrefix("www.")

            if (!isValidHost(normalizedHost)) {
                null
            } else {
                normalizedHost
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun isValidHost(host: String): Boolean {
        if (host.isBlank() || host.length > 253) return false
        if (host.startsWith(".") || host.endsWith(".")) return false

        // Avoid accepting IP literals as ordinary website domains for now.
        if (host.contains(":")) return false
        if (host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) return false

        val labels = host.split(".")
        if (labels.size < 2) return false

        return labels.all { label ->
            label.isNotEmpty() &&
                    label.length <= 63 &&
                    !label.startsWith("-") &&
                    !label.endsWith("-") &&
                    label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
        }
    }
}