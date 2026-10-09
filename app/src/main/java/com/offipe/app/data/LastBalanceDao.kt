package com.offipe.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LastBalanceDao {
    @Query("SELECT * FROM last_balance WHERE id = ${LastBalanceEntity.SINGLE_ROW_ID}")
    fun observe(): Flow<LastBalanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(balance: LastBalanceEntity)

    @Query("DELETE FROM last_balance")
    suspend fun clear()
}