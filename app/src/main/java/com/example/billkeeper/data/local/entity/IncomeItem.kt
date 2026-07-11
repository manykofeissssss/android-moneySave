package com.example.billkeeper.data.local.entity

import androidx.room.*

@Entity(
    tableName = "incomes",
    indices = [Index(value = ["date"]), Index(value = ["source"])]
)
data class IncomeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val source: String,
    val amountCents: Long,
    val date: Long,
    val note: String = ""
)
