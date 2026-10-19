package com.arieldavid.gymapplication.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Clientes")
data class ClientEntity(

    @PrimaryKey
    @ColumnInfo(name = "id_clientes")
    val uuid: String,

    @ColumnInfo(name = "nombre")
    val nombre: String? = null,

    @ColumnInfo(name = "numero_cliente")
    val numeroCliente: Int? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "id_membresia")
    val idMembresia: Int? = null,

    @ColumnInfo(name = "id_entrenador")
    val idEntrenador: String? = null,

    @ColumnInfo(name = "id_turno")
    val idTurno: Int? = null,

    @ColumnInfo(name = "fechaSigPago")
    val fechaSigPago: String? = null
)