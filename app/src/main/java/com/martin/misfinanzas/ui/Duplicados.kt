package com.martin.misfinanzas.ui

import com.martin.misfinanzas.data.Transaction

/** Un grupo de movimientos repetidos: se conserva uno y los demás sobran. */
data class GrupoDuplicados(
    val conservar: Transaction,
    val sobran: List<Transaction>
)

/**
 * Dos movimientos se consideran duplicados si tienen el mismo tipo, origen, entidad,
 * monto y descripción, y fueron registrados con menos de 10 minutos de diferencia
 * (por ejemplo, una notificación que llegó dos veces).
 * Los que están pendientes de revisión no se tocan.
 */
private const val VENTANA_DUPLICADO_MS = 10 * 60 * 1000L

private fun normalizar(texto: String): String =
    texto.trim().lowercase().replace(Regex("\\s+"), " ")

private fun cerrarGrupo(candidatos: List<Transaction>): GrupoDuplicados? {
    if (candidatos.size < 2) return null
    // Se prefiere conservar el que el usuario ya confirmó a mano; si no, el más antiguo.
    val conservar = candidatos.firstOrNull { !it.esAutomatica } ?: candidatos.first()
    return GrupoDuplicados(
        conservar = conservar,
        sobran = candidatos.filter { it.id != conservar.id }
    )
}

fun List<Transaction>.encontrarDuplicados(): List<GrupoDuplicados> {
    val resultado = mutableListOf<GrupoDuplicados>()

    val porClave = this
        .filter { !it.necesitaRevision }
        .groupBy { t ->
            listOf(
                t.tipo,
                t.origen,
                t.entidad.trim().lowercase(),
                Math.round(t.monto * 100),
                normalizar(t.descripcion)
            )
        }

    for (mismos in porClave.values) {
        if (mismos.size < 2) continue
        val ordenados = mismos.sortedBy { it.fecha }
        var actual = mutableListOf(ordenados.first())
        for (t in ordenados.drop(1)) {
            if (t.fecha - actual.first().fecha <= VENTANA_DUPLICADO_MS) {
                actual.add(t)
            } else {
                cerrarGrupo(actual)?.let { resultado.add(it) }
                actual = mutableListOf(t)
            }
        }
        cerrarGrupo(actual)?.let { resultado.add(it) }
    }

    return resultado.sortedByDescending { it.conservar.fecha }
}
