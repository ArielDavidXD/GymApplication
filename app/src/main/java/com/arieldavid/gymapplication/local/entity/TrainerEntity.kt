package com.arieldavid.gymapplication.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

    @Entity(tableName = "Entrenadores")
    data class TrainerEntity(
        @PrimaryKey val id_entrenadores: String,
        @ColumnInfo(name= "nombre") val nombre:String? = null,

        @ColumnInfo(name = "telefono") val telefono:String? = null,
        @ColumnInfo(name = "descripcion") val descripcion:String? = null,
    )
