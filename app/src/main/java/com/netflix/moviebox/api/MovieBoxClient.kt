package com.netflix.moviebox.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class MovieBoxItem(
    val subjectId: String,
    val title: String,
    val coverUrl: String,
    val stillsUrl: String? = null,
    val score: Double = 8.5,
    val releaseDate: String = "2024",
    val description: String = "",
    val detectedLang: String = "English",
    val duration: String = "2h 10m",
    val genre: String = "",
    val subjectType: Int = 1
)

data class StreamResolution(
    val subjectId: String,
    val title: String,
    val baseTitle: String,
    val detectedLang: String,
    val hasFullMovie: Boolean,
    val fullStreamUrl: String?,
    val trailerUrl: String?,
    val allStreams: List<String>
)

object MovieBoxClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getApiHeaders(): Map<String, String> {
        val now = System.currentTimeMillis() / 1000
        val revNow = now.toString().reversed()
        val tokenHash = md5(revNow)
        return mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            "Accept" to "application/json, text/plain, */*",
            "Content-Type" to "application/json",
            "Origin" to "https://h5.aoneroom.com",
            "Referer" to "https://h5.aoneroom.com/",
            "X-Client-Token" to "$now,$tokenHash",
            "X-Client-Info" to "{\"timezone\":\"Asia/Karachi\"}",
            "X-Request-Lang" to "en"
        )
    }

    fun detectLanguage(title: String): String {
        val t = title.lowercase()
        return when {
            "[hindi]" in t || "hindi dubbed" in t || " hindi " in t || t.endsWith(" hindi") -> "Hindi"
            "[urdu]" in t || "urdu dubbed" in t || " urdu " in t -> "Urdu"
            "dual audio" in t || "multi audio" in t -> "Dual Audio"
            else -> "English"
        }
    }

    suspend fun searchMovies(
        keyword: String,
        subjectType: Int = 0,
        langFilter: String = "",
        page: Int = 1,
        perPage: Int = 16
    ): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        var searchTerm = keyword.trim()
        if (langFilter.equals("Hindi", ignoreCase = true) && !searchTerm.lowercase().contains("hindi")) {
            searchTerm = "$searchTerm Hindi"
        } else if (langFilter.equals("Urdu", ignoreCase = true) && !searchTerm.lowercase().contains("urdu")) {
            searchTerm = "$searchTerm Urdu"
        }

        val url = "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/search"
        val payload = JSONObject().apply {
            put("keyword", searchTerm)
            if (subjectType > 0) {
                put("subjectType", subjectType)
            }
            put("page", page)
            put("perPage", perPage)
        }.toString()

        val reqBuilder = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json".toMediaType()))

        getApiHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        try {
            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(bodyStr)
                val itemsArr = root.optJSONObject("data")?.optJSONArray("items") ?: return@withContext emptyList()

                val result = mutableListOf<MovieBoxItem>()
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.getJSONObject(i)
                    val id = obj.optString("subjectId", obj.optString("id", ""))
                    val title = obj.optString("title", "")

                    // Extract cover and stills URLs correctly from nested JSON objects
                    val coverObj = obj.optJSONObject("cover")
                    val stillsObj = obj.optJSONObject("stills")
                    val cover = coverObj?.optString("url")?.takeIf { it.isNotBlank() }
                        ?: obj.optString("coverUrl").takeIf { it.isNotBlank() }
                        ?: obj.optString("cover").takeIf { it.isNotBlank() }
                        ?: stillsObj?.optString("url")
                        ?: ""

                    val stills = stillsObj?.optString("url")?.takeIf { it.isNotBlank() }

                    val ratingStr = obj.optString("imdbRatingValue", "")
                    val score = if (ratingStr.isNotBlank()) {
                        ratingStr.toDoubleOrNull() ?: 8.4
                    } else {
                        obj.optDouble("score", 8.4)
                    }

                    val release = obj.optString("releaseDate", "2024").let {
                        if (it.length >= 4) it.substring(0, 4) else "2024"
                    }
                    val desc = obj.optString("description", "")
                    val lang = detectLanguage(title)
                    val durationSec = obj.optInt("duration", 0)
                    val durationStr = if (durationSec > 0) {
                        val hrs = durationSec / 3600
                        val mins = (durationSec % 3600) / 60
                        if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                    } else "2h 10m"
                    val genre = obj.optString("genre", "")
                    val st = obj.optInt("subjectType", 1)

                    val item = MovieBoxItem(
                        subjectId = id,
                        title = title,
                        coverUrl = cover,
                        stillsUrl = stills,
                        score = score,
                        releaseDate = release,
                        description = desc,
                        detectedLang = lang,
                        duration = durationStr,
                        genre = genre,
                        subjectType = st
                    )

                    if (langFilter.isEmpty() || lang.equals(langFilter, ignoreCase = true)) {
                        result.add(item)
                    }
                }
                result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun extractStream(subjectId: String): StreamResolution = withContext(Dispatchers.IO) {
        val shareUrl = "https://h5.aoneroom.com/share/subject?id=$subjectId"
        val req = Request.Builder()
            .url(shareUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .build()

        try {
            client.newCall(req).execute().use { response ->
                val html = response.body?.string() ?: ""

                // 1. Extract Full Movies Streams
                val fullStreams = mutableSetOf<String>()
                val p1 = Pattern.compile("https://bcdn\\.hakunaymatata\\.com/[^\\\"]+\\.mp4\\?[^\\\"]+")
                val m1 = p1.matcher(html)
                while (m1.find()) {
                    fullStreams.add(m1.group())
                }

                if (fullStreams.isEmpty()) {
                    val p2 = Pattern.compile("https://[a-zA-Z0-9.-]+\\.com/[^\\\"]+\\.mp4\\?sign=[^\\\"]+")
                    val m2 = p2.matcher(html)
                    while (m2.find()) {
                        fullStreams.add(m2.group())
                    }
                }

                // 2. Extract Trailers
                val trailers = mutableSetOf<String>()
                val pTrailer = Pattern.compile("https://[a-zA-Z0-9.-]+\\.com/vone/[^\\\"]+\\.mp4")
                val mTrailer = pTrailer.matcher(html)
                while (mTrailer.find()) {
                    trailers.add(mTrailer.group())
                }

                // 3. Extract Page Title
                val pTitle = Pattern.compile("<title>([^<]+)</title>")
                val mTitle = pTitle.matcher(html)
                var pageTitle = if (mTitle.find()) mTitle.group(1).replace(" - MovieBox", "").trim() else "Movie"

                val cleanBase = pageTitle.replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)|hindi dubbed|dual audio|urdu dubbed"), "").trim()

                StreamResolution(
                    subjectId = subjectId,
                    title = pageTitle,
                    baseTitle = cleanBase,
                    detectedLang = detectLanguage(pageTitle),
                    hasFullMovie = fullStreams.isNotEmpty(),
                    fullStreamUrl = fullStreams.firstOrNull(),
                    trailerUrl = trailers.firstOrNull(),
                    allStreams = fullStreams.toList()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            StreamResolution(
                subjectId = subjectId,
                title = "Movie",
                baseTitle = "Movie",
                detectedLang = "English",
                hasFullMovie = false,
                fullStreamUrl = null,
                trailerUrl = null,
                allStreams = emptyList()
            )
        }
    }
}
