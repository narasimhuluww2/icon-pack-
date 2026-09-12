package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreenPreviewScreen
import com.example.ui.theme.MonoIconTheme
import com.example.ui.theme.NoirBlack
import com.example.ui.viewmodel.IconEditorViewModel
import com.example.ui.viewmodel.ScreenState

class MainActivity : ComponentActivity() {
  private val viewModel: IconEditorViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MonoIconTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = NoirBlack
        ) {
          val currentScreen by viewModel.screenState.collectAsState()

          AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
              fadeIn() togetherWith fadeOut()
            },
            label = "ScreenTransition"
          ) { screen ->
            when (screen) {
              ScreenState.DASHBOARD -> DashboardScreen(viewModel = viewModel)
              ScreenState.EDITOR -> EditorScreen(viewModel = viewModel)
              ScreenState.HOME_SCREEN_PREVIEW -> HomeScreenPreviewScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}

