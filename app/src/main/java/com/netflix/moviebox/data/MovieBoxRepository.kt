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

        // Fetch top trending item from MovieBox
        val trending = MovieBoxClient.searchMovies("Avatar", page = 1, perPage = 1)
        val hero = trending.firstOrNull() ?: MovieBoxItem(
            subjectId = "hero-default",
            title = "Avatar: The Way of Water",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000&q=80",
            score = 9.2,
            releaseDate = "2024",
            description = "Jake Sully lives with his newfound family formed on the extrasolar moon Pandora. Once a familiar threat returns to finish what was previously started, Jake must work with Neytiri and the army of the Na'vi race to protect their home.",
            detectedLang = "English"
        )
        cachedHero = hero
        hero
    }

    suspend fun getHomeCategories(): List<HomeCategory> = withContext(Dispatchers.IO) {
        if (cachedCategories.isNotEmpty()) return@withContext cachedCategories

        val trendingDeferred = async { MovieBoxClient.searchMovies("Action", page = 1, perPage = 10) }
        val top10Deferred = async { MovieBoxClient.searchMovies("2024", page = 1, perPage = 10) }
        val hindiDeferred = async { MovieBoxClient.searchMovies("Hindi", langFilter = "Hindi", page = 1, perPage = 10) }
        val urduDeferred = async { MovieBoxClient.searchMovies("Urdu", langFilter = "Urdu", page = 1, perPage = 8) }
        val sciFiDeferred = async { MovieBoxClient.searchMovies("Marvel", page = 1, perPage = 10) }

        val trending = trendingDeferred.await()
        val top10 = top10Deferred.await()
        val hindi = hindiDeferred.await()
        val urdu = urduDeferred.await()
        val sciFi = sciFiDeferred.await()

        val list = mutableListOf<HomeCategory>()

        if (trending.isNotEmpty()) {
            list.add(HomeCategory("trending", "Trending Now", isRanked = false, trending))
        }
        if (top10.isNotEmpty()) {
            list.add(HomeCategory("top10", "Top 10 in Your Country Today", isRanked = true, top10))
        }
        if (hindi.isNotEmpty()) {
            list.add(HomeCategory("hindi", "Bollywood & Hindi Dubbed", isRanked = false, hindi))
        }
        if (urdu.isNotEmpty()) {
            list.add(HomeCategory("urdu", "Urdu Dubbed Hits", isRanked = false, urdu))
        }
        if (sciFi.isNotEmpty()) {
            list.add(HomeCategory("scifi", "Sci-Fi & Superhero Action", isRanked = false, sciFi))
        }

        cachedCategories = list
        list
    }
}
