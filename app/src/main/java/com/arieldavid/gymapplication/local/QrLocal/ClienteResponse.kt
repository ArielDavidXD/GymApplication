package com.arieldavid.gymapplication.local.QrLocal

data class ClienteResponse(
    val idClientes: String,
    val numeroCliente: Int?,
    val nombre: String?,
    val email: String?,
    val id_entrenador: String?,
    val id_membresia: Int?,
    val nombreEntrenador: String?,
    val nombreMembresia: String?,
    val nombreTurno: String?,
    val password: String?,
    val plan: String?,
    val turno: String?,
    val userId: String?
)