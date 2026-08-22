package com.example.billkeeper.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun MoreFeaturesScreen(
    onOpenDailyLedger: () -> Unit,
    onOpenRecurringEntries: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    onOpenAppearanceSettings: () -> Unit
) {
    val features = listOf(
        FeatureItem(
            title = "单日账单查询",
            description = "查看指定日期的收入、支出与结余",
            icon = Icons.Default.CalendarMonth,
            onClick = onOpenDailyLedger
        ),
        FeatureItem(
            title = "周期记账",
            description = "管理每周或每月自动生成的账目",
            icon = Icons.Default.Repeat,
            onClick = onOpenRecurringEntries
        ),
        FeatureItem(
            title = "记账提醒",
            description = "设置每天的提醒时间与通知权限",
            icon = Icons.Default.Notifications,
            onClick = onOpenReminderSettings
        ),
        FeatureItem(
            title = "外观设置",
            description = "调整主题色、背景色和背景图片",
            icon = Icons.Default.Palette,
            onClick = onOpenAppearanceSettings
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(features.size) { index ->
            val feature = features[index]
            ListItem(
                headlineContent = { Text(feature.title) },
                supportingContent = { Text(feature.description) },
                leadingContent = {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = "进入${feature.title}")
                },
                modifier = Modifier.clickable(onClick = feature.onClick)
            )
            if (index < features.lastIndex) HorizontalDivider()
        }
    }
}

private data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
