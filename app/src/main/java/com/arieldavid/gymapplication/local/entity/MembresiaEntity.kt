package com.arieldavid.gymapplication.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Membresia")
data class MembresiaEntity(

    @PrimaryKey
    val id_membresia: Int,

    val nombre: String?,
    val precio: Double?,
    val beneficio: String?
)