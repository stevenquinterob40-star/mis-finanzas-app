package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Categorias
import com.martin.misfinanzas.data.CategoriasPersonalizadas
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.data.UsoCategorias
import com.martin.misfinanzas.ui.MontoVisualTransformation
import com.martin.misfinanzas.ui.aMontoDouble
import com.martin.misfinanzas.ui.aTextoEntrada
import com.martin.misfinanzas.ui.colorParaCategoria
import com.martin.misfinanzas.ui.theme.RojoGasto
import com.martin.misfinanzas.ui.theme.VerdeIngreso

private val BANCOS = listOf("Nequi", "Bancolombia", "Nu")

@Composable
fun AddTransactionDialog(
    transaccionExistente: Transaction? = null,
    onDismiss: () -> Unit,
    onGuardar: (Transaction) -> Unit,
    onEliminar: ((Transaction) -> Unit)? = null
) {
    var monto by remember {
        mutableStateOf(
            if (transaccionExistente != null && transaccionExistente.monto > 0)
                transaccionExistente.monto.aTextoEntrada()
            else ""
        )
    }
    var descripcion by remember { mutableStateOf(transaccionExistente?.descripcion ?: "") }
    var tipo by remember { mutableStateOf(transaccionExistente?.tipo ?: TipoMovimiento.GASTO) }
    var origen by remember { mutableStateOf(transaccionExistente?.origen ?: Origen.EFECTIVO) }
    var categoria by remember { mutableStateOf(transaccionExistente?.categoria ?: Categorias.SIN_CATEGORIA) }
    var entidad by remember {
        mutableStateOf(
            transaccionExistente?.entidad?.takeIf { it in BANCOS } ?: BANCOS.first()
        )
    }

    val context = LocalContext.current
    val categoriasPersonalizadas = remember { CategoriasPersonalizadas.obtener(context) }
    val todasLasCategorias = remember(categoriasPersonalizadas) {
        UsoCategorias.ordenarPorUso(context, (Categorias.SUGERIDAS + categoriasPersonalizadas).distinct())
    }

    val esEdicion = transaccionExistente != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(if (esEdicion) "Confirmar movimiento" else "Nuevo movimiento") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {

                if (transaccionExistente?.notificacionCruda != null) {
                    Text(
                        text = "Notificación original: ${transaccionExistente.notificacionCruda}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedTextField(
                    value = monto,
                    onValueChange = { nuevo ->
                        val normalizado = nuevo.replace(".", ",")
                        val soloValido = normalizado.filter { it.isDigit() || it == ',' }
                        val indexComa = soloValido.indexOf(',')
                        monto = if (indexComa == -1) {
                            soloValido
                        } else {
                            val entera = soloValido.substring(0, indexComa)
                            val decimal = soloValido.substring(indexComa + 1)
                                .filter { it.isDigit() }
                                .take(2)
                            "$entera,$decimal"
                        }
                    },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = MontoVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$") }
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipo == TipoMovimiento.INGRESO,
                        onClick = { tipo = TipoMovimiento.INGRESO },
                        label = { Text("Ingreso") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = VerdeIngreso.copy(alpha = 0.12f),
                            labelColor = VerdeIngreso,
                            selectedContainerColor = VerdeIngreso,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = tipo == TipoMovimiento.GASTO,
                        onClick = { tipo = TipoMovimiento.GASTO },
                        label = { Text("Gasto") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = RojoGasto.copy(alpha = 0.12f),
                            labelColor = RojoGasto,
                            selectedContainerColor = RojoGasto,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Text("Origen", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = origen == Origen.EFECTIVO,
                        onClick = { origen = Origen.EFECTIVO },
                        label = { Text("Efectivo") }
                    )
                    FilterChip(
                        selected = origen == Origen.DIGITAL,
                        onClick = { origen = Origen.DIGITAL },
                        label = { Text("Digital") }
                    )
                }

                if (origen == Origen.DIGITAL) {
                    Text("Banco", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BANCOS.forEach { banco ->
                            FilterChip(
                                selected = entidad == banco,
                                onClick = { entidad = banco },
                                label = { Text(banco) }
                            )
                        }
                    }
                }

                Text("Categoría", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(todasLasCategorias) { cat ->
                        val colorCat = colorParaCategoria(cat)
                        FilterChip(
                            selected = categoria == cat,
                            onClick = { categoria = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colorCat.copy(alpha = 0.15f),
                                labelColor = colorCat,
                                selectedContainerColor = colorCat,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("O escribe una categoría propia") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val montoDouble = monto.aMontoDouble() ?: 0.0
                if (montoDouble <= 0 || descripcion.isBlank()) return@Button
                val categoriaFinal = categoria.ifBlank { Categorias.SIN_CATEGORIA }
                CategoriasPersonalizadas.agregar(context, categoriaFinal)
                UsoCategorias.registrarUso(context, categoriaFinal)
                val resultado = (transaccionExistente ?: Transaction(
                    monto = 0.0,
                    tipo = tipo,
                    origen = origen,
                    descripcion = descripcion,
                    fecha = System.currentTimeMillis()
                )).copy(
                    monto = montoDouble,
                    tipo = tipo,
                    origen = origen,
                    descripcion = descripcion,
                    categoria = categoriaFinal,
                    entidad = if (origen == Origen.DIGITAL) entidad else "Efectivo",
                    necesitaRevision = false
                )
                onGuardar(resultado)
            }) {
                Text(if (esEdicion) "Confirmar" else "Guardar")
            }
        },
        dismissButton = {
            Row {
                if (transaccionExistente != null && onEliminar != null) {
                    TextButton(onClick = { onEliminar(transaccionExistente) }) {
                        Text("Eliminar", color = RojoGasto)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}
