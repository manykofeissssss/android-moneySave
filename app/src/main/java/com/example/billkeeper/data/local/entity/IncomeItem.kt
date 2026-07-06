package com.example.billkeeper

import androidx.room.*

@Entity(tableName = "incomes")
data class IncomeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val source: String,
    val amount: Double,
    val date: Long,
    val note: String = ""
)
