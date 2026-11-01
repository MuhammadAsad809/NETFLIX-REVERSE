package com.netflix.moviebox.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Netflix-grade persistent media cache manager.
 * Implements chunk-level caching with LRU eviction and connection-pooled OkHttp networking.
 */
@OptIn(UnstableApi::class)
object VideoCacheManager {

    @Volatile
    private var simpleCache: SimpleCache? = null

    // 512MB dedicated local LRU media cache for zero-lag scrubbing and instant re-plays
    private const val MAX_CACHE_SIZE = 512L * 1024L * 1024L

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: run {
            val cacheDir = File(context.applicationContext.cacheDir, "netflix_media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            SimpleCache(cacheDir, evictor, databaseProvider).also {
                simpleCache = it
            }
        }
    }

    fun buildDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://themoviebox.xyz/",
                    "Origin" to "https://themoviebox.xyz",
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Netflix-style stream pre-fetcher.
     * Pre-warms the first 2MB into the local cache before user taps Play.
     */
    fun prefetchStream(context: Context, streamUrl: String, prefetchBytes: Long = 2L * 1024L * 1024L) {
        if (streamUrl.isBlank()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val cache = getCache(context)
                val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
                    .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
                    .setDefaultRequestProperties(
                        mapOf(
                            "Referer" to "https://themoviebox.xyz/",
                            "Origin" to "https://themoviebox.xyz"
                        )
                    )
                val upstream = okHttpFactory.createDataSource()
                val cacheDataSource = CacheDataSource(cache, upstream)
                val dataSpec = androidx.media3.datasource.DataSpec(android.net.Uri.parse(streamUrl), 0, prefetchBytes)
                val cacheWriter = androidx.media3.datasource.cache.CacheWriter(cacheDataSource, dataSpec, null, null)
                cacheWriter.cache()
            } catch (e: Exception) {
                // Non-fatal prefetch failure
            }
        }
    }

    fun releaseCache() {
        try {
            simpleCache?.release()
            simpleCache = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
