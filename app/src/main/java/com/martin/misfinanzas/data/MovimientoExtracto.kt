package com.martin.misfinanzas.data

/**
 * Un movimiento leído de un extracto externo (CSV o PDF), antes de compararlo
 * contra lo que ya existe en la app.
 */
data class MovimientoExtracto(
    val fecha: Long,
    val descripcion: String,
    val tipo: TipoMovimiento,
    val monto: Double,
    val categoria: String,
    val esRetiro: Boolean,
    val lineaCruda: String
)
