package com.arieldavid.gymapplication.ui.home

import android.content.Context
import android.util.Log
import com.arieldavid.gymapplication.SupabaseConfig
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.ClientEntity
import com.arieldavid.gymapplication.repository.TrainerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class HomeRepository {

    suspend fun obtenerFechaPago(
        context: Context,
        token: String,
        userId: String
    ): String {

        var fechaPago = "-"

        TrainerRepository.insertarEntrenador(context)

        val db = DatabaseProvider.getDatabase(context)
        val dao = db.clientDAO()

        try {

            val client = OkHttpClient()

            Log.d("HOME", "ENTRO AL TRY")

            val request = Request.Builder()
                .url(
                    "${SupabaseConfig.URL}/rest/v1/Clientes?user_id=eq.$userId&select=Payment(estado,fecha_sig_pago)"
                )
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            Log.d("HOME", "HTTP CODE: ${response.code}")

            val body = response.body?.string()

            if (!body.isNullOrEmpty()) {

                val jsonArray = JSONArray(body)

                if (jsonArray.length() > 0) {

                    val cliente = jsonArray.getJSONObject(0)

                    val payments = cliente.getJSONArray("Payment")

                    if (payments.length() > 0) {

                        var ultimoPago: org.json.JSONObject? = null

                        for (i in 0 until payments.length()) {

                            val actual = payments.getJSONObject(i)

                            if (
                                ultimoPago == null ||
                                actual.getString("fecha_sig_pago") >
                                ultimoPago!!.getString("fecha_sig_pago")
                            ) {
                                ultimoPago = actual
                            }
                        }

                        ultimoPago?.let { pago ->

                            fechaPago = pago.optString(
                                "fecha_sig_pago",
                                "-"
                            )
                        }
                    }

                    withContext(Dispatchers.IO) {

                        val actual = dao.getProfile()

                        if (actual != null) {

                            dao.insert(
                                ClientEntity(
                                    uuid = actual.uuid,
                                    nombre = actual.nombre,
                                    email = actual.email,
                                    idMembresia = actual.idMembresia,
                                    numeroCliente = actual.numeroCliente,
                                    fechaSigPago = fechaPago
                                )
                            )
                        }
                    }
                }
            }

            Log.d("HOME", "BODY: $body")

            return fechaPago

        } catch (e: Exception) {

            val local = withContext(Dispatchers.IO) {
                dao.getProfile()
            }

            if (local != null) {
                fechaPago = local.fechaSigPago ?: "-"
            }

            Log.e("HOME", "ERROR: ${e.message}")

            return fechaPago
        }
    }

    suspend fun obtenerFechaPagoLocal(
        context: Context
    ): String {

        val dao = DatabaseProvider
            .getDatabase(context)
            .clientDAO()

        val cliente = withContext(Dispatchers.IO) {
            dao.getProfile()
        }

        return cliente?.fechaSigPago ?: "-"
    }

    /**
     * Trae los datos del cliente desde Supabase (por user_id).
     * Devuelve el primer JSONObject o null si no hay respuesta.
     */
    suspend fun obtenerClienteOnline(
        token: String,
        userId: String
    ): JSONObject? {

        return withContext(Dispatchers.IO) {

            try {

                val url =
                    "${SupabaseConfig.URL}/rest/v1/Clientes" +
                            "?user_id=eq.$userId" +
                            "&select=id_clientes,nombre,email," +
                            "id_membresia,id_entrenador,numero_cliente"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val client = OkHttpClient()

                client.newCall(request).execute().use { response ->

                    val body = response.body?.string()

                    if (response.isSuccessful && !body.isNullOrBlank()) {

                        JSONArray(body)
                            .takeIf { it.length() > 0 }
                            ?.getJSONObject(0)

                    } else null
                }

            } catch (e: Exception) {

                Log.e("HOME", "Error trayendo cliente de Supabase: ${e.message}")
                null
            }
        }
    }
}