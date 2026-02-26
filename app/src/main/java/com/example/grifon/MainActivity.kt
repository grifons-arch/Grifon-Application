package com.example.grifon

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.grifon.ui.GrifonApp
import com.example.grifon.ui.theme.GrifonTheme
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("CrashLog", "MainActivity onCreate: Starting...")
        super.onCreate(savedInstanceState)
        try {
            setContent {
                Log.d("CrashLog", "MainActivity: Setting Compose content")
                GrifonTheme {
                    GrifonApp()
                }
            }
        } catch (e: Exception) {
            Log.e("CrashLog", "MainActivity CRASH in setContent", e)
        }
    }
}
