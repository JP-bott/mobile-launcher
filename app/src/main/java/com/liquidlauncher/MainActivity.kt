package com.liquidlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var refreshToken by mutableIntStateOf(0)
    override fun onResume() {
        super.onResume()
        refreshToken++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LauncherStore(applicationContext)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0
        setContent {
            MaterialTheme {
                val apps by produceState<List<LauncherApp>>(emptyList(), refreshToken) {
                    value = withContext(Dispatchers.IO) { store.installedApps() }
                }
                LauncherV5(apps = apps, store = store)
            }
        }
    }
}
