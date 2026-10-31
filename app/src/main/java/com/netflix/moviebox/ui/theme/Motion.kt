package com.netflix.moviebox.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

object NetflixMotion {
    const val PLAY_PAUSE_DURATION_MS = 350
    const val MY_LIST_DURATION_MS = 516
    const val BUFFER_SPINNER_LOOP_MS = 1000
    const val SHIMMER_SWEEP_DURATION_MS = 1200
    const val CONTROLS_AUTOHIDE_DELAY_MS = 3000L

    val DecelerateEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)
}
