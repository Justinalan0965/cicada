
package app.cicada.security

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface PasswordBreachResult {
    data object NotFound : PasswordBreachResult

    data class Found(
        val occurrences: Long
    ) : PasswordBreachResult

    data object NetworkError : PasswordBreachResult
}

object PasswordBreachChecker {

    private const val CACHE_MAX_SIZE = 100
    private val CACHE_TTL_MS = TimeUnit.HOURS.toMillis(24)

    private data class CachedResult(
        val result: PasswordBreachResult,
        val timestamp: Long
    )

    private val cache = LruCache<String, CachedResult>(CACHE_MAX_SIZE)

    private val inFlightMutex = Mutex()

    private val inFlightRequests =
        mutableMapOf<String, CompletableDeferred<PasswordBreachResult>>()

    // Independent of individual screens/callers.
    private val requestScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun getCachedResult(key: String): PasswordBreachResult? {
        val cached = cache.get(key) ?: return null

        if (System.currentTimeMillis() - cached.timestamp >= CACHE_TTL_MS) {
            cache.remove(key)
            return null
        }

        return cached.result
    }

    private fun cacheResult(
        key: String,
        result: PasswordBreachResult
    ) {
        if (result is PasswordBreachResult.NetworkError) return

        cache.put(
            key,
            CachedResult(
                result = result,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun check(password: String): PasswordBreachResult {
        if (password.isEmpty()) {
            return PasswordBreachResult.NetworkError
        }

        val hash = MessageDigest.getInstance("SHA-1")
            .digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02X".format(it) }

        // Cache key is derived from the hash; never log or persist it.
        val cacheKey = MessageDigest.getInstance("SHA-256")
            .digest(hash.toByteArray(Charsets.US_ASCII))
            .joinToString("") { "%02X".format(it) }

        getCachedResult(cacheKey)?.let { return it }

        val (deferred, isOwner) = inFlightMutex.withLock {
            // Check again inside the lock to avoid a race with another caller.
            getCachedResult(cacheKey)?.let {
                return it
            }

            val existing = inFlightRequests[cacheKey]

            if (existing != null) {
                existing to false
            } else {
                val created = CompletableDeferred<PasswordBreachResult>()
                inFlightRequests[cacheKey] = created
                created to true
            }
        }

        if (isOwner) {
            requestScope.launch {
                val result = try {
                    fetchBreachResult(hash)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    PasswordBreachResult.NetworkError
                }

                try {
                    inFlightMutex.withLock {
                        cacheResult(cacheKey, result)
                        inFlightRequests.remove(cacheKey, deferred)
                        deferred.complete(result)
                    }
                } catch (_: Exception) {
                    // Ensure waiting callers aren't left suspended.
                    deferred.complete(PasswordBreachResult.NetworkError)
                }
            }
        }

        return deferred.await()
    }

    private suspend fun fetchBreachResult(
        hash: String
    ): PasswordBreachResult = withContext(Dispatchers.IO) {
        val prefix = hash.take(5)
        val suffix = hash.drop(5)

        try {

            val connection = URL(
                "https://api.pwnedpasswords.com/range/$prefix"
            ).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty(
                    "User-Agent",
                    "Cicada-Password-Manager"
                )
                connection.setRequestProperty("Add-Padding", "true")

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext PasswordBreachResult.NetworkError
                }


                val parsedCounts = mutableMapOf<String, Long>()
                var malformedResponse = false
                var lineCount = 0

                connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .useLines { lines ->
                        lines.forEach { line ->
                            lineCount++

                            val parts = line.split(':', limit = 2)

                            if (parts.size != 2) {
                                malformedResponse = true
                                return@forEach
                            }

                            val hashSuffix = parts[0]
                            val count = parts[1].toLongOrNull()

                            val validSuffix =
                                hashSuffix.length == 35 &&
                                        hashSuffix.all {
                                            it in '0'..'9' ||
                                                    it in 'A'..'F' ||
                                                    it in 'a'..'f'
                                        }

                            if (!validSuffix || count == null || count < 0L) {
                                malformedResponse = true
                                return@forEach
                            }

                            val normalizedSuffix = hashSuffix.uppercase()

                            if (parsedCounts.put(normalizedSuffix, count) != null) {
                                malformedResponse = true
                            }
                        }
                    }

                Log.d(
                    "PasswordBreach",
                    "Response lines=$lineCount, unique suffixes=${parsedCounts.size}, malformed=$malformedResponse"
                )

                if (
                    malformedResponse ||
                    lineCount == 0 ||
                    parsedCounts.size != lineCount
                ) {
                    return@withContext PasswordBreachResult.NetworkError
                }

                val occurrences = parsedCounts[suffix.uppercase()]

                if (occurrences != null && occurrences > 0L) {
                    PasswordBreachResult.Found(occurrences)
                } else {
                    PasswordBreachResult.NotFound
                }


            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            Log.e("PasswordBreach", "Network/response read failed", e)
            PasswordBreachResult.NetworkError
        } catch (e: SecurityException) {
            Log.e("PasswordBreach", "Security exception", e)
            PasswordBreachResult.NetworkError
        } catch (e: IllegalArgumentException) {
            Log.e("PasswordBreach", "Invalid request or response", e)
            PasswordBreachResult.NetworkError
        }
    }
}
