package com.edgeai.wearable

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.edgeai.wearable.ui.WearableApp
import com.edgeai.wearable.ui.theme.EdgeAIWearableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        setContent {
            EdgeAIWearableTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    WearableApp()
                }
            }
        }
    }
}
