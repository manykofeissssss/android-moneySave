package com.example.billkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.billkeeper.data.local.entity.IncomeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(income: IncomeItem)

    @Update
    suspend fun update(income: IncomeItem)

    @Delete
    suspend fun delete(income: IncomeItem)

    @Query("SELECT * FROM incomes WHERE id = :id")
    suspend fun getById(id: Int): IncomeItem?

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAll(): Flow<List<IncomeItem>>

    @Query("SELECT * FROM incomes WHERE date >= :startMs AND date < :endMs ORDER BY date DESC")
    fun getByMonth(startMs: Long, endMs: Long): Flow<List<IncomeItem>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM incomes")
    fun getTotalIncome(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM incomes WHERE date >= :startMs AND date < :endMs")
    fun getTotalIncomeByMonth(startMs: Long, endMs: Long): Flow<Long>
}
