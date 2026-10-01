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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.MovimientoExtracto
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.ComparacionExtracto
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.components.TarjetaSuave
import com.martin.misfinanzas.ui.components.TituloSeccion
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
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Cerrar comparación")
                }
                Text(
                    "Comparación con el extracto",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        item {
            TarjetaSuave(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (comparacion.limpiadosAutomaticamente > 0) {
                        FilaResumen(
                            "🧹",
                            "${comparacion.limpiadosAutomaticamente} duplicado(s) se limpiaron solos",
                            VerdeIngreso
                        )
                    }
                    FilaResumen("✅", "${comparacion.coinciden} ya estaban registrados", VerdeIngreso)
                    FilaResumen(
                        "➕",
                        "${comparacion.faltan.size} faltan en la app",
                        MaterialTheme.colorScheme.onSurface
                    )
                    FilaResumen(
                        "⚠️",
                        "${comparacion.sobran.size} están en la app pero NO aparecen en el extracto",
                        RojoGasto
                    )
                }
            }
        }

        if (comparacion.faltan.isNotEmpty()) {
            item {
                Button(
                    onClick = onAgregarFaltantes,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar los ${comparacion.faltan.size} que faltan")
                }
            }
            item { TituloSeccion("Faltan") }
            items(comparacion.faltan) { mov -> FilaMovimientoExtracto(mov) }
        }

        if (comparacion.sobran.isNotEmpty()) {
            item {
                Column {
                    TituloSeccion("Sobran en la app")
                    Text(
                        "Estos no aparecen en el extracto real — probablemente sea la causa de que el saldo no cuadre. Revísalos y bórralos si no corresponden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            items(comparacion.sobran) { t -> FilaSobrante(t, onEliminar = { onEliminarSobrante(t) }) }
        }
    }
}

@Composable
private fun FilaResumen(icono: String, texto: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text(icono)
        Text(
            text = "  $texto",
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FilaMovimientoExtracto(mov: MovimientoExtracto) {
    TarjetaSuave(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                mov.descripcion,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${mov.tipo} · ${mov.monto.comoPesos()} · ${formatoFecha.format(Date(mov.fecha))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun FilaSobrante(t: Transaction, onEliminar: () -> Unit) {
    TarjetaSuave(
        modifier = Modifier.fillMaxWidth(),
        containerColor = RojoGasto.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    t.descripcion,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${t.tipo} · ${t.monto.comoPesos()} · ${formatoFecha.format(Date(t.fecha))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar este movimiento",
                    tint = RojoGasto
                )
            }
        }
    }
}
