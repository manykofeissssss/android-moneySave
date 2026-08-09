package com.example.billkeeper.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billkeeper.BillKeeperApplication
import com.example.billkeeper.background.AppearancePreferences
import com.example.billkeeper.background.BackgroundStyle
import com.example.billkeeper.ui.theme.BillKeeperTheme
import com.example.billkeeper.viewmodel.LedgerViewModel
import com.example.billkeeper.viewmodel.LedgerViewModelFactory
import com.example.billkeeper.viewmodel.RecurringEntryViewModel
import com.example.billkeeper.viewmodel.RecurringEntryViewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val appearancePreferences by lazy { AppearancePreferences(applicationContext) }
    private val vm: LedgerViewModel by viewModels {
        LedgerViewModelFactory((application as BillKeeperApplication).repository)
    }
    private val recurringVm: RecurringEntryViewModel by viewModels {
        val app = application as BillKeeperApplication
        RecurringEntryViewModelFactory(app.repository, app.recurringScheduler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val appearanceSettings by appearancePreferences.settings.collectAsStateWithLifecycle(
                initialValue = appearancePreferences.initialSettings
            )
            val darkTheme = isSystemInDarkTheme()

            LaunchedEffect(Unit) {
                delay(1500)
                showSplash = false
            }

            BillKeeperTheme(
                darkTheme = darkTheme,
                themeSeedArgb = appearanceSettings.themeSeedArgb.takeUnless { darkTheme },
                backgroundColorArgb = appearanceSettings.backgroundColorArgb.takeIf {
                    !darkTheme && appearanceSettings.backgroundStyle == BackgroundStyle.SOLID_COLOR
                }
            ) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    BillKeeperApp(
                        vm = vm,
                        recurringVm = recurringVm,
                        appearanceSettings = appearanceSettings,
                        appearancePreferences = appearancePreferences,
                        darkTheme = darkTheme
                    )
                }
            }
        }
    }
}
