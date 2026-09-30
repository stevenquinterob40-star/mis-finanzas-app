package com.martin.misfinanzas.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MontoVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val crudo = text.text
        val indexComa = crudo.indexOf(',')

        val parteEntera = if (indexComa == -1) crudo else crudo.substring(0, indexComa)
        val resto = if (indexComa == -1) "" else crudo.substring(indexComa)

        val enteraConPuntos = agregarPuntosDeMiles(parteEntera)
        val transformado = enteraConPuntos + resto

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= parteEntera.length) {
                    return offset + separadoresAntesDe(parteEntera.length, offset)
                }
                val totalSeparadores = separadoresAntesDe(parteEntera.length, parteEntera.length)
                return parteEntera.length + totalSeparadores + (offset - parteEntera.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                for (o in 0..crudo.length) {
                    if (originalToTransformed(o) >= offset) return o
                }
                return crudo.length
            }
        }

        return TransformedText(AnnotatedString(transformado), offsetMapping)
    }

    private fun agregarPuntosDeMiles(numero: String): String =
        numero.reversed().chunked(3).joinToString(".").reversed()

    private fun separadoresAntesDe(n: Int, offset: Int): Int {
        val totalSeparadores = if (n <= 0) 0 else (n - 1) / 3
        var contador = 0
        for (k in 1..totalSeparadores) {
            val posicionSeparador = n - 3 * k
            if (posicionSeparador < offset) contador++
        }
        return contador
    }
}
