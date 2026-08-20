package com.arieldavid.gymapplication.local.Clase


import kotlinx.serialization.Serializable

@Serializable
data class Entradas(
    val id_entradas: String? = null,
    val id_clientes: String,
)

@Serializable
data class EntradasHoy(
    val fecha_hora: String
)