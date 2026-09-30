package com.martin.misfinanzas.data

import java.text.SimpleDateFormat
import java.util.Locale

data class FilaExtracto(
    val fecha: Long,
    val descripcion: String,
    val debito: Double,
    val credito: Double,
    val lineaCruda: String
)

/**
 * Interpreta el CSV de extracto que se puede descargar desde Nequi
 * (columnas: fecha, descripcion, debito, credito, saldo).
 */
object CsvImportador {
    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale("es", "CO"))

    fun parsear(contenido: String): List<FilaExtracto> {
        val lineas = contenido.lines().filter { it.isNotBlank() }
        if (lineas.size <= 1) return emptyList()

        return lineas.drop(1).mapNotNull { lineaOriginal ->
            val linea = lineaOriginal.removePrefix("\uFEFF")
            val partes = linea.split(",")
            if (partes.size < 4) return@mapNotNull null
            try {
                val fecha = formatoFecha.parse(partes[0].trim())?.time ?: return@mapNotNull null
                val descripcion = partes[1].trim()
                val debito = partes[2].trim().toDoubleOrNull() ?: 0.0
                val credito = partes[3].trim().toDoubleOrNull() ?: 0.0
                FilaExtracto(fecha, descripcion, debito, credito, linea)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun esRetiro(descripcion: String): Boolean {
        val d = descripcion.lowercase()
        return "retiro en cajero" in d || "retiro en corresponsales" in d
    }

    fun categoriaSugerida(descripcion: String): String {
        val d = descripcion.lowercase()
        return when {
            esRetiro(descripcion) -> "Retiro"
            "tigo" in d -> "Servicios"
            "compra pse" in d -> "Compras"
            "pago en qr" in d -> "Compras"
            "pago de intereses" in d -> "Ahorros"
            else -> Categorias.SIN_CATEGORIA
        }
    }

    fun aMovimientos(contenido: String): List<MovimientoExtracto> {
        return parsear(contenido).mapNotNull { fila ->
            val tipo: TipoMovimiento
            val monto: Double
            when {
                fila.debito > 0 -> {
                    tipo = TipoMovimiento.GASTO
                    monto = fila.debito
                }
                fila.credito > 0 -> {
                    tipo = TipoMovimiento.INGRESO
                    monto = fila.credito
                }
                else -> return@mapNotNull null
            }
            MovimientoExtracto(
                fecha = fila.fecha,
                descripcion = fila.descripcion,
                tipo = tipo,
                monto = monto,
                categoria = categoriaSugerida(fila.descripcion),
                esRetiro = esRetiro(fila.descripcion),
                lineaCruda = fila.lineaCruda
            )
        }
    }
}
