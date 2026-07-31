package com.example.billkeeper.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "monthly_budgets",
    primaryKeys = ["year", "month", "category"]
)
data class MonthlyBudget(
    val year: Int,
    val month: Int, // 明确规定为 1 到 12
    val category: String,
    val limitCents: Long
)