package com.arieldavid.gymapplication.ui.plans

import android.content.Context
import android.util.Log
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.local.entity.MembresiaEntity
import com.arieldavid.gymapplication.ui.MembresiaRepository
import com.arieldavid.gymapplication.ui.home.Membresia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PlansRepository {
    suspend fun actualizarOnline(
        context: Context,
        token: String
    ): List<Membresia> = withContext(Dispatchers.IO) {

        val db = DatabaseProvider.getDatabase(context)
        val dao = db.MembresiaDAO()

        try {

            val json = MembresiaRepository.cargarMembresias(token)

            if (json.isNullOrEmpty()) {
                return@withContext obtenerMembresiaLocal(context)
            }

            val lista = Json {
                ignoreUnknownKeys = true
            }.decodeFromString<List<Membresia>>(json)

            dao.insertAll(
                lista.map {
                    MembresiaEntity(
                        id_membresia = it.id_membresia ?: 0,
                        nombre = it.nombre,
                        precio_normal = it.precio_normal,
                        precio_transferencia = it.precio_transferencia,
                        beneficio = it.beneficio
                    )
                }
            )

            lista

        } catch (e: Exception) {
            Log.e("PLANES", "ERROR EN SUPABASE", e)

            dao.getAll().map {
                Membresia(
                    id_membresia = it.id_membresia,
                    nombre = it.nombre,
                    precio_normal = it.precio_normal,
                    precio_transferencia = it.precio_transferencia,
                    beneficio = it.beneficio
                )
            }
        }
    }

    suspend fun obtenerMembresiaLocal(
        context: Context
    ): List<Membresia> {

        val dao = DatabaseProvider
            .getDatabase(context)
            .MembresiaDAO()

        return withContext(Dispatchers.IO) {

            dao.getAll().map {

                Membresia(
                    id_membresia = it.id_membresia,
                    nombre = it.nombre,
                    precio_normal = it.precio_normal,
                    precio_transferencia = it.precio_transferencia,
                    beneficio = it.beneficio
                )
            }
        }
    }

}