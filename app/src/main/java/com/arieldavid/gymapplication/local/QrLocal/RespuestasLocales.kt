package com.arieldavid.gymapplication.local.QrLocal

data class EntrenadorResponse(
    val id_entrenadores: String,
    val nombre: String?,
    val descripcion: String?
)

data class MembresiaResponse(
    val id_membresia: Int,
    val nombre: String?,
    val precio_normal: Double?,
    val precio_transferencia: Double?,
    val beneficio: String?
)
data class PagoResponse(
    val fecha_sig_pago: String?,
    val estado: String?
)