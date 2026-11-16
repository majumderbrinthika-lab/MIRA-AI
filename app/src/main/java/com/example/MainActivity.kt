package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MiraRootScreen
import com.example.ui.MiraViewModel
import com.example.ui.theme.MiraTheme

class MainActivity : ComponentActivity() {
    private val miraViewModel: MiraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by miraViewModel.uiState.collectAsStateWithLifecycle()
            MiraTheme(darkTheme = state.preferences.darkTheme) {
                MiraRootScreen(
                    state = state,
                    viewModel = miraViewModel
                )
            }
        }
    }
}
