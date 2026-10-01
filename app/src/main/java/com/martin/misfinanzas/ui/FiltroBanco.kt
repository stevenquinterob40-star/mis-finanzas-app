package com.martin.misfinanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.Transaction

val OPCIONES_BANCO = listOf("Nequi", "Bancolombia", "Nu", "Efectivo")

/** null = todos. "Efectivo" filtra por origen; el resto, por entidad digital. */
fun Transaction.perteneceA(banco: String?): Boolean = when (banco) {
    null -> true
    "Efectivo" -> origen == Origen.EFECTIVO
    else -> origen == Origen.DIGITAL && entidad.equals(banco, ignoreCase = true)
}

/** Fila deslizable de chips: Todos los bancos / Nequi / Bancolombia / Nu / Efectivo. */
@Composable
fun BarraBancos(
    seleccionado: String?,
    onSeleccion: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = seleccionado == null,
                onClick = { onSeleccion(null) },
                label = { Text("Todos los bancos") }
            )
        }
        items(OPCIONES_BANCO) { banco ->
            FilterChip(
                selected = seleccionado == banco,
                onClick = { onSeleccion(if (seleccionado == banco) null else banco) },
                label = { Text(banco) }
            )
        }
    }
}
