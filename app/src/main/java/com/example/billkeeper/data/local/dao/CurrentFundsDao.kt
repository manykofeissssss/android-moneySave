package com.example.billkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.billkeeper.data.local.entity.CurrentFunds
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentFundsDao {
    @Query("SELECT * FROM current_funds WHERE id = ${CurrentFunds.SINGLETON_ID}")
    fun observe(): Flow<CurrentFunds?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(funds: CurrentFunds)
}
