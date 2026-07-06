package com.example.billkeeper

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Insert suspend fun insert(income: IncomeItem)

    @Update suspend fun update(income: IncomeItem)

    @Delete suspend fun delete(income: IncomeItem)

    @Query("SELECT * FROM incomes WHERE id = :id")
    suspend fun getById(id: Int): IncomeItem?

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAll(): Flow<List<IncomeItem>>

    @Query("SELECT * FROM incomes WHERE date BETWEEN :startMs AND :endMs ORDER BY date DESC")
    fun getByMonth(startMs: Long, endMs: Long): Flow<List<IncomeItem>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM incomes")
    fun getTotalIncome(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM incomes WHERE date BETWEEN :startMs AND :endMs")
    fun getTotalIncomeByMonth(startMs: Long, endMs: Long): Flow<Double>
}
