package app.cicada.util

import android.net.Uri

object SiteIconUtil {

    fun getDomain(website : String?): String? {

        if (website.isNullOrBlank()) {
            return null
        }

        var value =  website.trim()

        if (!value.startsWith("http://") &&
            !value.startsWith("https://")) {

            value = "https://$value"
        }

        return try {
            Uri.parse(value)
                .host
                ?.removePrefix("www.")
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun getFaviIcon(website: String?): String?{
        val domain = getDomain(website)
            ?: return null

        return "https://www.google.com/s2/favicons" +
                "?domain=$domain&sz=128"
    }
}