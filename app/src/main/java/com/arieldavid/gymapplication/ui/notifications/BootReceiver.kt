package com.arieldavid.gymapplication.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Re-programa las notificaciones cuando el teléfono se reinicia,
 * para que el recordatorio de pago siga funcionando aunque la app
 * no se haya abierto después del reinicio.
 */
class BootReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            WorkScheduler.schedule(context)
        }
    }
}