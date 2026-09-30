package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction

@Composable
fun AddTransactionDialog(
    transaccionExistente: Transaction?,
    onDismiss: () -> Unit,
    onGuardar: (Transaction) -> Unit,
    onEliminar: (Transaction) -> Unit
) {
    var monto by remember { mutableStateOf(transaccionExistente?.monto?.toLong()?.toString() ?: "") }
    var descripcion by remember { mutableStateOf(transaccionExistente?.descripcion ?: "") }
    var tipo by remember { mutableStateOf(transaccionExistente?.tipo ?: TipoMovimiento.GASTO) }
    var origen by remember { mutableStateOf(transaccionExistente?.origen ?: Origen.DIGITAL) }
    var categoria by remember { mutableStateOf(transaccionExistente?.categoria ?: "Sin categoría") }
    var entidad by remember { mutableStateOf(transaccionExistente?.entidad ?: "Nequi") }

    val bancos = listOf("Nequi", "Bancolombia", "Nu")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaccionExistente == null) "Nuevo movimiento" else "Editar movimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tipo == TipoMovimiento.GASTO, onClick = { tipo = TipoMovimiento.GASTO }, label = { Text("Gasto") })
                    FilterChip(selected = tipo == TipoMovimiento.INGRESO, onClick = { tipo = TipoMovimiento.INGRESO }, label = { Text("Ingreso") })
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = origen == Origen.DIGITAL, onClick = { origen = Origen.DIGITAL }, label = { Text("Digital") })
                    FilterChip(selected = origen == Origen.EFECTIVO, onClick = { origen = Origen.EFECTIVO }, label = { Text("Efectivo") })
                }

                if (origen == Origen.DIGITAL) {
                    Text("Banco:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bancos.forEach { banco ->
                            FilterChip(
                                selected = entidad == banco,
                                onClick = { entidad = banco },
                                label = { Text(banco) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = monto.toDoubleOrNull() ?: 0.0
                    if (m > 0 && descripcion.isNotBlank()) {
                        val entidadFinal = if (origen == Origen.DIGITAL) entidad else "Efectivo"
                        
                        if (transaccionExistente != null) {
                            onGuardar(transaccionExistente.copy(
                                monto = m, descripcion = descripcion, tipo = tipo, 
                                origen = origen, categoria = categoria, entidad = entidadFinal
                            ))
                        } else {
                            onGuardar(Transaction(
                                monto = m, descripcion = descripcion, tipo = tipo, 
                                origen = origen, fecha = System.currentTimeMillis(), 
                                categoria = categoria, entidad = entidadFinal
                            ))
                        }
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (transaccionExistente != null) {
                    TextButton(onClick = { onEliminar(transaccionExistente) }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}
