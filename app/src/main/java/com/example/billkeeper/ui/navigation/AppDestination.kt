package com.example.billkeeper.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppDestination : NavKey

@Serializable
data object HomeDestination : AppDestination

@Serializable
data object MoreFeaturesDestination : AppDestination

@Serializable
data object DailyLedgerDestination : AppDestination

@Serializable
data object RecurringEntriesDestination : AppDestination

@Serializable
data object ReminderSettingsDestination : AppDestination

@Serializable
data object AppearanceSettingsDestination : AppDestination
