package com.arieldavid.gymapplication.repository

import android.content.Context
import android.util.Log
import com.arieldavid.gymapplication.AuthClass
import com.arieldavid.gymapplication.SupabaseConfig
import com.arieldavid.gymapplication.local.Clase.Entrenadores
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.TrainerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.decodeFromString

object TrainerRepository {

    private val client = OkHttpClient()

    suspend fun insertarEntrenador(context: Context) {

        withContext(Dispatchers.IO) {

            try {
                Log.d("TRAINERS", "ENTRO SYNC")



                val token = AuthClass.getAccessToken(context)
                    ?: return@withContext

                val request = Request.Builder()
                    .url(
                        "${SupabaseConfig.URL}/rest/v1/Entrenadores?select=*"
                    )
                    .addHeader(
                        "apikey",
                        SupabaseConfig.API_KEY
                    )
                    .addHeader(
                        "Authorization",
                        "Bearer $token"
                    )
                    .build()

                val response =
                    client.newCall(request).execute()
                Log.d("TRAINERS", "HTTP: ${response.code}")

                if (!response.isSuccessful) {

                    return@withContext

                }

                val body =
                    response.body?.string()
                Log.d("TRAINERS", "BODY: $body")

                if (!body.isNullOrEmpty()) {

                    val json = Json {
                        ignoreUnknownKeys = true
                    }

                    val entrenadores =
                        json.decodeFromString<List<Entrenadores>>(body)
                    Log.d("TRAINERS", "CANTIDAD: ${entrenadores.size}")

                    val db =
                        DatabaseProvider.getDatabase(context)

                    val dao =
                        db.TrainersDAO()

                    dao.deleteAll()

                    entrenadores.forEach {
                        Log.d("TRAINERS", "INSERTANDO: ${it.nombre}")

                        dao.insert(
                            TrainerEntity(
                                id_entrenadores = it.id_entrenadores,
                                nombre = it.nombre,
                                telefono = it.telefono,
                                descripcion = it.descripcion
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(
                    "TRAINERS",
                    "ERROR: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }
}