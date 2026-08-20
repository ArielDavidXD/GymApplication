package com.arieldavid.gymapplication.ui.notifications

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.arieldavid.gymapplication.workers.NotificacionWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object WorkScheduler {

    @RequiresApi(Build.VERSION_CODES.O)
    fun schedule(context: Context) {

        val ahora = LocalDateTime.now()

        var proximaEjecucion = ahora.withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        // Si ya pasaron las 9 AM, programar para mañana
        if (ahora.isAfter(proximaEjecucion)) {
            proximaEjecucion = proximaEjecucion.plusDays(1)
        }

        val delay = Duration.between(ahora, proximaEjecucion)

        val request =
            PeriodicWorkRequestBuilder<NotificacionWorker>(
                1,
                TimeUnit.DAYS
            )
                .setInitialDelay(
                    delay.toMinutes(),
                    TimeUnit.MINUTES
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "altapinta_notifications",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }
}