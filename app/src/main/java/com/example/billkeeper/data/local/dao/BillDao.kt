package com.example.billkeeper

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert suspend fun insert(bill: BillItem)

    @Update suspend fun update(bill: BillItem)

    @Delete suspend fun delete(bill: BillItem)

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Int): BillItem?

    @Query("SELECT * FROM bills ORDER BY date DESC")
    fun getAll(): Flow<List<BillItem>>

    @Query("SELECT * FROM bills WHERE date BETWEEN :startMs AND :endMs ORDER BY date DESC")
    fun getByMonth(startMs: Long, endMs: Long): Flow<List<BillItem>>

    @Query("SELECT category, SUM(amount) as total FROM bills GROUP BY category ORDER BY total DESC")
    fun getCategorySummary(): Flow<List<CategorySummary>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM bills")
    fun getTotalExpense(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM bills WHERE date BETWEEN :startMs AND :endMs")
    fun getTotalExpenseByMonth(startMs: Long, endMs: Long): Flow<Double>

    @Query("SELECT category, SUM(amount) as total FROM bills WHERE date BETWEEN :startMs AND :endMs GROUP BY category ORDER BY total DESC")
    fun getCategorySummaryByMonth(startMs: Long, endMs: Long): Flow<List<CategorySummary>>
}
