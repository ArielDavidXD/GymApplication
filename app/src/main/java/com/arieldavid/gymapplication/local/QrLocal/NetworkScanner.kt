package com.arieldavid.gymapplication.local.QrLocal

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetworkScanner {

    /**
     * Busca el servidor del gimnasio (IP guardada o escaneo de la subred)
     * y lo deja configurado en LocalApi y SpringRepository.
     * Devuelve true si hay servidor disponible.
     */
    suspend fun asegurarServidor(context: Context): Boolean {

        val ip = buscarServidor(context)
            ?: return false

        SpringRepository.actualizarIp(ip)
        LocalApi.actualizarIp(ip)
        Log.d("SCAN", "Servidor configurado: $ip")

        return true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(1000, TimeUnit.MILLISECONDS)
        .readTimeout(1000, TimeUnit.MILLISECONDS)
        .build()

    private fun obtenerRed(): String {

        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()

        while (interfaces.hasMoreElements()) {

            val network = interfaces.nextElement()

            if (!network.isUp || network.isLoopback) continue

            val direcciones = network.inetAddresses

            while (direcciones.hasMoreElements()) {

                val direccion = direcciones.nextElement()

                if (!direccion.isLoopbackAddress &&
                    direccion is java.net.Inet4Address
                ) {

                    val ip = direccion.hostAddress ?: continue

                    if (ip.startsWith("192.") ||
                        ip.startsWith("10.") ||
                        ip.startsWith("172.")
                    ) {

                        return ip.substringBeforeLast(".")
                    }
                }
            }
        }

        throw Exception("No se encontró una red local")
    }

    // Prueba UNA ip: true solo si /api/ping responde GYM_SERVER
    private fun probar(ip: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("http://$ip:8080/api/ping")
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful &&
                        response.body?.string()?.trim() == "GYM_SERVER"
            }
        } catch (e: Exception) {
            false
        }
    }

    // Prueba las 254 ips en paralelo y devuelve la primera que responde
    private suspend fun escanear(red: String): String? = coroutineScope {

        val encontrado = CompletableDeferred<String?>()

        val tareas = (1..254).map { host ->
            launch(Dispatchers.IO) {
                val ip = "$red.$host"
                if (probar(ip)) {
                    encontrado.complete(ip)
                }
            }
        }

        // Si terminan todas sin éxito, se completa con null
        launch {
            tareas.joinAll()
            encontrado.complete(null)
        }

        val resultado = encontrado.await()

        // Ya tenemos respuesta: no hace falta seguir probando
        coroutineContext.cancelChildren()

        resultado
    }

    suspend fun buscarServidor(context: Context): String? {

        val prefs = context.getSharedPreferences("server", Context.MODE_PRIVATE)

        // 1. IP guardada
        val guardada = prefs.getString("server_ip", null)

        if (guardada != null) {
            val sigue = withContext(Dispatchers.IO) { probar(guardada) }
            if (sigue) {
                Log.d("SCAN", "IP guardada OK: $guardada")
                return guardada
            }
            Log.d("SCAN", "IP guardada $guardada ya no responde")
        }

        // 2. Escaneo de la subred
        val red = try {
            obtenerRed()
        } catch (e: Exception) {
            Log.d("SCAN", "Sin red local: ${e.message}")
            return null
        }

        Log.d("SCAN", "Escaneando $red.x")

        val ip = escanear(red)

        Log.d("SCAN", "Resultado=$ip")

        if (ip != null) {
            prefs.edit().putString("server_ip", ip).apply()
        }

        return ip
    }
}