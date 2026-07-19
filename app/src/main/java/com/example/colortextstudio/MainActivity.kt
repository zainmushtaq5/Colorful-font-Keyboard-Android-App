package com.example.colortextstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.colortextstudio.ui.EditorScreen
import com.example.colortextstudio.ui.GalleryScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColorTextStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "editor") {
        composable("editor") {
            EditorScreen(onOpenGallery = { navController.navigate("gallery") })
        }
        composable("gallery") {
            GalleryScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun ColorTextStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
