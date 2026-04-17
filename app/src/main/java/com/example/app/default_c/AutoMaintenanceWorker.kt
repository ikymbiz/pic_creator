package com.example.coreapp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class AutoMaintenanceWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("CoreConfig", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("auto_wipe_enabled", true)
        val lastTime = prefs.getLong("last_interaction", System.currentTimeMillis())
        
        if (isEnabled && (System.currentTimeMillis() - lastTime > 24 * 60 * 60 * 1000)) {
            applicationContext.filesDir.listFiles()?.forEach { it.delete() }
            applicationContext.cacheDir.deleteRecursively()
        }
        return Result.success()
    }
}