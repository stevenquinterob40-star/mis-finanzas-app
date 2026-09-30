package com.martin.misfinanzas.ui

import java.text.NumberFormat
import java.util.Locale

private val formatoCOP: NumberFormat = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 2
}

fun Double.comoPesos(): String = "$" + formatoCOP.format(this)

fun Double.aTextoEntrada(): String {
    val centavos = Math.round(this * 100)
    val enteros = centavos / 100
    val decimales = (centavos % 100).toInt()
    return if (decimales == 0) enteros.toString() else "$enteros,${decimales.toString().padStart(2, '0')}"
}

fun String.aMontoDouble(): Double? {
    if (isBlank()) return null
    val partes = split(",")
    val entero = partes[0].ifEmpty { "0" }.toLongOrNull() ?: return null
    val decimalTexto = if (partes.size > 1) partes[1].padEnd(2, '0').take(2) else "00"
    val decimal = decimalTexto.toIntOrNull() ?: 0
    return entero + decimal / 100.0
}
