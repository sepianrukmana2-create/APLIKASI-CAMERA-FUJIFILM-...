package com.example.filmcam

import android.graphics.*
import kotlin.math.hypot
import kotlin.random.Random

/** Menerapkan tone warna + grain + vignette ke foto hasil jepretan. */
object FilmProcessor {
    fun process(src: Bitmap, rotation: Int, sim: FilmSim): Bitmap {
        val rot = Matrix().apply { postRotate(rotation.toFloat()) }
        val up = Bitmap.createBitmap(src, 0, 0, src.width, src.height, rot, true)
        val out = Bitmap.createBitmap(up.width, up.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val w = out.width.toFloat(); val h = out.height.toFloat()

        c.drawBitmap(up, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(sim.matrix())
        })

        if (sim.grain > 0f) {
            val tile = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            tile.setPixels(IntArray(256 * 256) { val g = Random.nextInt(256); Color.rgb(g, g, g) }, 0, 256, 0, 0, 256, 256)
            val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            shader.setLocalMatrix(Matrix().apply { setScale(2f, 2f) })
            c.drawRect(0f, 0f, w, h, Paint().apply {
                this.shader = shader
                xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                alpha = (sim.grain * 255).toInt()
            })
        }
        if (sim.vignette > 0f) {
            c.drawRect(0f, 0f, w, h, Paint().apply {
                shader = RadialGradient(w / 2, h / 2, hypot(w, h) / 2,
                    intArrayOf(0, 0, Color.argb((sim.vignette * 255).toInt(), 0, 0, 0)),
                    floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            })
        }
        return out
    }
}
