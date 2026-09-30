package com.martin.misfinanzas.notification

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val componente = ComponentName(context, NequiNotificationListenerService::class.java)
            try {
                NotificationListenerService.requestRebind(componente)
            } catch (e: Exception) {
                // Si el permiso no está activo todavía, esto simplemente no hace nada
            }
        }
    }
}
