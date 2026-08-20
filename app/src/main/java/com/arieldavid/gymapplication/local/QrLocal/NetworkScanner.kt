package com.arieldavid.gymapplication.local.QrLocal

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetworkScanner {

    private val client = OkHttpClient.Builder()
        .connectTimeout(200, TimeUnit.MILLISECONDS)
        .readTimeout(200, TimeUnit.MILLISECONDS)
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

    suspend fun buscarServidor(
        context: Context
    ): String? = coroutineScope {

        val red = obtenerRed()

        Log.d("SCAN", "Red encontrada: $red")

        val tareas = (1..254).map { host ->

            async(Dispatchers.IO) {

                val ip = "$red.$host"

                Log.d("SCAN", "Probando $ip")

                try {

                    val request = Request.Builder()
                        .url("http://$ip:8080/api/ping")
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful &&
                        response.body!!.string() == "GYM_SERVER"
                    ) {

                        Log.d("SCAN", "ENCONTRADO $ip")

                        return@async ip
                    }

                } catch (e: Exception) {
                    Log.d("SCAN", "$ip -> ${e.message}")
                }

                null
            }
        }

        val resultado = tareas.awaitAll().firstOrNull()

        Log.d("SCAN", "Resultado=$resultado")

        resultado
    }
}