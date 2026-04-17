package com.example.coreapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("CoreConfig", Context.MODE_PRIVATE)

        switchCamera.isChecked = prefs.getBoolean("use_front", false)
        switchPreview.isChecked = prefs.getBoolean("show_preview", true)
        switchAutoWipe.isChecked = prefs.getBoolean("auto_wipe_enabled", true)

        btnSave.setOnClickListener {
            prefs.edit().apply {
                putBoolean("use_front", switchCamera.isChecked)
                putBoolean("show_preview", switchPreview.isChecked)
                putBoolean("auto_wipe_enabled", switchAutoWipe.isChecked)
                apply()
            }
            finish()
        }
    }
}