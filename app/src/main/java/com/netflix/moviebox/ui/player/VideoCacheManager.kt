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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Production-ready persistent media cache and prefetch manager.
 * Implements chunk-level caching with LRU eviction, duplicate-prefetch protection,
 * token-based player lifecycle tracking, and race-free cache release coordination.
 */
@OptIn(UnstableApi::class)
object VideoCacheManager {

    private const val TAG = "VideoCacheManager"

    @Volatile
    private var simpleCache: SimpleCache? = null

    // 256MB dedicated local LRU media cache (balanced for mobile storage)
    private const val MAX_CACHE_SIZE = 256L * 1024L * 1024L

    // Player registration token for lifecycle enforcement
    class PlayerToken internal constructor(val id: String)

    // Set of active player session tokens
    private val activePlayerTokens = ConcurrentHashMap.newKeySet<String>()

    // Counter of active ongoing prefetch tasks
    private val activePrefetchCount = AtomicInteger(0)

    // Managed single scope with SupervisorJob for all background prefetching
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-flight prefetch jobs deduplication
    private val inFlightPrefetches = ConcurrentHashMap.newKeySet<String>()

    // Persistent set of already prefetched stream URLs in current session
    private val completedPrefetches = ConcurrentHashMap.newKeySet<String>()

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
    fun getCache(context: Context): SimpleCache? {
        if (simpleCache != null) return simpleCache
        return try {
            val cacheDir = File(context.applicationContext.cacheDir, "netflix_media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            SimpleCache(cacheDir, evictor, databaseProvider).also {
                simpleCache = it
            }
        } catch (t: Throwable) {
            Log.w(TAG, "SimpleCache initialization failed, fallback to direct streaming: ${t.message}")
            null
        }
    }

    fun buildDataSourceFactory(
        context: Context,
        customHeaders: Map<String, String>? = null
    ): DataSource.Factory {
        val defaultHeaders = mapOf(
            "Referer" to "https://themoviebox.xyz/",
            "Origin" to "https://themoviebox.xyz",
            "Accept" to "*/*",
            "Connection" to "keep-alive"
        )

        // Custom headers safely override or augment defaults without stripping essential headers
        val headers = defaultHeaders + (customHeaders ?: emptyMap())

        // 1. High-speed, rock-solid DefaultHttpDataSource with VLC headers & cross-protocol redirects
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(25000)

        // 2. Safe Cache wrapper: if SimpleCache is available without error, wrap with CacheDataSource.
        // If anything fails during cache initialization (folder locked, SQLite locked, etc.),
        // fall back smoothly to direct httpDataSourceFactory without throwing!
        return try {
            val cache = getCache(context)
            if (cache != null) {
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } else {
                httpDataSourceFactory
            }
        } catch (t: Throwable) {
            Log.w(TAG, "CacheDataSource initialization bypassed: ${t.message}")
            httpDataSourceFactory
        }
    }

    /**
     * Managed stream pre-fetcher:
     * - Fast check: skip if already completed in session
     * - SimpleCache span check: skip if bytes already cached on disk
     * - In-flight deduplication
     * - Active prefetch tracking to guard releaseCache()
     */
    fun prefetchStream(
        context: Context,
        streamUrl: String,
        prefetchBytes: Long = 2L * 1024L * 1024L,
        customHeaders: Map<String, String>? = null
    ) {
        if (streamUrl.isBlank() || prefetchBytes <= 0L) return

        // 1. Fast check if already completed
        if (completedPrefetches.contains(streamUrl)) return

        // 2. Safe cache check
        val cache = try {
            getCache(context)
        } catch (e: Throwable) {
            null
        } ?: return

        try {
            if (cache.isCached(streamUrl, 0, prefetchBytes)) {
                completedPrefetches.add(streamUrl)
                return
            }
        } catch (e: Throwable) {
            // cache read error, continue
        }

        // 3. In-flight duplicate prevention
        if (!inFlightPrefetches.add(streamUrl)) return

        activePrefetchCount.incrementAndGet()

        prefetchScope.launch {
            try {
                val currentCache = getCache(context) ?: return@launch
                val defaultHeaders = mapOf(
                    "Referer" to "https://themoviebox.xyz/",
                    "Origin" to "https://themoviebox.xyz",
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
                val headers = defaultHeaders + (customHeaders ?: emptyMap())

                val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                    .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
                    .setDefaultRequestProperties(headers)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(20000)
                    .setReadTimeoutMs(25000)

                val factory = CacheDataSource.Factory()
                    .setCache(currentCache)
                    .setUpstreamDataSourceFactory(httpFactory)
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
                completedPrefetches.add(streamUrl)
                Log.d(TAG, "Prefetched $prefetchBytes bytes for $streamUrl")
            } catch (e: Throwable) {
                Log.w(TAG, "Stream prefetch failed for $streamUrl: ${e.message}")
            } finally {
                inFlightPrefetches.remove(streamUrl)
                activePrefetchCount.decrementAndGet()
            }
        }
    }

    /**
     * Registers a player session with a unique token.
     * Guaranteed safe: prevents releaseCache() while player is alive.
     */
    fun registerPlayer(): PlayerToken {
        val tokenId = UUID.randomUUID().toString()
        activePlayerTokens.add(tokenId)
        Log.d(TAG, "Player registered: $tokenId (total active: ${activePlayerTokens.size})")
        return PlayerToken(tokenId)
    }

    /**
     * Unregisters a player session using its specific token.
     * Prevents double-unregister bugs from corrupting counter state.
     */
    fun unregisterPlayer(token: PlayerToken?) {
        if (token == null) return
        val removed = activePlayerTokens.remove(token.id)
        if (removed) {
            Log.d(TAG, "Player unregistered: ${token.id} (remaining: ${activePlayerTokens.size})")
        } else {
            Log.w(TAG, "Player token ${token.id} was already unregistered or invalid")
        }
    }

    /**
     * Safely releases SimpleCache ONLY when:
     * 1. No active player sessions exist.
     * 2. No background prefetch jobs are writing to the cache.
     */
    fun releaseCache() {
        val activePlayers = activePlayerTokens.size
        val activePrefetches = activePrefetchCount.get()

        if (activePlayers > 0 || activePrefetches > 0) {
            Log.w(
                TAG,
                "Skipping releaseCache: $activePlayers players and $activePrefetches prefetch operations are active."
            )
            return
        }

        synchronized(this) {
            if (activePlayerTokens.size > 0 || activePrefetchCount.get() > 0) {
                return
            }
            try {
                simpleCache?.release()
                simpleCache = null
                completedPrefetches.clear()
                Log.d(TAG, "Media cache successfully released.")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing media cache", e)
            }
        }
    }
}
