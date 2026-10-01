package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.aTextoDiagnostico
import com.martin.misfinanzas.ui.components.TarjetaSuave
import com.martin.misfinanzas.ui.components.TituloPantalla
import com.martin.misfinanzas.ui.theme.VerdeSuave

@Composable
fun PendientesScreen(
    pendientes: List<Transaction>,
    automaticas: List<Transaction> = emptyList(),
    onClickTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var copiado by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { TituloPantalla("Pendientes") }

        item {
            Column {
                Text(
                    text = "Nequi mandó estas notificaciones pero no se pudieron clasificar solas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Toca cada una para poner el monto y confirmar si fue ingreso o gasto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
            }
        }

        item {
            TarjetaSuave(
                modifier = Modifier.fillMaxWidth(),
                containerColor = VerdeSuave
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "¿Algo se sigue clasificando mal? Copia el historial completo de notificaciones capturadas (${automaticas.size}) y pégalo en el chat con Claude para que lo revise en bloque.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(automaticas.aTextoDiagnostico()))
                            copiado = true
                        },
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(if (copiado) "¡Copiado! Pégalo en el chat" else "Copiar historial de notificaciones")
                    }
                }
            }
        }

        if (pendientes.isEmpty()) {
            item {
                Text(
                    text = "No hay nada pendiente por revisar. 🎉",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(pendientes) { transaccion ->
                TransactionRow(transaction = transaccion, onClick = { onClickTransaction(transaccion) })
            }
        }
    }
}
