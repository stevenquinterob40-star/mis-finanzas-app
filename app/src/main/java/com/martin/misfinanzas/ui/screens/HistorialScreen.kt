package com.martin.misfinanzas.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.theme.GrisTexto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class FiltroOrigen { TODOS, DIGITAL, EFECTIVO }

private val formatoFechaCorta = SimpleDateFormat("d MMM yyyy", Locale("es", "CO"))

@Composable
fun HistorialScreen(
    transacciones: List<Transaction>,
    onClickTransaction: (Transaction) -> Unit,
    onImportarExtracto: () -> Unit = {},
    onLimpiarDuplicados: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var textoBusqueda by remember { mutableStateOf("") }
    var filtroOrigen by remember { mutableStateOf(FiltroOrigen.TODOS) }
    var filtroTipo by remember { mutableStateOf<TipoMovimiento?>(null) }
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var fechaDesde by remember { mutableStateOf<Long?>(null) }
    var fechaHasta by remember { mutableStateOf<Long?>(null) }

    val categoriasDisponibles = remember(transacciones) {
        transacciones.map { it.categoria }.distinct().sorted()
    }

    val listaFiltrada = transacciones
        .filter { t ->
            val busqueda = textoBusqueda.trim().lowercase()
            if (busqueda.isBlank()) true
            else {
                val porNombre = t.descripcion.lowercase().contains(busqueda)
                val porMonto = t.monto.toString().contains(busqueda) || t.monto.comoPesos().lowercase().contains(busqueda)
                val porTipo = t.tipo.name.lowercase().contains(busqueda) || (if (t.tipo == TipoMovimiento.INGRESO) "ingreso" else "gasto").contains(busqueda)
                val porEntidad = t.entidad.lowercase().contains(busqueda)
                porNombre || porMonto || porTipo || porEntidad
            }
        }
        .filter { t ->
            when (filtroOrigen) {
                FiltroOrigen.TODOS -> true
                FiltroOrigen.DIGITAL -> t.origen == Origen.DIGITAL
                FiltroOrigen.EFECTIVO -> t.origen == Origen.EFECTIVO
            }
        }
        .filter { t -> filtroTipo == null || t.tipo == filtroTipo }
        .filter { t -> categoriaSeleccionada == null || t.categoria == categoriaSeleccionada }
        .filter { t -> fechaDesde == null || t.fecha >= fechaDesde!! }
        .filter { t -> fechaHasta == null || t.fecha <= fechaHasta!! }

    fun abrirSelectorFecha(esInicio: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val seleccionado = Calendar.getInstance().apply {
                    set(year, month, day, if (esInicio) 0 else 23, if (esInicio) 0 else 59, if (esInicio) 0 else 59)
                    set(Calendar.MILLISECOND, if (esInicio) 0 else 999)
                }.timeInMillis
                if (esInicio) fechaDesde = seleccionado else fechaHasta = seleccionado
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onImportarExtracto, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Importar")
                }
                OutlinedButton(onClick = onLimpiarDuplicados, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Limpiar rep.")
                }
            }
        }

        item {
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                label = { Text("Buscar por nombre, valor, tipo o banco...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (textoBusqueda.isNotEmpty()) {
                        IconButton(onClick = { textoBusqueda = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filtroTipo == null,
                        onClick = { filtroTipo = null },
                        label = { Text("Todos los tipos") }
                    )
                }
                item {
                    FilterChip(
                        selected = filtroTipo == TipoMovimiento.INGRESO,
                        onClick = { filtroTipo = if (filtroTipo == TipoMovimiento.INGRESO) null else TipoMovimiento.INGRESO },
                        label = { Text("Ingresos") }
                    )
                }
                item {
                    FilterChip(
                        selected = filtroTipo == TipoMovimiento.GASTO,
                        onClick = { filtroTipo = if (filtroTipo == TipoMovimiento.GASTO) null else TipoMovimiento.GASTO },
                        label = { Text("Gastos") }
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filtroOrigen == FiltroOrigen.TODOS,
                    onClick = { filtroOrigen = FiltroOrigen.TODOS },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filtroOrigen == FiltroOrigen.DIGITAL,
                    onClick = { filtroOrigen = FiltroOrigen.DIGITAL },
                    label = { Text("Digital") }
                )
                FilterChip(
                    selected = filtroOrigen == FiltroOrigen.EFECTIVO,
                    onClick = { filtroOrigen = FiltroOrigen.EFECTIVO },
                    label = { Text("Efectivo") }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { abrirSelectorFecha(esInicio = true) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(fechaDesde?.let { formatoFechaCorta.format(Date(it)) } ?: "Desde")
                }
                OutlinedButton(onClick = { abrirSelectorFecha(esInicio = false) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(fechaHasta?.let { formatoFechaCorta.format(Date(it)) } ?: "Hasta")
                }
                if (fechaDesde != null || fechaHasta != null) {
                    IconButton(onClick = { fechaDesde = null; fechaHasta = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar filtro de fechas")
                    }
                }
            }
        }

        if (categoriasDisponibles.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = categoriaSeleccionada == null,
                            onClick = { categoriaSeleccionada = null },
                            label = { Text("Todas las categorías") }
                        )
                    }
                    items(categoriasDisponibles) { cat ->
                        FilterChip(
                            selected = categoriaSeleccionada == cat,
                            onClick = {
                                categoriaSeleccionada = if (categoriaSeleccionada == cat) null else cat
                            },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        item {
            val hayFiltroActivo = textoBusqueda.isNotBlank() || filtroOrigen != FiltroOrigen.TODOS ||
                filtroTipo != null || categoriaSeleccionada != null || fechaDesde != null || fechaHasta != null
            if (hayFiltroActivo) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${listaFiltrada.size} movimiento(s)", color = GrisTexto)
                        Text(
                            listaFiltrada.sumOf { if (it.tipo == TipoMovimiento.INGRESO) it.monto else -it.monto }.comoPesos(),
                            color = GrisTexto
                        )
                    }
                }
            }
        }

        if (listaFiltrada.isEmpty()) {
            item {
                Text(
                    text = "No hay movimientos con estos filtros.",
                    color = GrisTexto,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(listaFiltrada) { transaccion ->
                TransactionRow(transaction = transaccion, onClick = { onClickTransaction(transaccion) })
            }
        }
    }
}
