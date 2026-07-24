package com.example.billkeeper.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.billkeeper.BillKeeperApplication
import com.example.billkeeper.viewmodel.LedgerViewModel
import com.example.billkeeper.viewmodel.LedgerViewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: LedgerViewModel by viewModels {
        LedgerViewModelFactory((application as BillKeeperApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1500)
                showSplash = false
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1B5E20),
                    secondary = Color(0xFF2E7D32),
                    surface = Color(0xFFF5F5F5),
                    background = Color(0xFFFAFAFA)
                )
            ) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    BillKeeperApp(vm)
                }
            }
        }
    }
}
