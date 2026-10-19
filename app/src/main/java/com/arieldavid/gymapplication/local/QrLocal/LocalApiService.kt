package com.arieldavid.gymapplication.local.QrLocal

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LocalApiService {

    @POST("api/qr")
    suspend fun procesarQR(
        @Body request: QrRequest
    ): QrResponse

    @GET("api/clientes/{idCliente}")
    suspend fun obtenerCliente(
        @Path("idCliente") idCliente: String
    ): ClienteResponse

    @GET("api/entrenadores")
    suspend fun obtenerEntrenadores(): List<EntrenadorResponse>

    @GET("api/membresias")
    suspend fun obtenerMembresias(): List<MembresiaResponse>

    @GET("api/clientes/{idCliente}/pago")
    suspend fun obtenerPago(
        @Path("idCliente") idCliente: String
    ): PagoResponse
}