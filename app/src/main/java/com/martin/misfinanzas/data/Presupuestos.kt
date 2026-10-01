package com.martin.misfinanzas.data

import android.content.Context

/**
 * Presupuesto mensual por categoría de gasto. Se guarda en SharedPreferences
 * (categoría -> monto límite), igual que las categorías personalizadas.
 */
object Presupuestos {
    private const val PREFS = "presupuestos_prefs"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun obtener(context: Context): Map<String, Double> =
        prefs(context).all.mapNotNull { (categoria, valor) ->
            val monto = (valor as? String)?.toDoubleOrNull()
            if (monto != null && monto > 0) categoria to monto else null
        }.toMap()

    fun guardar(context: Context, categoria: String, monto: Double) {
        val limpia = categoria.trim()
        if (limpia.isBlank() || monto <= 0) return
        prefs(context).edit().putString(limpia, monto.toString()).apply()
    }

    fun eliminar(context: Context, categoria: String) {
        prefs(context).edit().remove(categoria).apply()
    }
}
