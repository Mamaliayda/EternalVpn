package com.eternalvpn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eternalvpn.app.ui.navigation.AppNavHost
import com.eternalvpn.app.ui.theme.EternalVpnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EternalVpnTheme {
                AppNavHost()
            }
        }
    }
}
