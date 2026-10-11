package com.example.billkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_funds")
data class CurrentFunds(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val amountCents: Long,
    val incomeSnapshotCents: Long,
    val expenseSnapshotCents: Long
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
