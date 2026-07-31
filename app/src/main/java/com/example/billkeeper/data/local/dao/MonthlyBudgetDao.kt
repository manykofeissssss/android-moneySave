package com.example.billkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.billkeeper.data.local.entity.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {

    @Upsert
    suspend fun upsert(budget: MonthlyBudget)

    @Query(
        """
        SELECT * FROM monthly_budgets
        WHERE year = :year AND month = :month
        ORDER BY category
        """
    )
    fun observeByMonth(
        year: Int,
        month: Int
    ): Flow<List<MonthlyBudget>>

    @Query(
        """
        DELETE FROM monthly_budgets
        WHERE year = :year
          AND month = :month
          AND category = :category
        """
    )
    suspend fun delete(
        year: Int,
        month: Int,
        category: String
    )
}