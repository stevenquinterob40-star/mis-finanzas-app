package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.components.TarjetaBalanceHero
import com.martin.misfinanzas.ui.components.TarjetaSuave
import com.martin.misfinanzas.ui.components.TituloSeccion
import com.martin.misfinanzas.ui.theme.AmarilloAlerta

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
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!accesoNotificacionesActivo) {
            item {
                TarjetaSuave(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AmarilloAlerta.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = AmarilloAlerta
                            )
                            Text(
                                text = "  Falta activar la lectura de notificaciones",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = "Sin este permiso, los movimientos de Nequi, Bancolombia y Nu no se registran solos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                        )
                        Button(onClick = onAbrirAjustesNotificaciones) {
                            Text("Activar ahora")
                        }
                    }
                }
            }
        }

        item {
            TarjetaBalanceHero(
                titulo = "Balance total disponible",
                monto = total.comoPesos()
            )
        }

        item { TituloSeccion("Por entidad") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BalanceMiniCard("Nequi", balanceNequi, Color(0xFFD81B60), Modifier.weight(1f))
                    BalanceMiniCard("Bancolombia", balanceBancolombia, Color(0xFFF2A900), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BalanceMiniCard("Nu Colombia", balanceNu, Color(0xFF820AD1), Modifier.weight(1f))
                    BalanceMiniCard("Efectivo", balanceEfectivo, Color(0xFF2E7D32), Modifier.weight(1f))
                }
            }
        }

        item { TituloSeccion("Movimientos recientes") }

        if (transaccionesRecientes.isEmpty()) {
            item {
                Text(
                    text = "Todavía no hay movimientos registrados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun BalanceMiniCard(
    titulo: String,
    monto: Double,
    colorEntidad: Color,
    modifier: Modifier = Modifier
) {
    TarjetaSuave(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colorEntidad, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = monto.comoPesos(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
