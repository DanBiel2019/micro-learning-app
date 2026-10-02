package com.example.aigeneratedandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aigeneratedandroid.microlearning.ui.App
import com.example.aigeneratedandroid.microlearning.ui.theme.MicroLearningTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MicroLearningTheme { App() }
        }
    }
}
