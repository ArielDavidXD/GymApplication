package com.arieldavid.gymapplication.ui.notifications

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Worker
import android.content.Context
import android.util.Log
import com.arieldavid.gymapplication.local.database.DatabaseProvider
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PaymentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresApi(Build.VERSION_CODES.O)

    override suspend fun doWork(): Result {
        Log.d("WORKER", "Worker ejecutándose")
        val db =
            DatabaseProvider.getDatabase(applicationContext)

        val cliente =
            db.clientDAO().getProfile()

        if (cliente == null)
            return Result.success()

        val fechaPago =
            cliente.fechaSigPago ?: return Result.success()

        val fechaVencimiento =
            LocalDate.parse(fechaPago)

        val hoy =
            LocalDate.now()

        val dias =
            ChronoUnit.DAYS.between(
                hoy,
                fechaVencimiento
            )



        when (dias) {

            5L -> {

                NotificationHelper.mostrarNotificacion(
                    applicationContext,
                    "Pago próximo",
                    "Tu membresía vence en 5 días."
                )
            }

            0L -> {

                NotificationHelper.mostrarNotificacion(
                    applicationContext,
                    "Pago pendiente",
                    "Tu membresía vence hoy."
                )
            }

            -1L -> {

                NotificationHelper.mostrarNotificacion(
                    applicationContext,
                    "Membresía vencida",
                    "Tu membresía ha vencido. Realiza el pago para seguir ingresando."
                )
            }
        }

        return Result.success()
    }
}