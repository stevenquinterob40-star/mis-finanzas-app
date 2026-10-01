package com.martin.misfinanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Transaction

enum class Orden(val etiqueta: String) {
    RECIENTE("Más reciente"),
    MAYOR("Mayor a menor"),
    MENOR("Menor a mayor"),
    NOMBRE("Nombre A-Z")
}

val ORDEN_CATEGORIAS = listOf(Orden.MAYOR, Orden.MENOR, Orden.NOMBRE)
val ORDEN_MOVIMIENTOS = listOf(Orden.RECIENTE, Orden.MAYOR, Orden.MENOR, Orden.NOMBRE)

/** Ordena pares (categoría, monto). RECIENTE no aplica y se trata como mayor a menor. */
fun List<Pair<String, Double>>.ordenarCategorias(orden: Orden): List<Pair<String, Double>> = when (orden) {
    Orden.MENOR -> sortedBy { it.second }
    Orden.NOMBRE -> sortedBy { it.first.lowercase() }
    else -> sortedByDescending { it.second }
}

fun List<Transaction>.ordenarMovimientos(orden: Orden): List<Transaction> = when (orden) {
    Orden.RECIENTE -> sortedByDescending { it.fecha }
    Orden.MAYOR -> sortedByDescending { it.monto }
    Orden.MENOR -> sortedBy { it.monto }
    Orden.NOMBRE -> sortedBy { it.descripcion.lowercase() }
}

/** Fila deslizable de chips para elegir el orden. */
@Composable
fun BarraOrden(
    opciones: List<Orden>,
    seleccionado: Orden,
    onSeleccion: (Orden) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(opciones) { orden ->
            FilterChip(
                selected = seleccionado == orden,
                onClick = { onSeleccion(orden) },
                label = { Text(orden.etiqueta) }
            )
        }
    }
}
