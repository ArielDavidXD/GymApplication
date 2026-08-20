package com.arieldavid.gymapplication.workers

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import com.arieldavid.gymapplication.ui.notifications.NotificationHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class NotificacionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {

        try {

            val db = DatabaseProvider.getDatabase(applicationContext)

            // Solo el usuario que inició sesión
            val cliente = db.clientDAO().getProfile()

            if (cliente == null)
                return Result.success()

            if (cliente.fechaSigPago.isNullOrBlank())
                return Result.success()

            val fechaPago = LocalDate.parse(cliente.fechaSigPago)

            val hoy = LocalDate.now()

            val dias =
                ChronoUnit.DAYS.between(hoy, fechaPago)

            when {

                dias <= 0 -> {

                    NotificationHelper.mostrarNotificacion(
                        applicationContext,
                        "Membresía vencida",
                        "Hola ${cliente.nombre}, tu membresía está vencida. Regulariza tu pago."
                    )

                }

                dias == 1L -> {

                    NotificationHelper.mostrarNotificacion(
                        applicationContext,
                        "Membresía próxima a vencer",
                        "Hola ${cliente.nombre}, tu membresía vence mañana."
                    )

                }

                dias == 5L -> {

                    NotificationHelper.mostrarNotificacion(
                        applicationContext,
                        "Membresía próxima a vencer",
                        "Hola ${cliente.nombre}, tu membresía vence en 5 días."
                    )

                }
            }

            return Result.success()

        } catch (e: Exception) {

            e.printStackTrace()

            return Result.failure()
        }
    }
}