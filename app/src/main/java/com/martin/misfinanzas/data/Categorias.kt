package com.martin.misfinanzas.data

import android.content.Context

object Categorias {
    val SUGERIDAS = listOf(
        "Comida",
        "Transporte",
        "Servicios",
        "Entretenimiento",
        "Salud",
        "Educación",
        "Compras",
        "Vivienda",
        "Ahorros",
        "Sueldo/Ingresos",
        "Retiro",
        "Otros"
    )

    const val SIN_CATEGORIA = "Sin categoría"
}

object CategoriasPersonalizadas {
    private const val PREFS = "categorias_prefs"
    private const val KEY = "personalizadas"

    fun obtener(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY, emptySet()) ?: emptySet()
    }

    fun agregar(context: Context, categoria: String) {
        val limpia = categoria.trim()
        if (limpia.isBlank() || limpia == Categorias.SIN_CATEGORIA || Categorias.SUGERIDAS.contains(limpia)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val actuales = (prefs.getStringSet(KEY, emptySet()) ?: emptySet()).toMutableSet()
        if (actuales.add(limpia)) {
            prefs.edit().putStringSet(KEY, actuales).apply()
        }
    }
}

/**
 * Lleva la cuenta de qué tanto y qué tan recientemente se usa cada categoría,
 * para que las más usadas / más recientes aparezcan primero en los chips.
 */
object UsoCategorias {
    private const val PREFS = "uso_categorias_prefs"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun registrarUso(context: Context, categoria: String) {
        if (categoria.isBlank() || categoria == Categorias.SIN_CATEGORIA) return
        val p = prefs(context)
        val conteoActual = p.getInt("conteo_$categoria", 0)
        p.edit()
            .putInt("conteo_$categoria", conteoActual + 1)
            .putLong("ultimoUso_$categoria", System.currentTimeMillis())
            .apply()
    }

    /**
     * Ordena una lista de categorías: primero las más usadas, y entre las que
     * tienen el mismo número de usos, la que se usó más recientemente. Las
     * que nunca se han usado quedan al final, en su orden original.
     */
    fun ordenarPorUso(context: Context, categorias: List<String>): List<String> {
        val p = prefs(context)
        return categorias.sortedWith(
            compareByDescending<String> { p.getInt("conteo_$it", 0) }
                .thenByDescending { p.getLong("ultimoUso_$it", 0L) }
        )
    }
}
