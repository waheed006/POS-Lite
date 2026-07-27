package com.gembyte.poslite.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gembyte.poslite.ui.app.App
import com.gembyte.poslite.ui.theme.POSLiteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            POSLiteTheme {
                App()
            }
        }
    }
}