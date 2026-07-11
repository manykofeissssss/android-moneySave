package com.example.billkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.model.CategorySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert
    suspend fun insert(bill: BillItem)

    @Update
    suspend fun update(bill: BillItem)

    @Delete
    suspend fun delete(bill: BillItem)

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Int): BillItem?

    @Query("SELECT * FROM bills ORDER BY date DESC")
    fun getAll(): Flow<List<BillItem>>

    @Query("SELECT * FROM bills WHERE date >= :startMs AND date < :endMs ORDER BY date DESC")
    fun getByMonth(startMs: Long, endMs: Long): Flow<List<BillItem>>

    @Query("SELECT category, SUM(amountCents) AS totalCents FROM bills GROUP BY category ORDER BY totalCents DESC")
    fun getCategorySummary(): Flow<List<CategorySummary>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM bills")
    fun getTotalExpense(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM bills WHERE date >= :startMs AND date < :endMs")
    fun getTotalExpenseByMonth(startMs: Long, endMs: Long): Flow<Long>

    @Query("SELECT category, SUM(amountCents) AS totalCents FROM bills WHERE date >= :startMs AND date < :endMs GROUP BY category ORDER BY totalCents DESC")
    fun getCategorySummaryByMonth(startMs: Long, endMs: Long): Flow<List<CategorySummary>>
}
