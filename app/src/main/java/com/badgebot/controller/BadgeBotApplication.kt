package com.badgebot.controller

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class BadgeBotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load the bundled OpenCV native library for ArUco marker detection.
        if (OpenCVLoader.initLocal()) {
            Log.i("BadgeBot", "OpenCV loaded")
        } else {
            Log.e("BadgeBot", "OpenCV failed to load; camera path feature disabled")
        }
    }
}
