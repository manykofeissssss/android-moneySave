package com.example.billkeeper.data.local.entity

import androidx.room.*

@Entity(
    tableName = "bills",
    indices = [Index(value = ["date"]), Index(value = ["category"])]
)
data class BillItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val amountCents: Long,
    val date: Long,
    val note: String = ""
)
