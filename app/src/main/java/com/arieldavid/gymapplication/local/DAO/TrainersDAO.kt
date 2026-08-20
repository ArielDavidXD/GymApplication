package com.arieldavid.gymapplication.local.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arieldavid.gymapplication.local.entity.ClientEntity
import com.arieldavid.gymapplication.local.entity.TrainerEntity

@Dao
interface TrainersDAO {

    @Query("SELECT * FROM Entrenadores")
    suspend fun getAll(): List<TrainerEntity>

    @Query("SELECT * FROM Entrenadores LIMIT 1")
    suspend fun getProfile(): TrainerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(Trainer: TrainerEntity)
    @Query("DELETE FROM Entrenadores")
    suspend fun deleteAll()
}
