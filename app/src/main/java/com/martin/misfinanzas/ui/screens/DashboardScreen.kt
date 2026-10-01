package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.colorParaCategoria
import com.martin.misfinanzas.ui.BarraBancos
import com.martin.misfinanzas.ui.BarraOrden
import com.martin.misfinanzas.ui.ORDEN_CATEGORIAS
import com.martin.misfinanzas.ui.ORDEN_MOVIMIENTOS
import com.martin.misfinanzas.ui.Orden
import com.martin.misfinanzas.ui.ordenarCategorias
import com.martin.misfinanzas.ui.ordenarMovimientos
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.perteneceA
import com.martin.misfinanzas.ui.components.TarjetaBalanceHero
import com.martin.misfinanzas.ui.components.TarjetaSuave
import com.martin.misfinanzas.ui.components.TituloPantalla
import com.martin.misfinanzas.ui.theme.GrisTexto
import com.martin.misfinanzas.ui.theme.RojoGasto
import com.martin.misfinanzas.ui.theme.VerdeIngreso
import com.martin.misfinanzas.ui.theme.VerdeMedio
import java.time.LocalDate
import java.time.ZoneId

private enum class Periodo(val etiqueta: String) {
    DIA("Día"),
    SEMANA("Semana"),
    MES("Mes"),
    ANIO("Año")
}

private data class FiltroDetalle(val tipo: TipoMovimiento, val categoria: String?)

private fun inicioDePeriodo(periodo: Periodo): Long {
    val zona = ZoneId.systemDefault()
    val hoy = LocalDate.now(zona)
    val inicio: LocalDate = when (periodo) {
        Periodo.DIA -> hoy
        Periodo.SEMANA -> hoy.minusDays((hoy.dayOfWeek.value - 1).toLong())
        Periodo.MES -> hoy.withDayOfMonth(1)
        Periodo.ANIO -> hoy.withDayOfYear(1)
    }
    return inicio.atStartOfDay(zona).toInstant().toEpochMilli()
}

