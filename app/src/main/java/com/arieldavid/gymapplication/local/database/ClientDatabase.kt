package com.arieldavid.gymapplication.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arieldavid.gymapplication.local.DAO.ClientDAO
import com.arieldavid.gymapplication.local.DAO.MembresiaDAO
import com.arieldavid.gymapplication.local.entity.ClientEntity
import com.arieldavid.gymapplication.local.entity.MembresiaEntity


@Database(entities = [ClientEntity::class, MembresiaEntity::class], version = 6)
abstract class ClientDatabase :RoomDatabase() {
    abstract fun clientDAO(): ClientDAO
    abstract fun MembresiaDAO(): MembresiaDAO
}
