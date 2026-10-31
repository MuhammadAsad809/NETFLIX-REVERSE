package com.netflix.moviebox.data

import com.netflix.moviebox.api.MovieBoxClient
import com.netflix.moviebox.api.MovieBoxItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

data class HomeCategory(
    val id: String,
    val title: String,
    val isRanked: Boolean = false,
    val movies: List<MovieBoxItem>
)

object MovieBoxRepository {

    // In-memory cache for fast UI restores
    private var cachedHeroes: List<MovieBoxItem> = emptyList()
    private var cachedCategories: List<HomeCategory> = emptyList()

    suspend fun getFeaturedHeroes(): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        if (cachedHeroes.isNotEmpty()) return@withContext cachedHeroes

        val feed = MovieBoxClient.getHomeFeed()
        val heroes = if (feed.heroBanners.isNotEmpty()) {
            feed.heroBanners
        } else {
            val trending = feed.sections["Trending Now"] ?: feed.sections.values.firstOrNull() ?: emptyList()
            if (trending.isNotEmpty()) trending.take(8) else listOf(
                MovieBoxItem(
                    subjectId = "4362429319592001952",
                    title = "Mirzapur: The Movie [Hindi]",
                    coverUrl = "https://pbcdnw.aoneroom.com/image/2026/01/27/e9297d39584c33b5ded66cff74a31dc8.jpg",
                    stillsUrl = "https://pbcdnw.aoneroom.com/image/2022/09/09/1bb7aa281a086492c0b2a0eb354bb4bc.jpg",
                    trailerUrl = "https://macdn.aoneroom.com/media/vone/2026/06/06/2efb348a0ff6312e4926a2e91934a1db-ld.mp4",
                    score = 9.2,
                    releaseDate = "2024",
                    description = "The battle for Purvanchal reaches its ultimate cinematic showdown.",
                    detectedLang = "Hindi"
                )
            )
        }
        cachedHeroes = heroes
        heroes
    }

    suspend fun getFeaturedHero(): MovieBoxItem = withContext(Dispatchers.IO) {
        getFeaturedHeroes().firstOrNull() ?: MovieBoxItem(
            subjectId = "4362429319592001952",
            title = "Mirzapur: The Movie [Hindi]",
            coverUrl = "https://pbcdnw.aoneroom.com/image/2026/01/27/e9297d39584c33b5ded66cff74a31dc8.jpg",
            stillsUrl = "https://pbcdnw.aoneroom.com/image/2022/09/09/1bb7aa281a086492c0b2a0eb354bb4bc.jpg",
            trailerUrl = "https://macdn.aoneroom.com/media/vone/2026/06/06/2efb348a0ff6312e4926a2e91934a1db-ld.mp4",
            score = 9.2,
            releaseDate = "2024",
            description = "The battle for Purvanchal reaches its ultimate cinematic showdown.",
            detectedLang = "Hindi"
        )
    }

    suspend fun getHomeCategories(): List<HomeCategory> = withContext(Dispatchers.IO) {
        if (cachedCategories.isNotEmpty()) return@withContext cachedCategories

        // 1. Fetch real homepage sections from MovieBox BFF
        val feed = MovieBoxClient.getHomeFeed()
        val list = mutableListOf<HomeCategory>()

        if (feed.sections.isNotEmpty()) {
            var index = 0
            feed.sections.forEach { (title, movies) ->
                if (movies.isNotEmpty()) {
                    val isRanked = index == 0 || title.contains("Trending", ignoreCase = true)
                    val id = "cat_${index}_" + title.lowercase().replace(" ", "_")
                    list.add(HomeCategory(id = id, title = title, isRanked = isRanked, movies = movies))
                    index++
                }
            }
        }

        // 2. Fallback to catalog filtering if feed was empty
        if (list.isEmpty()) {
            val trendingDeferred = async { MovieBoxClient.searchMovies("Trending", maxPages = 2) }
            val top10Deferred = async { MovieBoxClient.searchMovies("Top", maxPages = 2) }
            val tvSeriesDeferred = async { MovieBoxClient.getFilteredCatalog(subjectType = 2, perPage = 24) }
            val blockbusterDeferred = async { MovieBoxClient.getFilteredCatalog(subjectType = 1, perPage = 24) }
            val animeDeferred = async { MovieBoxClient.getFilteredCatalog(genre = "Anime", perPage = 24) }
            val actionDeferred = async { MovieBoxClient.searchMovies("Action", maxPages = 2) }

            val trending = trendingDeferred.await()
            val top10 = top10Deferred.await()
            val tvSeries = tvSeriesDeferred.await()
            val blockbuster = blockbusterDeferred.await()
            val anime = animeDeferred.await()
            val action = actionDeferred.await()

            if (trending.isNotEmpty()) {
                list.add(HomeCategory("trending", "Trending Now", isRanked = false, trending))
            }
            if (top10.isNotEmpty()) {
                list.add(HomeCategory("top10", "Top 10 Today", isRanked = true, top10))
            }
            if (tvSeries.isNotEmpty()) {
                list.add(HomeCategory("series", "Popular TV Series", isRanked = false, tvSeries))
            }
            if (blockbuster.isNotEmpty()) {
                list.add(HomeCategory("blockbusters", "Blockbuster Movies", isRanked = false, blockbuster))
            }
            if (anime.isNotEmpty()) {
                list.add(HomeCategory("anime", "Anime & Animation", isRanked = false, anime))
            }
            if (action.isNotEmpty()) {
                list.add(HomeCategory("action", "Action & Adventure", isRanked = false, action))
            }
        }

        cachedCategories = list
        list
    }
}
