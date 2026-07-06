package com.example.billkeeper

import androidx.room.*

@Entity(tableName = "bills")
data class BillItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val amount: Double,
    val date: Long,
    val note: String = ""
)
