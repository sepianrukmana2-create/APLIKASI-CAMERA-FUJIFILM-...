package com.example.filmcam

import android.graphics.ColorMatrix

/** Satu "film simulation": saturasi, kontras, lift bayangan, tint RGB, grain, vignette. */
data class FilmSim(
    val name: String, val sat: Float, val contrast: Float, val lift: Float,
    val r: Float, val g: Float, val b: Float, val grain: Float, val vignette: Float
) {
    fun matrix(): ColorMatrix {
        val m = ColorMatrix().apply { setSaturation(sat) }
        val off = 128f * (1f - contrast) + lift
        m.postConcat(ColorMatrix(floatArrayOf(
            contrast * r, 0f, 0f, 0f, off,
            0f, contrast * g, 0f, 0f, off,
            0f, 0f, contrast * b, 0f, off,
            0f, 0f, 0f, 1f, 0f)))
        return m
    }
}

object FilmSims {
    val all = listOf(
        FilmSim("Standar",        1.05f, 1.05f,  0f, 1.00f, 1.00f, 1.00f, 0.05f, 0.10f),
        FilmSim("Vivid Slide",    1.35f, 1.15f, -4f, 1.02f, 1.00f, 1.00f, 0.05f, 0.15f),
        FilmSim("Lembut",         1.00f, 0.95f,  3f, 1.02f, 1.00f, 0.98f, 0.05f, 0.10f),
        FilmSim("Chrome Klasik",  0.80f, 1.12f,  4f, 0.97f, 1.00f, 1.04f, 0.12f, 0.20f),
        FilmSim("Negatif Klasik", 1.00f, 1.18f,  6f, 1.03f, 1.00f, 0.95f, 0.15f, 0.20f),
        FilmSim("Sinema",         0.75f, 0.90f,  8f, 1.00f, 1.00f, 1.02f, 0.08f, 0.10f),
        FilmSim("Mono",           0.00f, 1.20f,  0f, 1.00f, 1.00f, 1.00f, 0.30f, 0.20f)
    )
}
