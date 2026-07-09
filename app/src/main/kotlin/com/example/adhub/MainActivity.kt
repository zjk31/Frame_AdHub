package com.example.adhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.adhub.ui.AdHubApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val hotStartManager: com.example.adhub.core.HotStartInterstitialManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdHubApp()
        }
        hotStartManager.bindActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        hotStartManager.unbindActivity(this)
    }
}
