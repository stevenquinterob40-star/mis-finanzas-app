package com.martin.misfinanzas.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.martin.misfinanzas.data.AppDatabase
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.data.UsoCategorias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NequiNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val paquetesPermitidos = listOf(
        "com.nequi.MobileApp",
        "com.bancolombia.personassf",
        "com.bancolombia.personas",
        "co.nu.app",
        "co.com.nu"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val paquete = sbn.packageName ?: return
        if (paquetesPermitidos.none { paquete.contains(it, ignoreCase = true) }) return

        val extras = sbn.notification.extras
        val titulo = extras.getCharSequence("android.title")?.toString().orEmpty()
        val texto = (
            extras.getCharSequence("android.bigText")
                ?: extras.getCharSequence("android.text")
                ?: extras.getCharSequence("android.subText")
            )?.toString().orEmpty()
        val ticker = sbn.notification.tickerText?.toString().orEmpty()

        if (titulo.isBlank() && texto.isBlank() && ticker.isBlank()) return

        val textoCombinado = if (texto.isNotBlank()) texto else ticker

        if (NequiParser.esPublicidad(titulo, textoCombinado)) return

        val resultado = NequiParser.parse(paquete, titulo, textoCombinado)
        val dao = AppDatabase.getInstance(applicationContext).transactionDao()

        if (NequiParser.esConfirmacionGenerica(titulo, textoCombinado) && resultado.monto == null) {
            return
        }

        if (NequiParser.esRetiro(titulo, textoCombinado) && resultado.monto != null && resultado.monto > 0) {
            val fecha = System.currentTimeMillis()
            val gastoDigital = Transaction(
                monto = resultado.monto,
                tipo = TipoMovimiento.GASTO,
                origen = Origen.DIGITAL,
                descripcion = if (titulo.isNotBlank()) titulo else "Retiro ${resultado.entidad}",
                fecha = fecha,
                esAutomatica = true,
                necesitaRevision = false,
                notificacionCruda = "$titulo | $textoCombinado",
                categoria = "Retiro",
                entidad = resultado.entidad
            )
            val ingresoEfectivo = Transaction(
                monto = resultado.monto,
                tipo = TipoMovimiento.INGRESO,
                origen = Origen.EFECTIVO,
                descripcion = "Efectivo retirado en cajero",
                fecha = fecha,
                esAutomatica = true,
                necesitaRevision = false,
                notificacionCruda = "$titulo | $textoCombinado",
                categoria = "Retiro",
                entidad = resultado.entidad
            )
            scope.launch {
                dao.insert(gastoDigital)
                dao.insert(ingresoEfectivo)
            }
            UsoCategorias.registrarUso(applicationContext, "Retiro")
            return
        }

        val transaction = Transaction(
            monto = resultado.monto ?: 0.0,
            tipo = resultado.tipo ?: TipoMovimiento.GASTO,
            origen = Origen.DIGITAL,
            descripcion = if (titulo.isNotBlank()) titulo else "Movimiento ${resultado.entidad}",
            fecha = System.currentTimeMillis(),
            esAutomatica = true,
            necesitaRevision = !resultado.confiable,
            notificacionCruda = "$titulo | $textoCombinado",
            entidad = resultado.entidad
        )

        scope.launch {
            dao.insert(transaction)
        }
    }
}
