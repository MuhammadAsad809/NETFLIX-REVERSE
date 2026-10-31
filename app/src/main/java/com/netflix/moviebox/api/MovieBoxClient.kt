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
    val trailerUrl: String? = null,
    val score: Double = 8.5,
    val releaseDate: String = "2024",
    val description: String = "",
    val detectedLang: String = "English",
    val duration: String = "2h 10m",
    val genre: String = "",
    val subjectType: Int = 1,
    val seasonCount: Int = 0,
    val detailPath: String = ""
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

data class PlayStreamItem(
    val format: String,
    val resolution: String,
    val url: String,
    val durationSec: Int,
    val size: Long = 0L,
    val codecName: String = "h264"
)

data class PlayResult(
    val hasResource: Boolean,
    val durationSec: Int,
    val streams: List<PlayStreamItem>,
    val bestStreamUrl: String?
)

data class EpisodeItem(
    val episodeNumber: Int,
    val title: String,
    val duration: String = "45m",
    val stillUrl: String? = null
)

data class SeasonDetail(
    val seasonNumber: Int,
    val title: String = "Season $seasonNumber",
    val maxEpisodes: Int,
    val resolutions: List<Int>,
    val episodes: List<EpisodeItem>
)

data class SubjectDetailResult(
    val subjectId: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val stillsUrl: String?,
    val trailerUrl: String?,
    val score: Double,
    val releaseDate: String,
    val detectedLang: String,
    val subjectType: Int,
    val detailPath: String = "",
    val seasons: List<SeasonDetail>,
    val streams: StreamResolution
)

