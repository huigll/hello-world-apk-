package com.example.helloworld

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: VirtualScreenPresentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openBtn = findViewById<Button>(R.id.btn_open_virtual)
        val container = findViewById<FrameLayout>(R.id.virtual_container)

        openBtn.setOnClickListener {
            if (virtualDisplay == null) {
                openBtn.isEnabled = false
                startVirtualScreen(container) {
                    openBtn.isEnabled = true
                }
            } else {
                stopVirtualScreen()
            }
            openBtn.text = if (virtualDisplay == null) "Open Virtual Screen" else "Close Virtual Screen"
        }
    }

    private fun startVirtualScreen(container: FrameLayout, onReady: () -> Unit) {
        container.removeAllViews()
        val surfaceView = SurfaceView(this)
        container.addView(surfaceView)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                createVirtualDisplay(holder.surface)
                onReady()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // no-op
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopVirtualScreen()
            }
        })
    }

    private fun createVirtualDisplay(surface: Surface) {
        stopVirtualScreen()

        val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val metrics = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }

        val width = (metrics.widthPixels * 0.9).toInt().coerceAtLeast(800)
        val height = (metrics.heightPixels * 0.9).toInt().coerceAtLeast(600)
        val densityDpi = metrics.densityDpi

        virtualDisplay = dm.createVirtualDisplay(
            "hello-world-virtual",
            width,
            height,
            densityDpi,
            surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
        )

        val display = virtualDisplay?.display ?: return
        presentation = VirtualScreenPresentation(this, display).also { it.show() }
    }

    private fun stopVirtualScreen() {
        presentation?.dismiss()
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }

    override fun onDestroy() {
        stopVirtualScreen()
        super.onDestroy()
    }
}
