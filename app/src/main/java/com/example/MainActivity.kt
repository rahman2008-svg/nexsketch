package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DrawingStudioScreen
import com.example.ui.DrawingViewModel
import com.example.ui.ProjectGalleryScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: DrawingViewModel = viewModel()
        val isGalleryMode by viewModel.isGalleryMode.collectAsState()

        if (isGalleryMode) {
          ProjectGalleryScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        } else {
          DrawingStudioScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }
      }
    }
  }
}
