package com.arieldavid.gymapplication.local.QrLocal

import android.util.Log
import com.arieldavid.gymapplication.local.QrLocal.SpringRepository.BASE_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object LocalApi {

    private var service: LocalApiService? = null

    fun actualizarIp(ip: String) {

        service = Retrofit.Builder()
            .baseUrl("http://$ip:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LocalApiService::class.java)
        Log.d("SPRING", "BASE_URL = $BASE_URL")
    }

    fun getService(): LocalApiService {

        return service
            ?: error("Servidor no inicializado")
    }

}object LocalQrRepository {

    suspend fun procesarQR(
        qr: String,
        idCliente: String
    ): QrResponse {

        return LocalApi
            .getService()
            .procesarQR(QrRequest(qr, idCliente))
    }
}