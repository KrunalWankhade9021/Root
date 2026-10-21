package com.krunal.rootd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.krunal.rootd.ui.nav.RootdNavHost
import com.krunal.rootd.ui.theme.RootdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RootdTheme {
                RootdNavHost()
            }
        }
    }
}
