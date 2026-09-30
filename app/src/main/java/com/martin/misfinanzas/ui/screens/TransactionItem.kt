package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.colorParaCategoria
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.theme.AmarilloAlerta
import com.martin.misfinanzas.ui.theme.GrisTexto
import com.martin.misfinanzas.ui.theme.RojoGasto
import com.martin.misfinanzas.ui.theme.VerdeClaro
import com.martin.misfinanzas.ui.theme.VerdeIngreso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM, h:mm a", Locale("es", "CO"))

@Composable
fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (transaction.origen == Origen.DIGITAL) VerdeClaro.copy(alpha = 0.4f)
                        else Color(0xFFEFE6D8),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.origen == Origen.DIGITAL) Icons.Filled.AccountBalanceWallet
                    else Icons.Filled.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatoFecha.format(Date(transaction.fecha)),
                        style = MaterialTheme.typography.labelSmall,
                        color = GrisTexto
                    )
                    if (transaction.categoria != com.martin.misfinanzas.data.Categorias.SIN_CATEGORIA) {
                        Text(
                            text = " · ${transaction.categoria}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorParaCategoria(transaction.categoria)
                        )
                    }
                    if (transaction.necesitaRevision) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Necesita revisión",
                            tint = AmarilloAlerta,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = " Revisar",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmarilloAlerta
                        )
                    }
                }
            }

            val esIngreso = transaction.tipo == TipoMovimiento.INGRESO
            Text(
                text = (if (esIngreso) "+" else "-") + transaction.monto.comoPesos(),
                color = if (esIngreso) VerdeIngreso else RojoGasto,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
