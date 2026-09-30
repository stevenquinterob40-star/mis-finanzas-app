package com.martin.misfinanzas.notification

import com.martin.misfinanzas.data.TipoMovimiento

object NequiParser {

    data class Resultado(
        val monto: Double?,
        val tipo: TipoMovimiento?,
        val confiable: Boolean,
        val entidad: String
    )

    private val PALABRAS_INGRESO = listOf(
        "recibiste", "te enviaron", "te envió", "te llegó", "te llego",
        "abono a tu cuenta", "recibiste un pago", "recibiste dinero",
        "consignación", "consignacion", "te transfirieron",
        "recargaste", "recarga exitosa", "tu recarga fue un éxito",
        "tu recarga fue un exito", "transferencia recibida"
    )

    private val PALABRAS_GASTO = listOf(
        "enviaste", "realizaste una compra", "compraste", "pagaste",
        "retiraste", "transferiste", "compra por", "pago realizado",
        "envío de", "envio de", "retiro por", "compra aprobada",
        "pago exitoso", "hiciste un pago", "transferencia exitosa",
        "transferencia enviada"
    )

    private val PALABRAS_PUBLICIDAD = listOf(
        "descuento", "promoción", "promocion", "cupón", "cupon",
        "oferta especial", "sorteo", "gana premios", "invita a tus amigos",
        "actualiza tu app", "confirma tu pago en nequi"
    )

    private val PALABRAS_CONFIRMACION_GENERICA = listOf(
        "tu plata llegó con éxito", "tu plata llego con exito",
        "envío exitoso", "envio exitoso", "salió bien", "salio bien"
    )

    private val PALABRAS_RETIRO = listOf(
        "retiraste", "retiro por", "retiro en cajero", "retiro exitoso",
        "realizaste un retiro", "retiro aprobado", "retiro de efectivo"
    )

    private val REGEX_MONTO = Regex(
        """\$\s?([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)|([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{3,})"""
    )

    fun esPublicidad(titulo: String, texto: String): Boolean {
        val combinado = "$titulo $texto".lowercase()
        return PALABRAS_PUBLICIDAD.any { combinado.contains(it) }
    }

    fun esConfirmacionGenerica(titulo: String, texto: String): Boolean {
        val combinado = "$titulo $texto".lowercase()
        return PALABRAS_CONFIRMACION_GENERICA.any { combinado.contains(it) }
    }

    fun esRetiro(titulo: String, texto: String): Boolean {
        val combinado = "$titulo $texto".lowercase()
        return PALABRAS_RETIRO.any { combinado.contains(it) }
    }

    fun parse(paquete: String, titulo: String, texto: String): Resultado {
        val combinado = "$titulo $texto".lowercase()

        val entidad = when {
            paquete.contains("bancolombia", ignoreCase = true) -> "Bancolombia"
            paquete.contains("nu", ignoreCase = true) -> "Nu"
            else -> "Nequi"
        }

        val esIngreso = PALABRAS_INGRESO.any { combinado.contains(it) }
        val esGasto = PALABRAS_GASTO.any { combinado.contains(it) }

        val tipo = when {
            esIngreso && !esGasto -> TipoMovimiento.INGRESO
            esGasto && !esIngreso -> TipoMovimiento.GASTO
            else -> null
        }

        val monto = extraerMonto(combinado)
        val confiable = tipo != null && monto != null && monto > 0

        return Resultado(monto = monto, tipo = tipo, confiable = confiable, entidad = entidad)
    }

    private fun extraerMonto(texto: String): Double? {
        val match = REGEX_MONTO.find(texto) ?: return null
        val grupo = match.groupValues[1].ifEmpty { match.groupValues[2] }
        val soloNumeros = grupo.replace(".", "").replace(",", "")
        return soloNumeros.toDoubleOrNull()
    }
}
