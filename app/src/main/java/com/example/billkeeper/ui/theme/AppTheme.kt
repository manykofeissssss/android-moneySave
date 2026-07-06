package com.example.billkeeper

import androidx.compose.ui.graphics.Color

val EXPENSE_CATEGORIES = listOf("餐饮", "交通", "购物", "娱乐", "住房", "日用", "医疗", "教育", "其他")
val INCOME_SOURCES = listOf("工资", "兼职", "投资", "理财", "红包", "报销", "其他")

val CATEGORY_COLORS = mapOf(
    "餐饮" to Color(0xFFE57373),
    "交通" to Color(0xFF64B5F6),
    "购物" to Color(0xFFFFB74D),
    "娱乐" to Color(0xFFBA68C8),
    "住房" to Color(0xFF4DB6AC),
    "日用" to Color(0xFFAED581),
    "医疗" to Color(0xFFBCAAA4),
    "教育" to Color(0xFFFFD54F),
    "其他" to Color(0xFF90A4AE)
)
