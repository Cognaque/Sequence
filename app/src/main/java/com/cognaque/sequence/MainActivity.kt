@file:OptIn(ExperimentalMaterial3Api::class)

package com.cognaque.sequence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cognaque.sequence.data.AppColors
import com.cognaque.sequence.ui.TaskViewModel
import com.cognaque.sequence.ui.TaskViewModelFactory
import com.cognaque.sequence.ui.components.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val viewModel: TaskViewModel = viewModel(factory = TaskViewModelFactory(context.applicationContext))
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.checkAndGenerateDailyChores()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Background) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppColors.Primary,
            secondary = AppColors.Secondary,
            surface = AppColors.Surface,
            background = AppColors.Background,
            tertiary = AppColors.Tertiary,
            error = AppColors.Error
        ),
        content = content
    )
}
