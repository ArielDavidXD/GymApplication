package com.arieldavid.gymapplication.ui

import android.os.Build
import com.arieldavid.gymapplication.SupabaseConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log
import androidx.annotation.RequiresApi
import com.arieldavid.gymapplication.local.Clase.EntradasHoy
import com.arieldavid.gymapplication.local.QrLocal.SpringRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.LocalDate


object EntradasRepository {

    private val client = OkHttpClient()

    @Serializable
    data class ClienteTurno(
        val id_turno: Int
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun registrarEntradaSupabase(
        token: String,
        userId: String
    ): Boolean {

        return try {

            val ahora = LocalDateTime.now()

            val hora = ahora.hour

            val idTurno = when (hora) {

                7 -> 1
                8 -> 2
                9 -> 3
                10 -> 4
                11 -> 5
                14 -> 6
                15 -> 7
                16 -> 8
                17 -> 9
                18 -> 10
                19 -> 11

                else -> null
            }

            if (idTurno == null) {

                Log.e("TURNO", "NO HAY TURNO PARA ESTA HORA")

                return false
            }

            val fechaHora = ahora.toString()

            val json = """
        {
            "id_clientes":"$userId",
            "id_turno":$idTurno,
            "fecha_hora":"$fechaHora"
        }
        """

            Log.d("TURNO", "JSON: $json")

            val body = json.toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/Entradas")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            Log.d("TURNO", "HTTP: ${response.code}")

            val responseBody = response.body?.string()

            Log.d("TURNO", "BODY: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }
            response.isSuccessful

        } catch (e: Exception) {
            Log.e("TURNO", "ERROR", e)
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun registrarEntrada(
        token: String,
        userId: String
    ): Boolean {

        return try {

            registrarEntradaSupabase(token, userId)

        } catch (e: Exception) {

            Log.d("FALLBACK", "Registrando entrada por Spring")

            SpringRepository.registrarEntrada(userId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun obtenerTurnoActual(): Int? {

        return when (LocalDateTime.now().hour) {

            7 -> 1
            8 -> 2
            9 -> 3
            10 -> 4
            11 -> 5
            14 -> 6
            15 -> 7
            16 -> 8
            17 -> 9
            18 -> 10
            19 -> 11

            else -> null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun verificarTurnoCliente(
        token: String,
        idCliente: String
    ): Boolean {

        return try {

            verificarTurnoClienteSupabase(token, idCliente)

        } catch (e: Exception) {

            if (e.message == "GIMNASIO_CERRADO")
                throw e

            Log.d("FALLBACK", "Supabase no disponible. Verificando por Spring")

            SpringRepository.verificarTurno(idCliente)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun verificarTurnoClienteSupabase(
        token: String,
        idCliente: String
    ): Boolean {

        val request = Request.Builder()
            .url("${SupabaseConfig.URL}/rest/v1/Clientes?id_clientes=eq.$idCliente&select=id_turno")
            .addHeader("apikey", SupabaseConfig.API_KEY)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful)
            throw Exception("HTTP ${response.code}")

        val body = response.body?.string() ?: return false

        val cliente = Json {
            ignoreUnknownKeys = true
        }.decodeFromString<List<ClienteTurno>>(body)
            .firstOrNull()
            ?: return false

        val turnoCliente = cliente.id_turno
        val turnoActual = obtenerTurnoActual()

        Log.d("TURNO", "Cliente=$turnoCliente Actual=$turnoActual")

        if (turnoActual == null)
            throw Exception("GIMNASIO_CERRADO")

        return turnoCliente == turnoActual
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun yaEntroHoySupabase(
        token:String,
        idCliente:String
    ):Boolean {


        val hoy = LocalDate.now().toString()


        val request = Request.Builder()
            .url(
                "${SupabaseConfig.URL}/rest/v1/Entradas?id_clientes=eq.$idCliente&select=fecha_hora&order=fecha_hora.desc&limit=1"
            )
            .addHeader("apikey",SupabaseConfig.API_KEY)
            .addHeader("Authorization","Bearer $token")
            .build()


        val response = client.newCall(request).execute()


        if(!response.isSuccessful){
            throw Exception("Supabase HTTP ${response.code}")
        }


        val body=response.body?.string()
            ?: throw Exception("Body vacío")


        val entradas =
            Json{
                ignoreUnknownKeys=true
            }.decodeFromString<List<EntradasHoy>>(body)


        return entradas.any{
            it.fecha_hora.startsWith(hoy)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun yaEntroHoy(
        token: String,
        idCliente: String
    ): Boolean {

        return try {

            yaEntroHoySupabase(token, idCliente)

        } catch (e: Exception) {

            Log.d("FALLBACK", "Supabase no disponible: ${e.message}")

            SpringRepository.yaEntroHoy(idCliente)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun crearSolicitudEntrada(
        token: String,
        idCliente: String
    ): Boolean {

        return try {

            crearSolicitudEntradaSupabase(token, idCliente)

        } catch (e: Exception) {

            Log.d("FALLBACK", "Creando solicitud por Spring")

            SpringRepository.crearSolicitud(idCliente)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun crearSolicitudEntradaSupabase(
        token: String,
        idCliente: String
    ): Boolean {

        return try {

            val json = """
        {
            "id_cliente":"$idCliente",
            "estado":"PENDIENTE",
            "fecha":"${java.time.LocalDateTime.now()}"
        }
        """

            val body = json.toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/SolicitudEntrada")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build()

            val response = client.newCall(request).execute()


            Log.d(
                "SOLICITUD_ENTRADA",
                "HTTP ${response.code}"
            )

            Log.d(
                "SOLICITUD_ENTRADA",
                response.body?.string() ?: "SIN BODY"
            )

            response.isSuccessful

        } catch (e: Exception) {

                Log.e("SOLICITUD",e.message ?: "")
                throw e
            }
    }

    suspend fun existeSolicitudPendiente(
        token: String,
        idCliente: String
    ): Boolean {

        return try {

            existeSolicitudPendienteSupabase(token, idCliente)

        } catch (e: Exception) {

            Log.d("FALLBACK", "Consultando solicitud por Spring")

            SpringRepository.existeSolicitudPendiente(idCliente)
        }
    }

    private fun existeSolicitudPendienteSupabase(
        token: String,
        idCliente: String
    ): Boolean {

        return try {

            val request = Request.Builder()
                .url(
                    "${SupabaseConfig.URL}/rest/v1/SolicitudEntrada?id_cliente=eq.$idCliente&estado=eq.PENDIENTE"
                )
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response =
                client.newCall(request).execute()

            val body =
                response.body?.string() ?: "[]"

            Log.d(
                "SOLICITUD",
                "BODY: $body"
            )

            body != "[]"

        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun obtenerEstadoSolicitud(
        token: String,
        idCliente: String
    ): String? {

        return try {

            obtenerEstadoSolicitudSupabase(token, idCliente)

        } catch (e: Exception) {

            Log.d("FALLBACK", "Estado por Spring")

            SpringRepository.obtenerEstadoSolicitud(idCliente)
        }
    }

    private fun obtenerEstadoSolicitudSupabase(
        token: String,
        idCliente: String
    ): String? {

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(
                "${SupabaseConfig.URL}/rest/v1/SolicitudEntrada" +
                        "?id_cliente=eq.$idCliente" +
                        "&select=estado" +
                        "&order=fecha.desc" +
                        "&limit=1"
            )
            .addHeader("apikey", SupabaseConfig.API_KEY)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful)
                throw Exception("HTTP ${response.code}")

            val body = response.body?.string() ?: return null

            val json = JSONArray(body)

            if (json.length() == 0)
                return null

            return json
                .getJSONObject(0)
                .getString("estado")
        }
    }
}