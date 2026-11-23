package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.firebase.configureAppCheck
import com.example.service.RadioPlaybackService
import com.example.ui.AutoTunerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            AutoTunerApp()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            runCatching {
                stopService(Intent(this, RadioPlaybackService::class.java))
            }
        }
        super.onDestroy()
    }
}
