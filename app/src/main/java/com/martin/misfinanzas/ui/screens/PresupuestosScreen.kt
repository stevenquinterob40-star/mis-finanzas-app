package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Categorias
import com.martin.misfinanzas.data.CategoriasPersonalizadas
import com.martin.misfinanzas.data.Presupuestos
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.aMontoDouble
import com.martin.misfinanzas.ui.aTextoEntrada
import com.martin.misfinanzas.ui.colorParaCategoria
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.components.Pastilla
import com.martin.misfinanzas.ui.components.TarjetaSuave
import com.martin.misfinanzas.ui.components.TituloPantalla
import com.martin.misfinanzas.ui.theme.AmarilloAlerta
import com.martin.misfinanzas.ui.theme.GrisTexto
import com.martin.misfinanzas.ui.theme.RojoGasto
import com.martin.misfinanzas.ui.theme.VerdeMedio
import java.time.LocalDate
import java.time.ZoneId

private fun inicioDelMes(): Long {
    val zona = ZoneId.systemDefault()
    return LocalDate.now(zona).withDayOfMonth(1).atStartOfDay(zona).toInstant().toEpochMilli()
}

@Composable
fun PresupuestosScreen(
    transacciones: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var presupuestos by remember { mutableStateOf(Presupuestos.obtener(context)) }
    var dialogoAbierto by remember { mutableStateOf(false) }
    var categoriaEnEdicion by remember { mutableStateOf<String?>(null) }

    val desde = inicioDelMes()
    val gastoPorCategoria = transacciones
        .filter { it.tipo == TipoMovimiento.GASTO && !it.necesitaRevision && it.fecha >= desde }
        .groupBy { it.categoria }
        .mapValues { (_, lista) -> lista.sumOf { it.monto } }

    val totalPresupuestado = presupuestos.values.sum()
    val totalGastado = presupuestos.keys.sumOf { gastoPorCategoria[it] ?: 0.0 }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TituloPantalla("Presupuestos del mes") }

        item {
            TarjetaSuave(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (presupuestos.isEmpty()) "Aún no tienes presupuestos"
                        else "${totalGastado.comoPesos()} de ${totalPresupuestado.comoPesos()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (presupuestos.isEmpty())
                            "Define un límite mensual por categoría y mira cuánto llevas gastado."
                        else "Solo cuentan los gastos confirmados de este mes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Button(onClick = {
                        categoriaEnEdicion = null
                        dialogoAbierto = true
                    }) {
                        Text("Agregar presupuesto")
                    }
                }
            }
        }

        items(presupuestos.entries.sortedBy { it.key }.toList()) { entrada ->
            val categoria = entrada.key
            val limite = entrada.value
            val gastado = gastoPorCategoria[categoria] ?: 0.0
            val proporcion = (gastado / limite).toFloat()
            val colorEstado = when {
                proporcion >= 1f -> RojoGasto
                proporcion >= 0.8f -> AmarilloAlerta
                else -> VerdeMedio
            }
            TarjetaSuave(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    categoriaEnEdicion = categoria
                    dialogoAbierto = true
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pastilla(texto = categoria, color = colorParaCategoria(categoria))
                        Text(
                            text = "${(proporcion * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorEstado,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { proporcion.coerceIn(0f, 1f) },
                        color = colorEstado,
                        trackColor = colorEstado.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp)
                            .height(8.dp)
                    )
                    Text(
                        text = "${gastado.comoPesos()} de ${limite.comoPesos()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val restante = limite - gastado
                    Text(
                        text = if (restante >= 0) "Te quedan ${restante.comoPesos()}"
                        else "Te pasaste por ${(-restante).comoPesos()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (restante >= 0) GrisTexto else RojoGasto
                    )
                }
            }
        }
    }

    if (dialogoAbierto) {
        DialogoPresupuesto(
            categoriaInicial = categoriaEnEdicion,
            limiteInicial = categoriaEnEdicion?.let { presupuestos[it] },
            onDismiss = { dialogoAbierto = false },
            onGuardar = { categoria, monto ->
                Presupuestos.guardar(context, categoria, monto)
                presupuestos = Presupuestos.obtener(context)
                dialogoAbierto = false
            },
            onEliminar = categoriaEnEdicion?.let { cat ->
                {
                    Presupuestos.eliminar(context, cat)
                    presupuestos = Presupuestos.obtener(context)
                    dialogoAbierto = false
                }
            }
        )
    }
}

@Composable
private fun DialogoPresupuesto(
    categoriaInicial: String?,
    limiteInicial: Double?,
    onDismiss: () -> Unit,
    onGuardar: (String, Double) -> Unit,
    onEliminar: (() -> Unit)?
) {
    val context = LocalContext.current
    var categoria by remember { mutableStateOf(categoriaInicial ?: "") }
    var monto by remember { mutableStateOf(limiteInicial?.aTextoEntrada() ?: "") }
    val sugeridas = remember {
        (Categorias.SUGERIDAS.filter { it != "Sueldo/Ingresos" && it != "Retiro" } +
            CategoriasPersonalizadas.obtener(context)).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(if (categoriaInicial == null) "Nuevo presupuesto" else "Editar presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría") },
                    enabled = categoriaInicial == null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (categoriaInicial == null) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sugeridas) { cat ->
                            OutlinedButton(onClick = { categoria = cat }) { Text(cat) }
                        }
                    }
                }
                OutlinedTextField(
                    value = monto,
                    onValueChange = { nuevo -> monto = nuevo.filter { it.isDigit() } },
                    label = { Text("Límite mensual") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val valor = monto.aMontoDouble() ?: 0.0
                if (categoria.isNotBlank() && valor > 0) onGuardar(categoria, valor)
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onEliminar != null) {
                    TextButton(onClick = onEliminar) { Text("Eliminar", color = RojoGasto) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}
