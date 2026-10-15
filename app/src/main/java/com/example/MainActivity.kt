package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.example.ui.AppUI
import com.example.ui.AppViewModel
import com.example.ui.AuthScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
  private val viewModel: AppViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    android.util.Log.d("MainActivity", "onCreate start")
    
    try {
      com.example.ui.NotificationHelper.createNotificationChannel(this)
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Notification channel error", e)
    }
    
    try {
      java.util.Locale.setDefault(java.util.Locale.US)
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Locale error", e)
    }

    // Set uncaught exception handler to log errors safely
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      android.util.Log.e("MainActivityCrash", "Uncaught exception on thread ${thread.name}", throwable)
      defaultHandler?.uncaughtException(thread, throwable)
    }

    android.util.Log.d("MainActivity", "before enableEdgeToEdge")
    enableEdgeToEdge()
    android.util.Log.d("MainActivity", "after enableEdgeToEdge")
    
    setContent {
        android.util.Log.d("MainActivity", "setContent start")
      val isDarkMode by viewModel.isDarkMode.collectAsState()
      MyApplicationTheme(darkTheme = isDarkMode) {
        CompositionLocalProvider(
          LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                var showSplash by remember { mutableStateOf(true) }
                var hasError by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }

                if (hasError) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            text = "حدث خطأ أثناء تشغيل التطبيق",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(onClick = {
                            hasError = false
                            showSplash = true
                        }) {
                            androidx.compose.material3.Text("إعادة المحاولة ")
                        }
                    }
                } else {
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "splashTransition"
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(viewModel = viewModel, onSplashFinished = { showSplash = false })
                        } else {
                            val userAccount by viewModel.currentUserAccount.collectAsState()
                            if (userAccount == null) {
                                AuthScreen(viewModel)
                            } else {
                                AppUI(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
      }
    }
  }
}