@Composable
fun DashboardScreen(
    transacciones: List<Transaction>,
    onClickTransaction: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var periodo by remember { mutableStateOf(Periodo.MES) }
    var textoBusqueda by remember { mutableStateOf("") }
    var filtroBanco by remember { mutableStateOf<String?>(null) }
    var ordenCategorias by remember { mutableStateOf(Orden.MAYOR) }
    var filtroTipoResumen by remember { mutableStateOf<TipoMovimiento?>(null) }
    var filtroDetalle by remember { mutableStateOf<FiltroDetalle?>(null) }

    val inicioMillis = remember(periodo) { inicioDePeriodo(periodo) }
    
    val filtradas = transacciones
        .filter { it.fecha >= inicioMillis && !it.necesitaRevision }
        .filter { it.perteneceA(filtroBanco) }
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
        .filter { t -> filtroTipoResumen == null || t.tipo == filtroTipoResumen }

    val filtroActual = filtroDetalle
    if (filtroActual != null) {
        val transaccionesDelFiltro = filtradas.filter {
            it.tipo == filtroActual.tipo && (filtroActual.categoria == null || it.categoria == filtroActual.categoria)
        }
        DetalleFiltradoScreen(
            filtro = filtroActual,
            transacciones = transaccionesDelFiltro,
            onVolver = { filtroDetalle = null },
            onClickTransaction = onClickTransaction,
            modifier = modifier
        )
        return
    }

    val ingresos = filtradas.filter { it.tipo == TipoMovimiento.INGRESO }.sumOf { it.monto }
    val gastos = filtradas.filter { it.tipo == TipoMovimiento.GASTO }.sumOf { it.monto }
    val balance = ingresos - gastos

    val ingresosDigital = filtradas.filter { it.tipo == TipoMovimiento.INGRESO && it.origen == Origen.DIGITAL }.sumOf { it.monto }
    val gastosDigital = filtradas.filter { it.tipo == TipoMovimiento.GASTO && it.origen == Origen.DIGITAL }.sumOf { it.monto }
    val totalMovidoDigital = ingresosDigital + gastosDigital
    val balanceDigitalPeriodo = ingresosDigital - gastosDigital

    val gastosPorCategoria = filtradas
        .filter { it.tipo == TipoMovimiento.GASTO }
        .groupBy { it.categoria }
        .mapValues { (_, lista) -> lista.sumOf { it.monto } }
        .toList()
        .ordenarCategorias(ordenCategorias)

    val ingresosPorCategoria = filtradas
        .filter { it.tipo == TipoMovimiento.INGRESO }
        .groupBy { it.categoria }
        .mapValues { (_, lista) -> lista.sumOf { it.monto } }
        .toList()
        .ordenarCategorias(ordenCategorias)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TituloPantalla("Resumen") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Periodo.entries.forEach { p ->
                    FilterChip(
                        selected = periodo == p,
                        onClick = { periodo = p },
                        label = { Text(p.etiqueta) }
                    )
                }
            }
        }

        item { BarraBancos(seleccionado = filtroBanco, onSeleccion = { filtroBanco = it }) }

        item {
            BarraOrden(
                opciones = ORDEN_CATEGORIAS,
                seleccionado = ordenCategorias,
                onSeleccion = { ordenCategorias = it }
            )
        }

        item {
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                label = { Text("Buscar en resumen por nombre, valor, tipo...") },
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
                        selected = filtroTipoResumen == null,
                        onClick = { filtroTipoResumen = null },
                        label = { Text("Todos los tipos") }
                    )
                }
                item {
                    FilterChip(
                        selected = filtroTipoResumen == TipoMovimiento.INGRESO,
                        onClick = { filtroTipoResumen = if (filtroTipoResumen == TipoMovimiento.INGRESO) null else TipoMovimiento.INGRESO },
                        label = { Text("Ver solo Ingresos") }
                    )
                }
                item {
                    FilterChip(
                        selected = filtroTipoResumen == TipoMovimiento.GASTO,
                        onClick = { filtroTipoResumen = if (filtroTipoResumen == TipoMovimiento.GASTO) null else TipoMovimiento.GASTO },
                        label = { Text("Ver solo Gastos") }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResumenMiniCard(
                    titulo = "Ingresos",
                    monto = ingresos,
                    color = VerdeIngreso,
                    onClick = { filtroDetalle = FiltroDetalle(TipoMovimiento.INGRESO, null) },
                    modifier = Modifier.weight(1f)
                )
                ResumenMiniCard(
                    titulo = "Gastos",
                    monto = gastos,
                    color = RojoGasto,
                    onClick = { filtroDetalle = FiltroDetalle(TipoMovimiento.GASTO, null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            TarjetaBalanceHero(
                titulo = "Balance de ${periodo.etiqueta.lowercase()}",
                monto = balance.comoPesos()
            )
        }

        item {
            TarjetaSuave(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Movimientos bancarios · ${periodo.etiqueta.lowercase()}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Ingresos", style = MaterialTheme.typography.labelSmall, color = GrisTexto)
                            Text(ingresosDigital.comoPesos(), color = VerdeIngreso, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Gastos", style = MaterialTheme.typography.labelSmall, color = GrisTexto)
                            Text(gastosDigital.comoPesos(), color = RojoGasto, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total movido: ${totalMovidoDigital.comoPesos()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrisTexto
                        )
                    }
                    Text(
                        text = "Balance digital del periodo: ${balanceDigitalPeriodo.comoPesos()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrisTexto,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Gastos por categoría",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (gastosPorCategoria.isEmpty()) {
            item { Text("Sin gastos en este filtro.", color = GrisTexto) }
        } else {
            items(gastosPorCategoria) { (categoria, monto) ->
                BarraCategoria(
                    categoria = categoria,
                    monto = monto,
                    totalDelTipo = gastos,
                    color = RojoGasto,
                    onClick = { filtroDetalle = FiltroDetalle(TipoMovimiento.GASTO, categoria) }
                )
            }
        }

        item {
            Text(
                text = "Ingresos por categoría",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (ingresosPorCategoria.isEmpty()) {
            item { Text("Sin ingresos en este filtro.", color = GrisTexto) }
        } else {
            items(ingresosPorCategoria) { (categoria, monto) ->
                BarraCategoria(
                    categoria = categoria,
                    monto = monto,
                    totalDelTipo = ingresos,
                    color = VerdeIngreso,
                    onClick = { filtroDetalle = FiltroDetalle(TipoMovimiento.INGRESO, categoria) }
                )
            }
        }
    }
}

@Composable
private fun DetalleFiltradoScreen(
    filtro: FiltroDetalle,
    transacciones: List<Transaction>,
    onVolver: () -> Unit,
    onClickTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (filtro.tipo == TipoMovimiento.INGRESO) VerdeIngreso else RojoGasto
    val nombreTipo = if (filtro.tipo == TipoMovimiento.INGRESO) "Ingresos" else "Gastos"
    val titulo = if (filtro.categoria != null) "$nombreTipo · ${filtro.categoria}" else nombreTipo
    var orden by remember { mutableStateOf(Orden.RECIENTE) }
    val total = transacciones.sumOf { it.monto }
    val ordenadas = transacciones.ordenarMovimientos(orden)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver al resumen")
                }
                Text(titulo, style = MaterialTheme.typography.titleMedium)
            }
        }

        item {
            TarjetaSuave(
                modifier = Modifier.fillMaxWidth(),
                containerColor = color.copy(alpha = 0.12f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = GrisTexto)
                    Text(
                        text = total.comoPesos(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "${transacciones.size} movimiento(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrisTexto
                    )
                }
            }
        }

        item {
            BarraOrden(
                opciones = ORDEN_MOVIMIENTOS,
                seleccionado = orden,
                onSeleccion = { orden = it }
            )
        }

        if (transacciones.isEmpty()) {
            item { Text("No hay movimientos en este filtro.", color = GrisTexto) }
        } else {
            items(ordenadas) { transaccion ->
                TransactionRow(transaction = transaccion, onClick = { onClickTransaction(transaccion) })
            }
        }
    }
}

@Composable
private fun ResumenMiniCard(
    titulo: String,
    monto: Double,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TarjetaSuave(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(titulo, style = MaterialTheme.typography.labelSmall, color = GrisTexto)
                Text(
                    text = monto.comoPesos(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Ver detalle",
                tint = GrisTexto,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BarraCategoria(
    categoria: String,
    monto: Double,
    totalDelTipo: Double,
    color: Color,
    onClick: () -> Unit
) {
    val fraccion = if (totalDelTipo > 0) (monto / totalDelTipo).toFloat().coerceIn(0f, 1f) else 0f
    TarjetaSuave(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = colorParaCategoria(categoria), shape = CircleShape)
                    )
                    Text(
                        text = "  $categoria",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(monto.comoPesos(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Ver detalle",
                        tint = GrisTexto,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(top = 6.dp)
                    .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraccion)
                        .height(8.dp)
                        .background(color = color, shape = RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
