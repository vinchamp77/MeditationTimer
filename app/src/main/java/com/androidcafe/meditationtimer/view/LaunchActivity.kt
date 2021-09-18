package com.androidcafe.meditationtimer.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainIntent = Intent(this, MainActivity::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //mainIntent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
            mainIntent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;
        }

        startActivity(mainIntent)
    }
}
