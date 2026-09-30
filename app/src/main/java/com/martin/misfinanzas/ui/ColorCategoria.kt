package com.martin.misfinanzas.ui

import androidx.compose.ui.graphics.Color

private val PALETA_CATEGORIAS = listOf(
    Color(0xFFE07A5F),
    Color(0xFF3D5A80),
    Color(0xFF81B29A),
    Color(0xFFDDA448),
    Color(0xFF9B5DE5),
    Color(0xFFF15BB5),
    Color(0xFF0098C9),
    Color(0xFF06A77D),
    Color(0xFFEF476F),
    Color(0xFF118AB2),
    Color(0xFF7F5539),
    Color(0xFF5C677D),
)

fun colorParaCategoria(categoria: String): Color {
    val indice = categoria.trim().lowercase().hashCode().let { if (it < 0) -it else it } % PALETA_CATEGORIAS.size
    return PALETA_CATEGORIAS[indice]
}
