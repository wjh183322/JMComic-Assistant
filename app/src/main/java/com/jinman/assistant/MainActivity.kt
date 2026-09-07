package com.jinman.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jinman.assistant.ui.App
import com.jinman.assistant.ui.JinmanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ScoutViewModel = viewModel()
            var resumeTick by remember { mutableIntStateOf(0) }
            LifecycleResumeEffect(Unit) {
                resumeTick += 1
                onPauseOrDispose { }
            }
            JinmanTheme {
                App(vm, resumeTick)
            }
        }
    }
}
