package com.arieldavid.gymapplication.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity (tableName = "Clientes")
data class ClientEntity(
    @PrimaryKey val uuid: String,
    @ColumnInfo(name= "nombre") val nombre:String? = null,

    @ColumnInfo(name = "telefono") val telefono:String? = null,

    @ColumnInfo(name = "id_membresia")
    val idMembresia: Int? = null,

    @ColumnInfo(name = "numero_cliente")
    val numeroCliente: Int? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    val fechaSigPago: String? = null
)

{

}