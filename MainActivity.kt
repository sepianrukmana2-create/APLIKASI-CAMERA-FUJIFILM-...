package com.example.filmcam

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Color
import android.graphics.RenderEffect
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var preview: PreviewView
    private lateinit var label: TextView
    private var capture: ImageCapture? = null
    private var sim = FilmSims.all[0]
    private var back = true

    private val perm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startCamera() else finish()
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preview = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE // wajib agar RenderEffect berlaku
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        label = TextView(this).apply { setTextColor(Color.WHITE); textSize = 16f; gravity = Gravity.CENTER; text = sim.name }

        val chips = LinearLayout(this).apply {
            FilmSims.all.forEach { s ->
                addView(Button(context).apply {
                    text = s.name; isAllCaps = false
                    setOnClickListener { sim = s; label.text = s.name; applyLive() }
                })
            }
        }
        val shutter = Button(this).apply { text = "● Ambil"; setOnClickListener { takePhoto() } }
        val flip = Button(this).apply { text = "Putar"; setOnClickListener { back = !back; startCamera() } }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER; addView(flip); addView(shutter) }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(0x88000000.toInt()); setPadding(8.dp(), 8.dp(), 8.dp(), 24.dp())
            addView(label); addView(HorizontalScrollView(context).apply { addView(chips) }); addView(row)
        }
        setContentView(FrameLayout(this).apply {
            addView(preview, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(bottom, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
        })

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else perm.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val pv = Preview.Builder().build().also { it.setSurfaceProvider(preview.surfaceProvider) }
            val ic = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            capture = ic
            provider.unbindAll()
            provider.bindToLifecycle(this,
                if (back) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA, pv, ic)
            applyLive()
        }, ContextCompat.getMainExecutor(this))
    }

    /** Preview live: tone warna dijalankan di GPU lewat RenderEffect (Android 12+). */
    private fun applyLive() {
        preview.setRenderEffect(RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(sim.matrix())))
    }

    private fun takePhoto() {
        val ic = capture ?: return
        val chosen = sim
        ic.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bmp = image.toBitmap(); val rot = image.imageInfo.rotationDegrees; image.close()
                Thread { save(FilmProcessor.process(bmp, rot, chosen)) }.start()
            }
            override fun onError(e: ImageCaptureException) {
                Toast.makeText(this@MainActivity, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun save(bmp: Bitmap) {
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "FILM_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FilmCam")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: return
        contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        runOnUiThread { Toast.makeText(this, "Tersimpan di Pictures/FilmCam", Toast.LENGTH_SHORT).show() }
    }
}
