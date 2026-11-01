package com.netflix.moviebox.ui.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Robust persistent media cache and prefetch manager.
 * Implements chunk-level caching with LRU eviction, duplicate-prefetch protection,
 * and player reference-counted lifecycle coordination.
 */
@OptIn(UnstableApi::class)
object VideoCacheManager {

    private const val TAG = "VideoCacheManager"

    @Volatile
    private var simpleCache: SimpleCache? = null

    // 256MB dedicated local LRU media cache (balanced for mobile storage)
    private const val MAX_CACHE_SIZE = 256L * 1024L * 1024L

    // Player reference counting to prevent premature cache eviction during active playback
    private val activePlayers = AtomicInteger(0)

    // Managed CoroutineScope for all background stream prefetching
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Duplicate-prefetch in-flight protection
    private val prefetchJobs = ConcurrentHashMap.newKeySet<String>()

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

    fun buildDataSourceFactory(
        context: Context,
        customHeaders: Map<String, String>? = null
    ): DataSource.Factory {
        val cache = getCache(context)

        val defaultHeaders = mapOf(
            "Referer" to "https://themoviebox.xyz/",
            "Origin" to "https://themoviebox.xyz",
            "Accept" to "*/*",
            "Connection" to "keep-alive"
        )

        val headers = customHeaders ?: defaultHeaders

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
            .setDefaultRequestProperties(headers)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Managed stream pre-fetcher.
     * Pre-warms the first [prefetchBytes] into the local cache before user taps Play.
     * Includes deduplication, validation, and error logging.
     */
    fun prefetchStream(
        context: Context,
        streamUrl: String,
        prefetchBytes: Long = 2L * 1024L * 1024L
    ) {
        if (streamUrl.isBlank() || prefetchBytes <= 0L) return

        // Prevent duplicate prefetch for the same stream URL
        if (!prefetchJobs.add(streamUrl)) return

        prefetchScope.launch {
            try {
                val cache = getCache(context)
                val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
                    .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
                    .setDefaultRequestProperties(
                        mapOf(
                            "Referer" to "https://themoviebox.xyz/",
                            "Origin" to "https://themoviebox.xyz",
                            "Accept" to "*/*",
                            "Connection" to "keep-alive"
                        )
                    )

                val factory = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(okHttpFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                val dataSource = factory.createDataSource()

                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(streamUrl))
                    .setPosition(0)
                    .setLength(prefetchBytes)
                    .build()

                val writer = CacheWriter(
                    dataSource,
                    dataSpec,
                    null,
                    null
                )

                writer.cache()
                Log.d(TAG, "Prefetched $prefetchBytes bytes for $streamUrl")
            } catch (e: Exception) {
                Log.w(TAG, "Stream prefetch failed for $streamUrl: ${e.message}")
            } finally {
                prefetchJobs.remove(streamUrl)
            }
        }
    }

    fun registerPlayer() {
        activePlayers.incrementAndGet()
    }

    fun unregisterPlayer() {
        val remaining = activePlayers.decrementAndGet()
        if (remaining <= 0) {
            activePlayers.set(0)
        }
    }

    fun releaseCache() {
        if (activePlayers.get() > 0) {
            Log.w(TAG, "Skipping cache release: ${activePlayers.get()} players still active.")
            return
        }
        synchronized(this) {
            try {
                simpleCache?.release()
                simpleCache = null
                Log.d(TAG, "Media cache successfully released.")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing media cache", e)
            }
        }
    }
}
