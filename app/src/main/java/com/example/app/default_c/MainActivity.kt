package com.example.coreapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.work.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var recording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private val masterKey by lazy { MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateInteractionTime()
        scheduleMaintenance()

        val prefs = getSharedPreferences("CoreConfig", Context.MODE_PRIVATE)
        val useFront = prefs.getBoolean("use_front", false)
        val showPreview = prefs.getBoolean("show_preview", true)

        viewFinder.visibility = if (showPreview) View.VISIBLE else View.INVISIBLE
        standbyLayer.visibility = if (showPreview) View.GONE else View.VISIBLE

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCamera(useFront)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }

        setupControls()
    }

    private fun initCamera(useFront: Boolean) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
            videoCapture = VideoCapture.withOutput(recorder)

            val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, preview, imageCapture, videoCapture)
                startAutoRecording()
            } catch (e: Exception) {}
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startAutoRecording() {
        val outputOptions = MediaStoreOutputOptions.Builder(contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI).build()
        recording = videoCapture?.output?.prepareRecording(this, outputOptions)?.start(ContextCompat.getMainExecutor(this)) {}
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            updateInteractionTime()
            takeEncryptedPhoto()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun takeEncryptedPhoto() {
        imageCapture?.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnByteArrayCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                saveEncrypted(bytes)
                image.close()
            }
            override fun onError(e: ImageCaptureException) {}
        })
    }

    private fun saveEncrypted(bytes: ByteArray) {
        val file = File(filesDir, "DAT_${System.currentTimeMillis()}.dat")
        val encryptedFile = EncryptedFile.Builder(this, file, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
        encryptedFile.openFileOutput().use { it.write(bytes) }
    }

    private fun setupControls() {
        val barGesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                return true
            }
        })

        primaryBar.setOnTouchListener { _, event ->
            barGesture.onTouchEvent(event)
            true
        }

        primaryBar.setOnLongClickListener {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
            true
        }

        var taps = 0
        var lastTap = 0L
        rootContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                updateInteractionTime()
                val now = System.currentTimeMillis()
                if (now - lastTap < 400) taps++ else taps = 1
                lastTap = now
                if (taps == 3) {
                    recording?.stop()
                    finishAffinity()
                }
            }
            true
        }

        removalSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {}
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar) {
                if (s.progress > 95) {
                    recording?.stop()
                    filesDir.listFiles()?.forEach { it.delete() }
                    cacheDir.deleteRecursively()
                    startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    finishAndRemoveTask()
                } else {
                    s.progress = 0
                }
            }
        })
    }

    private fun updateInteractionTime() {
        getSharedPreferences("CoreConfig", Context.MODE_PRIVATE)
            .edit().putLong("last_interaction", System.currentTimeMillis()).apply()
    }

    private fun scheduleMaintenance() {
        val request = PeriodicWorkRequestBuilder<AutoMaintenanceWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("MaintenanceJob", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}