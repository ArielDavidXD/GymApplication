package com.arieldavid.gymapplication.local.QrLocal

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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

        if (!response.isSuccessful)
            return false

        // El API responde true/false en el body: hay que leerlo
        // para no decirle al usuario que la entrada sirvió
        // cuando en realidad el servidor la rechazó.
        val contenido = response.body?.string()?.trim()

        Log.d("ENTRADA", "HTTP ${response.code} body=$contenido")

        return contenido == "true"
    }

    /** GET /api/clientes/{id} contra el escritorio (datos frescos). */
    suspend fun obtenerCliente(idCliente: String): JSONObject? {

        val request = Request.Builder()
            .url("$BASE_URL/api/clientes/$idCliente")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            return null

        val contenido = response.body?.string()
            ?: return null

        return JSONObject(contenido)
    }

    /** GET /api/clientes/{id}/pago contra el escritorio. */
    suspend fun ultimoPago(idCliente: String): JSONObject? {

        val request = Request.Builder()
            .url("$BASE_URL/api/clientes/$idCliente/pago")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            return null

        val contenido = response.body?.string()
            ?: return null

        return JSONObject(contenido)
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