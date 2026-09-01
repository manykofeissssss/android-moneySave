package com.example.billkeeper.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.example.billkeeper.background.AppearancePreferences
import com.example.billkeeper.background.AppearanceSettings
import com.example.billkeeper.background.BackgroundPreferences
import com.example.billkeeper.viewmodel.DailyLedgerViewModel
import com.example.billkeeper.viewmodel.LedgerViewModel
import com.example.billkeeper.viewmodel.RecurringEntryViewModel
import com.xah.navigation.component.SharedNavHost
import com.xah.navigation.component.rememberNavController
import com.xah.navigation.util.rememberNavDependencies
import kotlinx.coroutines.launch

@Composable
fun BillKeeperNavigation(
    ledgerViewModel: LedgerViewModel,
    recurringEntryViewModel: RecurringEntryViewModel,
    dailyLedgerViewModel: DailyLedgerViewModel,
    appearanceSettings: AppearanceSettings,
    appearancePreferences: AppearancePreferences,
    backgroundPreferences: BackgroundPreferences,
    darkTheme: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackgroundChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dependencies = rememberNavDependencies(
        ledgerViewModel,
        recurringEntryViewModel,
        dailyLedgerViewModel,
        appearanceSettings,
        appearancePreferences,
        backgroundPreferences,
        darkTheme,
        snackbarHostState,
        onBackgroundChanged
    ) {
        put(
            BillKeeperNavDependencies(
                ledgerViewModel = ledgerViewModel,
                recurringEntryViewModel = recurringEntryViewModel,
                dailyLedgerViewModel = dailyLedgerViewModel,
                appearanceSettings = appearanceSettings,
                appearancePreferences = appearancePreferences,
                backgroundPreferences = backgroundPreferences,
                darkTheme = darkTheme,
                snackbarHostState = snackbarHostState,
                onBackgroundChanged = {
                    onBackgroundChanged()
                    scope.launch {
                        snackbarHostState.showSnackbar("外观设置已应用")
                    }
                }
            )
        )
    }

    val navController = rememberNavController(
        startDestination = HomeDestination
    )

    SharedNavHost(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
        dependencies = dependencies
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeaturePageScaffold(
    title: String,
    titleIcon: ImageVector? = null,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        titleIcon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null
                            )
                            Text(title, modifier = Modifier.padding(start = 8.dp))
                        } ?: Text(title)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回更多功能"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            content()
        }
    }
}