data class HomeFeedResult(
    val heroBanners: List<MovieBoxItem> = emptyList(),
    val sections: Map<String, List<MovieBoxItem>> = emptyMap()
) {
    val heroBanner: MovieBoxItem? get() = heroBanners.firstOrNull()
}

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
                    val item = parseMovieBoxItemFromJson(obj)
                    if (langFilter.isEmpty() || item.detectedLang.equals(langFilter, ignoreCase = true)) {
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

    fun parseMovieBoxItemFromJson(obj: JSONObject): MovieBoxItem {
        val id = obj.optString("subjectId", obj.optString("id", ""))
        val title = obj.optString("title", "")

        val coverObj = obj.optJSONObject("cover")
        val stillsObj = obj.optJSONObject("stills")
        val cover = coverObj?.optString("url")?.takeIf { it.isNotBlank() }
            ?: obj.optString("coverUrl").takeIf { it.isNotBlank() }
            ?: obj.optString("cover").takeIf { it.isNotBlank() }
            ?: stillsObj?.optString("url")
            ?: ""

        val stills = stillsObj?.optString("url")?.takeIf { it.isNotBlank() }
            ?: cover

        val trailerUrl = obj.optJSONObject("trailer")?.optJSONObject("videoAddress")?.optString("url")?.takeIf { it.isNotBlank() }

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
        val season = obj.optInt("season", 0)
        val detailPath = obj.optString("detailPath", "")

        return MovieBoxItem(
            subjectId = id,
            title = title,
            coverUrl = cover,
            stillsUrl = stills,
            trailerUrl = trailerUrl,
            score = score,
            releaseDate = release,
            description = desc,
            detectedLang = lang,
            duration = durationStr,
            genre = genre,
            subjectType = st,
            seasonCount = season,
            detailPath = detailPath
        )
    }

    suspend fun getHomeFeed(): HomeFeedResult = withContext(Dispatchers.IO) {
        val url = "https://h5-api.aoneroom.com/wefeed-h5api-bff/home"
        val reqBuilder = Request.Builder().url(url).get()
        getApiHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        try {
            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext HomeFeedResult(emptyList(), emptyMap())
                val bodyStr = response.body?.string() ?: return@withContext HomeFeedResult(emptyList(), emptyMap())
                val root = JSONObject(bodyStr)
                val operatingList = root.optJSONObject("data")?.optJSONArray("operatingList")
                    ?: return@withContext HomeFeedResult(emptyList(), emptyMap())

                val heroList = mutableListOf<MovieBoxItem>()
                val sectionsMap = mutableMapOf<String, MutableList<MovieBoxItem>>()

                for (i in 0 until operatingList.length()) {
                    val op = operatingList.getJSONObject(i)
                    val title = op.optString("title", "").trim()
                    val opType = op.optString("type", "")

                    if (opType == "BANNER") {
                        val bannerObj = op.optJSONObject("banner")
                        val itemsArr = bannerObj?.optJSONArray("items")
                        if (itemsArr != null) {
                            for (bIdx in 0 until itemsArr.length()) {
                                val bObj = itemsArr.getJSONObject(bIdx)
                                val subObj = bObj.optJSONObject("subject")
                                val rawSubId = bObj.optString("subjectId").takeIf { it.isNotBlank() && it != "0" }
                                    ?: subObj?.optString("subjectId")?.takeIf { it.isNotBlank() && it != "0" }
                                    ?: continue

                                val bTitle = bObj.optString("title").ifBlank { subObj?.optString("title", "") ?: "Featured Title" }
                                val imgUrl = bObj.optJSONObject("image")?.optString("url")?.takeIf { it.isNotBlank() }
                                    ?: subObj?.optJSONObject("stills")?.optString("url")?.takeIf { it.isNotBlank() }
                                    ?: subObj?.optJSONObject("cover")?.optString("url")?.takeIf { it.isNotBlank() }
                                    ?: ""

                                val trailerUrl = subObj?.optJSONObject("trailer")?.optJSONObject("videoAddress")?.optString("url")?.takeIf { it.isNotBlank() }
                                val rating = subObj?.optString("imdbRatingValue")?.toDoubleOrNull() ?: 8.8
                                val release = subObj?.optString("releaseDate")?.take(4) ?: "2024"
                                val desc = subObj?.optString("description") ?: ""
                                val stType = subObj?.optInt("subjectType", 1) ?: 1
                                val bDetailPath = bObj.optString("detailPath").ifBlank { subObj?.optString("detailPath", "") ?: "" }

                                heroList.add(
                                    MovieBoxItem(
                                        subjectId = rawSubId,
                                        title = bTitle,
                                        coverUrl = imgUrl,
                                        stillsUrl = imgUrl,
                                        trailerUrl = trailerUrl,
                                        score = rating,
                                        releaseDate = release,
                                        description = desc,
                                        detectedLang = detectLanguage(bTitle),
                                        duration = "2h 15m",
                                        genre = subObj?.optString("genre") ?: "Featured, Action",
                                        subjectType = stType,
                                        detailPath = bDetailPath
                                    )
                                )
                            }
                        }
                    } else if (op.has("subjects")) {
                        val subjectsArr = op.optJSONArray("subjects") ?: continue
                        if (subjectsArr.length() == 0) continue

                        val itemsList = mutableListOf<MovieBoxItem>()
                        for (j in 0 until subjectsArr.length()) {
                            val sObj = subjectsArr.getJSONObject(j)
                            val item = parseMovieBoxItemFromJson(sObj)
                            itemsList.add(item)
                        }

                        if (itemsList.isNotEmpty()) {
                            val cleanTitle = when {
                                title.contains("Trending Now", ignoreCase = true) -> "Trending Now"
                                title.contains("Cinema", ignoreCase = true) -> "Top Cinema"
                                title.contains("Top Series", ignoreCase = true) -> "Top TV Series"
                                title.contains("Hollywood", ignoreCase = true) -> "Hollywood Hits"
                                title.contains("Bollywood", ignoreCase = true) -> "Bollywood Cinema"
                                title.contains("South Indian", ignoreCase = true) -> "South Indian Action"
                                title.contains("Western TV", ignoreCase = true) -> "Western TV Shows"
                                title.contains("Indian Drama", ignoreCase = true) -> "Indian Drama"
                                title.contains("Best Asian Drama", ignoreCase = true) -> "Asian Dramas"
                                title.contains("Top Anime", ignoreCase = true) -> "Top Anime & Animation"
                                title.contains("Turkish Drama", ignoreCase = true) -> "Turkish Drama"
                                title.contains("Coming Soon", ignoreCase = true) -> "Coming Soon"
                                else -> title.replace(Regex("[^a-zA-Z0-9 &-]"), "").trim()
                            }
                            if (cleanTitle.isNotBlank()) {
                                sectionsMap[cleanTitle] = itemsList
                            }
                        }
                    }
                }

                // If heroList is sparse, augment with top items from Trending Now that have trailers
                val trendingItems = sectionsMap["Trending Now"] ?: sectionsMap.values.firstOrNull() ?: emptyList()
                trendingItems.forEach { tr ->
                    if (!heroList.any { it.subjectId == tr.subjectId }) {
                        heroList.add(tr)
                    }
                }

                HomeFeedResult(heroList, sectionsMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            HomeFeedResult(emptyList(), emptyMap())
        }
    }

    suspend fun getSubjectDetail(subjectId: String): SubjectDetailResult = withContext(Dispatchers.IO) {
        val detailUrl = "https://h5-api.aoneroom.com/wefeed-h5api-bff/detail?subjectId=$subjectId"
        val reqBuilder = Request.Builder().url(detailUrl).get()
        getApiHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        var item = MovieBoxItem(subjectId = subjectId, title = "Movie", coverUrl = "")
        val seasonsList = mutableListOf<SeasonDetail>()

        try {
            client.newCall(reqBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val root = JSONObject(bodyStr)
                    val dataObj = root.optJSONObject("data")
                    val subjectObj = dataObj?.optJSONObject("subject")
                    if (subjectObj != null) {
                        item = parseMovieBoxItemFromJson(subjectObj)
                    }

                    val resourceObj = dataObj?.optJSONObject("resource")
                    val seasonsArr = resourceObj?.optJSONArray("seasons")
                    val isAnime = item.title.contains("naruto", ignoreCase = true) ||
                            item.genre.contains("Anime", ignoreCase = true) ||
                            item.genre.contains("Animation", ignoreCase = true)
                    val defaultDur = if (isAnime) "24m" else "45m"

                    if (seasonsArr != null && seasonsArr.length() > 0) {
                        data class RawSeason(val se: Int, val maxEp: Int, val resolutions: List<Int>)
                        val rawList = mutableListOf<RawSeason>()

                        for (i in 0 until seasonsArr.length()) {
                            val sObj = seasonsArr.getJSONObject(i)
                            val maxEp = sObj.optInt("maxEp", 0)
                            if (maxEp <= 0) continue

                            val seNum = sObj.optInt("se", i + 1)
                            val resList = mutableListOf<Int>()
                            val resArr = sObj.optJSONArray("resolutions")
                            if (resArr != null) {
                                for (r in 0 until resArr.length()) {
                                    val rObj = resArr.getJSONObject(r)
                                    val resVal = rObj.optInt("resolution", 0)
                                    if (resVal > 0 && !resList.contains(resVal)) {
                                        resList.add(resVal)
                                    }
                                }
                            }
                            if (resList.isEmpty()) {
                                resList.addAll(listOf(1080, 720, 480))
                            }
                            resList.sortDescending()
                            rawList.add(RawSeason(seNum, maxEp, resList))
                        }

                        if (rawList.size == 1 && rawList[0].maxEp > 30) {
                            // Large episode anime / series (e.g. Naruto with 375 episodes in season 1)
                            // Divide into clean batches of 25 episodes with range tabs
                            val totalEp = rawList[0].maxEp
                            val singleSe = rawList[0].se
                            val resList = rawList[0].resolutions
                            val batchSize = 25
                            val batchCount = (totalEp + batchSize - 1) / batchSize

                            for (b in 0 until batchCount) {
                                val startEp = b * batchSize + 1
                                val endEp = kotlin.math.min((b + 1) * batchSize, totalEp)
                                val batchEpisodes = (startEp..endEp).map { epNum ->
                                    EpisodeItem(
                                        episodeNumber = epNum,
                                        title = "Episode $epNum",
                                        duration = defaultDur,
                                        stillUrl = item.stillsUrl ?: item.coverUrl
                                    )
                                }
                                seasonsList.add(
                                    SeasonDetail(
                                        seasonNumber = singleSe,
                                        title = "Ep $startEp-$endEp",
                                        maxEpisodes = totalEp,
                                        resolutions = resList,
                                        episodes = batchEpisodes
                                    )
                                )
                            }
                        } else {
                            // Standard multi-season series (e.g. Lupin S1-S4, Stranger Things S1-S5)
                            for (raw in rawList) {
                                val episodes = (1..raw.maxEp).map { epNum ->
                                    EpisodeItem(
                                        episodeNumber = epNum,
                                        title = "Episode $epNum",
                                        duration = defaultDur,
                                        stillUrl = item.stillsUrl ?: item.coverUrl
                                    )
                                }
                                seasonsList.add(
                                    SeasonDetail(
                                        seasonNumber = raw.se,
                                        title = "Season ${raw.se}",
                                        maxEpisodes = raw.maxEp,
                                        resolutions = raw.resolutions,
                                        episodes = episodes
                                    )
                                )
                            }
                        }

                        // Set authentic duration label on item for series
                        if (seasonsList.isNotEmpty()) {
                            val durLabel = if (rawList.size > 1) "${rawList.size} Seasons" else "${rawList[0].maxEp} Episodes"
                            item = item.copy(duration = durLabel, subjectType = 2)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val streams = extractStream(subjectId)
        val finalTrailer = item.trailerUrl ?: streams.trailerUrl

        SubjectDetailResult(
            subjectId = item.subjectId,
            title = item.title,
            description = item.description,
            coverUrl = item.coverUrl,
            stillsUrl = item.stillsUrl,
            trailerUrl = finalTrailer,
            score = item.score,
            releaseDate = item.releaseDate,
            detectedLang = item.detectedLang,
            subjectType = item.subjectType,
            detailPath = item.detailPath,
            seasons = seasonsList,
            streams = streams
        )
    }

    suspend fun getEpisodePlayStream(
        subjectId: String,
        se: Int = 0,
        ep: Int = 0,
        detailPath: String = ""
    ): PlayResult = withContext(Dispatchers.IO) {
        var realDetailPath = detailPath
        if (realDetailPath.isBlank()) {
            try {
                val dUrl = "https://h5-api.aoneroom.com/wefeed-h5api-bff/detail?subjectId=$subjectId"
                val dReq = Request.Builder().url(dUrl).get()
                getApiHeaders().forEach { (k, v) -> dReq.addHeader(k, v) }
                client.newCall(dReq.build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val root = JSONObject(resp.body?.string() ?: "")
                        realDetailPath = root.optJSONObject("data")?.optJSONObject("subject")?.optString("detailPath", "") ?: ""
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val playUrl = "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/play?subjectId=$subjectId&se=$se&ep=$ep&detailPath=$realDetailPath&streamSignType=1"
        val reqBuilder = Request.Builder()
            .url(playUrl)
            .get()
            .addHeader("Origin", "https://themoviebox.xyz")
            .addHeader("Referer", "https://themoviebox.xyz/en/spa/videoPlayPage/movies/$realDetailPath")
            .addHeader("X-Vip-Restrict", "0")

        getApiHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        try {
            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext PlayResult(hasResource = false, durationSec = 0, streams = emptyList(), bestStreamUrl = null)
                }
                val bodyStr = response.body?.string() ?: return@withContext PlayResult(false, 0, emptyList(), null)
                val root = JSONObject(bodyStr)
                val dataObj = root.optJSONObject("data")
                val hasResource = dataObj?.optBoolean("hasResource", false) ?: false
                val streamsArr = dataObj?.optJSONArray("streams")

                val streamList = mutableListOf<PlayStreamItem>()
                var maxDur = 0
                if (streamsArr != null) {
                    for (i in 0 until streamsArr.length()) {
                        val sObj = streamsArr.getJSONObject(i)
                        val url = sObj.optString("url", "")
                        if (url.isNotBlank()) {
                            val format = sObj.optString("format", "MP4")
                            val res = sObj.optString("resolutions", "720")
                            val dur = sObj.optInt("duration", 0)
                            if (dur > maxDur) maxDur = dur
                            val size = sObj.optLong("size", 0L)
                            val codec = sObj.optString("codecName", "h264")
                            streamList.add(
                                PlayStreamItem(
                                    format = format,
                                    resolution = res,
                                    url = url,
                                    durationSec = dur,
                                    size = size,
                                    codecName = codec
                                )
                            )
                        }
                    }
                }

                // Pick 1080p, then 720p, then first available
                val bestUrl = streamList.firstOrNull { it.resolution.contains("1080") }?.url
                    ?: streamList.firstOrNull { it.resolution.contains("720") }?.url
                    ?: streamList.firstOrNull()?.url

                PlayResult(
                    hasResource = hasResource || streamList.isNotEmpty(),
                    durationSec = maxDur,
                    streams = streamList,
                    bestStreamUrl = bestUrl
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PlayResult(hasResource = false, durationSec = 0, streams = emptyList(), bestStreamUrl = null)
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

    suspend fun getRelatedLanguageReleases(title: String): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        val cleanBase = title.replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)|hindi dubbed|dual audio|urdu dubbed|season\\s*\\d+|s\\d+(-s\\d+)?"), "").trim()
        if (cleanBase.length < 2) return@withContext emptyList()

        try {
            val searchItems = searchMovies(keyword = cleanBase, perPage = 10)
            val versionsByLang = mutableMapOf<String, MovieBoxItem>()

            for (item in searchItems) {
                val lang = item.detectedLang
                if (!versionsByLang.containsKey(lang)) {
                    versionsByLang[lang] = item
                }
            }
            versionsByLang.values.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
