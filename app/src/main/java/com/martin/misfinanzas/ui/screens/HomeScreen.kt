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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.theme.AmarilloAlerta
import com.martin.misfinanzas.ui.theme.GrisTexto
import com.martin.misfinanzas.ui.theme.VerdeMedio

@Composable
fun HomeScreen(
    balanceDigital: Double,
    balanceEfectivo: Double,
    transaccionesRecientes: List<Transaction>,
    accesoNotificacionesActivo: Boolean,
    onAbrirAjustesNotificaciones: () -> Unit,
    onClickTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!accesoNotificacionesActivo) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmarilloAlerta.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = AmarilloAlerta)
                            Text(
                                text = "  Falta activar la lectura automática de Nequi",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Sin este permiso, los movimientos de Nequi no se registran solos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrisTexto,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(onClick = onAbrirAjustesNotificaciones) {
                            Text("Activar ahora")
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = VerdeMedio),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Balance total", color = Color.White.copy(alpha = 0.85f))
                    Text(
                        text = (balanceDigital + balanceEfectivo).comoPesos(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BalanceMiniCard(
                    titulo = "Digital (Nequi)",
                    monto = balanceDigital,
                    modifier = Modifier.weight(1f)
                )
                BalanceMiniCard(
                    titulo = "Efectivo",
                    monto = balanceEfectivo,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Movimientos recientes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transaccionesRecientes.isEmpty()) {
            item {
                Text(
                    text = "Todavía no hay movimientos registrados.",
                    color = GrisTexto,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(transaccionesRecientes.take(15)) { transaccion ->
                TransactionRow(transaction = transaccion, onClick = { onClickTransaction(transaccion) })
            }
        }
    }
}

@Composable
private fun BalanceMiniCard(titulo: String, monto: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = GrisTexto)
            Text(
                text = monto.comoPesos(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
