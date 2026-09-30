package com.martin.misfinanzas.ui

import com.martin.misfinanzas.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFechaDiagnostico = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("es", "CO"))

/**
 * Arma un texto plano con todas las notificaciones de Nequi que la app ha
 * capturado, para que Martin lo copie y lo pegue en el chat con Claude y así
 * se puedan revisar patrones reales en bloque (en vez de una captura de
 * pantalla a la vez).
 */
fun List<Transaction>.aTextoDiagnostico(): String {
    if (isEmpty()) return "No hay notificaciones capturadas todavía."

    val encabezado = "Diagnóstico de notificaciones Nequi capturadas (${size} en total)\n" +
        "Formato: [fecha] tipo | monto | necesitaRevision | categoría -> texto crudo de la notificación\n\n"

    val cuerpo = joinToString("\n---\n") { t ->
        val fecha = formatoFechaDiagnostico.format(Date(t.fecha))
        "[$fecha] ${t.tipo} | ${t.monto} | revisar=${t.necesitaRevision} | ${t.categoria}\n" +
            "RAW: ${t.notificacionCruda ?: "(sin texto)"}"
    }

    return encabezado + cuerpo
}
