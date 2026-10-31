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
    private var cachedHero: MovieBoxItem? = null
    private var cachedCategories: List<HomeCategory> = emptyList()

    suspend fun getFeaturedHero(): MovieBoxItem = withContext(Dispatchers.IO) {
        cachedHero?.let { return@withContext it }

        // 1. Try to get hero with trailer from genuine MovieBox home feed
        val feed = MovieBoxClient.getHomeFeed()
        val feedHero = feed.heroBanner ?: feed.sections.values.flatten().firstOrNull { !it.trailerUrl.isNullOrEmpty() }

        val hero = feedHero ?: run {
            val items = MovieBoxClient.searchMovies("Trending", page = 1, perPage = 6)
            items.firstOrNull { !it.stillsUrl.isNullOrEmpty() }
                ?: items.firstOrNull { it.coverUrl.isNotEmpty() }
                ?: MovieBoxItem(
                    subjectId = "hero-default",
                    title = "Avatar: The Way of Water",
                    coverUrl = "https://pbcdnw.aoneroom.com/image/2026/01/27/e9297d39584c33b5ded66cff74a31dc8.jpg",
                    stillsUrl = "https://pbcdnw.aoneroom.com/image/2022/09/09/1bb7aa281a086492c0b2a0eb354bb4bc.jpg",
                    trailerUrl = "https://macdn.aoneroom.com/media/vone/2024/08/07/4824e7b3b80a3e630ce1b91b5cc00670.mp4",
                    score = 9.2,
                    releaseDate = "2024",
                    description = "Jake Sully lives with his newfound family formed on the extrasolar moon Pandora. Once a familiar threat returns to finish what was previously started, Jake must work with Neytiri and the army of the Na'vi race to protect their home.",
                    detectedLang = "English"
                )
        }
        cachedHero = hero
        hero
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

        // 2. Fallback to search queries if feed was empty
        if (list.isEmpty()) {
            val trendingDeferred = async { MovieBoxClient.searchMovies("Trending", page = 1, perPage = 12) }
            val top10Deferred = async { MovieBoxClient.searchMovies("Top", page = 1, perPage = 10) }
            val tvSeriesDeferred = async { MovieBoxClient.searchMovies("Series", subjectType = 2, page = 1, perPage = 12) }
            val blockbusterDeferred = async { MovieBoxClient.searchMovies("Marvel", subjectType = 1, page = 1, perPage = 12) }
            val animeDeferred = async { MovieBoxClient.searchMovies("Anime", subjectType = 6, page = 1, perPage = 12) }
            val actionDeferred = async { MovieBoxClient.searchMovies("Action", page = 1, perPage = 12) }

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
