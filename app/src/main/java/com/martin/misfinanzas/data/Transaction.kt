package com.martin.misfinanzas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoMovimiento {
    INGRESO,
    GASTO
}

enum class Origen {
    DIGITAL,
    EFECTIVO
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monto: Double,
    val tipo: TipoMovimiento,
    val origen: Origen,
    val descripcion: String,
    val fecha: Long,
    val esAutomatica: Boolean = false,
    val necesitaRevision: Boolean = false,
    val notificacionCruda: String? = null,
    val categoria: String = "Sin categoría",
    val entidad: String = "Nequi"
)
