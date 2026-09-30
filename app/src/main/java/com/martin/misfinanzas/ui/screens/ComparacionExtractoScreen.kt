package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.MovimientoExtracto
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.ComparacionExtracto
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.theme.GrisTexto
import com.martin.misfinanzas.ui.theme.RojoGasto
import com.martin.misfinanzas.ui.theme.VerdeIngreso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM yyyy", Locale("es", "CO"))

@Composable
fun ComparacionExtractoScreen(
    comparacion: ComparacionExtracto,
    onAgregarFaltantes: () -> Unit,
    onEliminarSobrante: (Transaction) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Cerrar comparación")
                }
                Text("Comparación con el extracto", style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (comparacion.limpiadosAutomaticamente > 0) {
                        Text(
                            "🧹 ${comparacion.limpiadosAutomaticamente} duplicado(s) se limpiaron solos",
                            color = VerdeIngreso,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text("✅ ${comparacion.coinciden} ya estaban registrados", color = VerdeIngreso)
                    Text("➕ ${comparacion.faltan.size} faltan en la app", modifier = Modifier.padding(top = 4.dp))
                    Text(
                        "⚠️ ${comparacion.sobran.size} están en la app pero NO aparecen en el extracto",
                        color = RojoGasto,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (comparacion.faltan.isNotEmpty()) {
            item {
                OutlinedButton(onClick = onAgregarFaltantes, modifier = Modifier.fillMaxWidth()) {
                    Text("Agregar los ${comparacion.faltan.size} que faltan")
                }
            }
            item {
                Text(
                    "Faltan",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(comparacion.faltan) { mov -> FilaMovimientoExtracto(mov) }
        }

        if (comparacion.sobran.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Sobran en la app", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Estos no aparecen en el extracto real — probablemente sea la causa de que el saldo no cuadre. Revísalos y bórralos si no corresponden.",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrisTexto,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            items(comparacion.sobran) { t -> FilaSobrante(t, onEliminar = { onEliminarSobrante(t) }) }
        }
    }
}

@Composable
private fun FilaMovimientoExtracto(mov: MovimientoExtracto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(mov.descripcion, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                "${mov.tipo} · ${mov.monto.comoPesos()} · ${formatoFecha.format(Date(mov.fecha))}",
                style = MaterialTheme.typography.labelSmall,
                color = GrisTexto
            )
        }
    }
}

@Composable
private fun FilaSobrante(t: Transaction, onEliminar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RojoGasto.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(t.descripcion, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${t.tipo} · ${t.monto.comoPesos()} · ${formatoFecha.format(Date(t.fecha))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrisTexto
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar este movimiento", tint = RojoGasto)
            }
        }
    }
}
