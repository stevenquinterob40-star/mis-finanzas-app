package com.martin.misfinanzas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.martin.misfinanzas.data.Categorias
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.ui.colorParaCategoria
import com.martin.misfinanzas.ui.comoPesos
import com.martin.misfinanzas.ui.components.Pastilla
import com.martin.misfinanzas.ui.components.TarjetaSuave
import com.martin.misfinanzas.ui.theme.AmarilloAlerta
import com.martin.misfinanzas.ui.theme.RojoGasto
import com.martin.misfinanzas.ui.theme.VerdeIngreso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatoFecha = SimpleDateFormat("d MMM, h:mm a", Locale("es", "CO"))

@Composable
fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val esIngreso = transaction.tipo == TipoMovimiento.INGRESO
    val colorTipo = if (esIngreso) VerdeIngreso else RojoGasto

    TarjetaSuave(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = colorTipo.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (esIngreso) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = if (esIngreso) "Ingreso" else "Gasto",
                    tint = colorTipo,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatoFecha.format(Date(transaction.fecha)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                val mostrarCategoria = transaction.categoria != Categorias.SIN_CATEGORIA
                if (mostrarCategoria || transaction.necesitaRevision) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        if (mostrarCategoria) {
                            Pastilla(
                                texto = transaction.categoria,
                                color = colorParaCategoria(transaction.categoria)
                            )
                        }
                        if (mostrarCategoria && transaction.necesitaRevision) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (transaction.necesitaRevision) {
                            Pastilla(texto = "Revisar", color = AmarilloAlerta)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (esIngreso) "+" else "-") + transaction.monto.comoPesos(),
                    color = colorTipo,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (transaction.origen == Origen.DIGITAL) transaction.entidad else "Efectivo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
