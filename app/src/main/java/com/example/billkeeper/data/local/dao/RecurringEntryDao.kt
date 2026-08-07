package com.example.billkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.billkeeper.data.local.entity.RecurringEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringEntryDao {
    @Query("SELECT * FROM recurring_entries ORDER BY enabled DESC, nextRunAt, id")
    fun observeAll(): Flow<List<RecurringEntry>>

    @Query("SELECT * FROM recurring_entries WHERE id = :id")
    suspend fun getById(id: Int): RecurringEntry?

    @Query(
        """
        SELECT * FROM recurring_entries
        WHERE enabled = 1 AND nextRunAt <= :nowMillis
        ORDER BY nextRunAt, id
        """
    )
    suspend fun getDueEntries(nowMillis: Long): List<RecurringEntry>

    @Upsert
    suspend fun upsert(entry: RecurringEntry)

    @Query("DELETE FROM recurring_entries WHERE id = :id")
    suspend fun deleteById(id: Int)
}
