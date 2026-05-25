package com.arieldavid.gymapplication.local.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arieldavid.gymapplication.local.entity.ClientEntity

@Dao
interface ClientDAO {

    @Query("SELECT * FROM Clientes")
    suspend fun getAll(): List<ClientEntity>

    @Query("SELECT * FROM Clientes LIMIT 1")
    suspend fun getProfile(): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cliente: ClientEntity)
    @Query("DELETE FROM Clientes")
    suspend fun deleteAll()
}