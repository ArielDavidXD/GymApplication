package com.arieldavid.gymapplication.local.QrLocal

import kotlinx.serialization.Serializable

@Serializable
data class QrResponse(
    val ok: Boolean,
    val mensaje: String,
    val nombre: String
)