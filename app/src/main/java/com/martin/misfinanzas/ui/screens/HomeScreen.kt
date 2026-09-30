package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
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
    balanceNequi: Double,
    balanceBancolombia: Double,
    balanceNu: Double,
    balanceEfectivo: Double,
    transaccionesRecientes: List<Transaction>,
    accesoNotificacionesActivo: Boolean,
    onAbrirAjustesNotificaciones: () -> Unit,
    onClickTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = balanceNequi + balanceBancolombia + balanceNu + balanceEfectivo

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!accesoNotificacionesActivo) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = AmarilloAlerta.copy(alpha = 0.15f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = AmarilloAlerta)
                            Text("  Falta activar notificaciones", fontWeight = FontWeight.Medium)
                        }
                        Text("Sin este permiso, los movimientos no se registran solos.", style = MaterialTheme.typography.bodyMedium, color = GrisTexto)
                        TextButton(onClick = onAbrirAjustesNotificaciones) { Text("Activar ahora") }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = VerdeMedio), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Balance total disponible", color = Color.White.copy(alpha = 0.85f))
                    Text(text = total.comoPesos(), color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        item { Text("Desglose por entidad", style = MaterialTheme.typography.titleMedium) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BalanceMiniCard("Nequi", balanceNequi, Modifier.weight(1f))
                    BalanceMiniCard("Bancolombia", balanceBancolombia, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BalanceMiniCard("Nu Colombia", balanceNu, Modifier.weight(1f))
                    BalanceMiniCard("Efectivo", balanceEfectivo, Modifier.weight(1f))
                }
            }
        }

        item { Text("Movimientos recientes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }

        if (transaccionesRecientes.isEmpty()) {
            item { Text("Todavía no hay movimientos registrados.", color = GrisTexto, modifier = Modifier.padding(vertical = 24.dp)) }
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
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = GrisTexto)
            Text(text = monto.comoPesos(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
