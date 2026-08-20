package com.arieldavid.gymapplication.local.QrLocal

import retrofit2.http.Body
import retrofit2.http.POST

interface LocalApiService {

    @POST("api/qr")
    suspend fun procesarQR(
        @Body request: QrRequest
    ): QrResponse
}