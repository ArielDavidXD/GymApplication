package com.arieldavid.gymapplication.local.Clase

import kotlinx.serialization.Serializable

@Serializable
data class SolicitudEntrada(
    val id_cliente: String,
    val estado: String = "PENDIENTE"
)