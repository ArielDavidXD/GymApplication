package com.arieldavid.gymapplication.local.QrLocal

import kotlinx.serialization.Serializable

@Serializable
data class QrRequest(
    val qr: String,
    val idCliente: String
)