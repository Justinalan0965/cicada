package app.cicada.util

import org.junit.Test
import org.junit.Assert.assertEquals

class AutofillDomainUtilsTest {

    @Test
    fun `test normalizer`() {
        // Valid URL with www and path
        assertEquals(
            "instagram.com",
            AutofillDomainUtils.normalizeWebsite(
                "https://www.instagram.com/login"
            )
        )

        // Case normalization
        assertEquals(
            "instagram.com",
            AutofillDomainUtils.normalizeWebsite("Instagram.COM")
        )

        // A different domain must NOT match Instagram
        assertEquals(
            "instagram.com.evil.example",
            AutofillDomainUtils.normalizeWebsite(
                "https://instagram.com.evil.example"
            )
        )

        // Reject a URL containing user-info syntax
        assertEquals(
            null,
            AutofillDomainUtils.normalizeWebsite(
                "https://instagram.com@evil.example"
            )
        )

        // Reject invalid input
        assertEquals(
            null,
            AutofillDomainUtils.normalizeWebsite("not a website")
        )

        // Reject unsupported schemes
        assertEquals(
            null,
            AutofillDomainUtils.normalizeWebsite("ftp://instagram.com")
        )
    }
}
