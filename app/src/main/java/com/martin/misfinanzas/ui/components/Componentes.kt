package com.martin.misfinanzas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martin.misfinanzas.ui.theme.VerdeMedio
import com.martin.misfinanzas.ui.theme.VerdeOscuro

/** Tarjeta grande con degradado verde para el balance principal. */
@Composable
fun TarjetaBalanceHero(
    titulo: String,
    monto: String,
    modifier: Modifier = Modifier,
    detalle: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(VerdeOscuro, VerdeMedio)))
            .padding(22.dp)
    ) {
        Column {
            Text(
                text = titulo,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = monto,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            if (detalle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detalle,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/** Título grande de pantalla. */
@Composable
fun TituloPantalla(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

/** Título de sección dentro de una pantalla. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

/** Tarjeta plana con borde fino; si recibe onClick, es tocable. */
@Composable
fun TarjetaSuave(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val forma = RoundedCornerShape(18.dp)
    val borde = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val colores = CardDefaults.cardColors(containerColor = containerColor)
    val elevacion = CardDefaults.cardElevation(defaultElevation = 0.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = forma,
            colors = colores,
            border = borde,
            elevation = elevacion,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = forma,
            colors = colores,
            border = borde,
            elevation = elevacion,
            content = content
        )
    }
}

/** Etiqueta pequeña redondeada (categoría, "Revisar", etc.). */
@Composable
fun Pastilla(texto: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
