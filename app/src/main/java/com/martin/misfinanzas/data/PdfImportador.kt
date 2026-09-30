package com.martin.misfinanzas.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.text.SimpleDateFormat
import java.util.Locale

sealed class ResultadoLecturaPdf {
    data class Exito(val movimientos: List<MovimientoExtracto>) : ResultadoLecturaPdf()
    object RequierePassword : ResultadoLecturaPdf()
    data class Error(val mensaje: String) : ResultadoLecturaPdf()
}

/**
 * Lee el extracto en PDF de Nequi. Cada fila de la tabla tiene este formato:
 * "31/01/2026 De MATEO RUEDA MANCILLA $35,000.00 $118,322.20"
 * (fecha, descripción, valor CON signo -que puede ser negativo-, saldo).
 */
object PdfImportador {
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))

    private val REGEX_FILA = Regex(
        """(\d{2}/\d{2}/\d{4})\s+(.+?)\s+\$(-?[\d,]+\.\d{2})\s+\$-?[\d,]+\.\d{2}"""
    )

    fun leer(context: Context, uri: Uri, password: String?): ResultadoLecturaPdf {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ResultadoLecturaPdf.Error("No se pudo abrir el archivo.")

            val documento = if (password != null) {
                PDDocument.load(inputStream, password)
            } else {
                PDDocument.load(inputStream)
            }

            val texto = PDFTextStripper().getText(documento)
            documento.close()

            val movimientos = parsearTexto(texto)
            if (movimientos.isEmpty()) {
                ResultadoLecturaPdf.Error(
                    "No se encontró ningún movimiento con el formato esperado. " +
                        "Puede que este extracto tenga un formato distinto al de Nequi."
                )
            } else {
                ResultadoLecturaPdf.Exito(movimientos)
            }
        } catch (e: Exception) {
            val mensaje = e.message?.lowercase() ?: ""
            if (mensaje.contains("password") || mensaje.contains("encrypt")) {
                ResultadoLecturaPdf.RequierePassword
            } else {
                ResultadoLecturaPdf.Error(e.message ?: "No se pudo leer el PDF.")
            }
        }
    }

    private fun parsearTexto(texto: String): List<MovimientoExtracto> {
        return REGEX_FILA.findAll(texto).mapNotNull { match ->
            try {
                val fecha = formatoFecha.parse(match.groupValues[1])?.time ?: return@mapNotNull null
                val descripcion = match.groupValues[2].trim()
                val valor = match.groupValues[3].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null

                val tipo = if (valor < 0) TipoMovimiento.GASTO else TipoMovimiento.INGRESO
                val monto = kotlin.math.abs(valor)

                MovimientoExtracto(
                    fecha = fecha,
                    descripcion = descripcion,
                    tipo = tipo,
                    monto = monto,
                    categoria = CsvImportador.categoriaSugerida(descripcion),
                    esRetiro = CsvImportador.esRetiro(descripcion),
                    lineaCruda = match.value
                )
            } catch (e: Exception) {
                null
            }
        }.toList()
    }
}
