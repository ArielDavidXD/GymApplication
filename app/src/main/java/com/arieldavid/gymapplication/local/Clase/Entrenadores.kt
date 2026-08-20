package com.arieldavid.gymapplication.local.Clase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Entrenadores(
    @SerialName("id_entrenadores")
    val id_entrenadores: String,
    val nombre: String? =null,
    val telefono: String? = null,
    val descripcion: String? = null

) {

}