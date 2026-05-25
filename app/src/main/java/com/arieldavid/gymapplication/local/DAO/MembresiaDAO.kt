package com.arieldavid.gymapplication.local.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arieldavid.gymapplication.local.entity.ClientEntity
import com.arieldavid.gymapplication.local.entity.MembresiaEntity

@Dao
interface MembresiaDAO {

    @Query("SELECT * FROM Membresia")
    suspend fun getAll(): List<MembresiaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(
        membresias: List<MembresiaEntity>
    )
}