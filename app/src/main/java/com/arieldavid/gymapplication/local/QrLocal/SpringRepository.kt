package com.arieldavid.gymapplication.local.QrLocal

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.time.LocalDateTime

object SpringRepository {

    private val client = OkHttpClient()

    var BASE_URL = ""

    fun actualizarIp(ip: String) {
        BASE_URL = "http://$ip:8080"
    }

    suspend fun yaEntroHoy(idCliente: String): Boolean {

        Log.d("SPRING", "BASE_URL=$BASE_URL")
        val request = Request.Builder()
            .url("$BASE_URL/api/entradas/ya-entro/$idCliente")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Error ${response.code}")

        return response.body!!.string().toBoolean()
    }

    suspend fun verificarTurno(idCliente: String): Boolean {

        val request = Request.Builder()
            .url("$BASE_URL/api/clientes/$idCliente/turno")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Error ${response.code}")

        return response.body!!.string().toBoolean()
    }

    suspend fun registrarEntrada(idCliente: String): Boolean {

        val body = """
    {
        "idCliente":"$idCliente"
    }
    """.trimIndent()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/api/entradas")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        return response.isSuccessful
    }

    suspend fun crearSolicitud(idCliente: String): Boolean {

        val body = """
        {
            "idCliente":"$idCliente",
            "fecha":"${LocalDateTime.now()}"
        }
        """.trimIndent()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/api/solicitudes")
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Error ${response.code}")

        return true
    }

    suspend fun existeSolicitudPendiente(idCliente: String): Boolean {

        val request = Request.Builder()
            .url("$BASE_URL/api/solicitudes/pendiente/$idCliente")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Error ${response.code}")

        return response.body!!.string().toBoolean()
    }

    suspend fun obtenerEstadoSolicitud(idCliente: String): String? {

        val request = Request.Builder()
            .url("$BASE_URL/api/solicitudes/estado/$idCliente")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("Error ${response.code}")

        return response.body!!.string()
    }
}