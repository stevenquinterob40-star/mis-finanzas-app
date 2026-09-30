package com.martin.misfinanzas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.martin.misfinanzas.data.AppDatabase
import com.martin.misfinanzas.data.MovimientoExtracto
import com.martin.misfinanzas.data.Origen
import com.martin.misfinanzas.data.TipoMovimiento
import com.martin.misfinanzas.data.Transaction
import com.martin.misfinanzas.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ComparacionExtracto(
    val coinciden: Int,
    val faltan: List<MovimientoExtracto>,
    val sobran: List<Transaction>,
    val limpiadosAutomaticamente: Int = 0
)

private fun montosCercanos(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) < 1.0

private fun mismoDia(millisA: Long, millisB: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = millisA }
    val calB = Calendar.getInstance().apply { timeInMillis = millisB }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    init {
        val dao = AppDatabase.getInstance(application).transactionDao()
        repository = TransactionRepository(dao)
    }

    val transacciones = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendientesRevision = repository.pendientesRevision
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automaticas = repository.automaticas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val balanceEfectivo = repository.balancePorOrigen(Origen.EFECTIVO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balanceNequi = transacciones.map { lista ->
        lista.filter { it.origen == Origen.DIGITAL && it.entidad.equals("Nequi", ignoreCase = true) && !it.necesitaRevision }
            .sumOf { if (it.tipo == TipoMovimiento.INGRESO) it.monto else -it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balanceBancolombia = transacciones.map { lista ->
        lista.filter { it.origen == Origen.DIGITAL && it.entidad.equals("Bancolombia", ignoreCase = true) && !it.necesitaRevision }
            .sumOf { if (it.tipo == TipoMovimiento.INGRESO) it.monto else -it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balanceNu = transacciones.map { lista ->
        lista.filter { it.origen == Origen.DIGITAL && it.entidad.equals("Nu", ignoreCase = true) && !it.necesitaRevision }
            .sumOf { if (it.tipo == TipoMovimiento.INGRESO) it.monto else -it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun agregar(transaction: Transaction) {
        viewModelScope.launch { repository.insert(transaction) }
    }

    fun actualizar(transaction: Transaction) {
        viewModelScope.launch { repository.update(transaction) }
    }

    fun eliminar(transaction: Transaction) {
        viewModelScope.launch { repository.delete(transaction) }
    }

    private val _comparacion = MutableStateFlow<ComparacionExtracto?>(null)
    val comparacion: StateFlow<ComparacionExtracto?> = _comparacion

    fun limpiarComparacion() {
        _comparacion.value = null
    }

    fun prepararComparacion(movimientos: List<MovimientoExtracto>) {
        viewModelScope.launch {
            val existentesDigital = transacciones.value.filter { it.origen == Origen.DIGITAL }
            val idsEmparejados = mutableSetOf<Long>()
            val faltan = mutableListOf<MovimientoExtracto>()

            for (mov in movimientos) {
                val match = existentesDigital.firstOrNull { t ->
                    t.id !in idsEmparejados &&
                        t.tipo == mov.tipo &&
                        montosCercanos(t.monto, mov.monto) &&
                        mismoDia(t.fecha, mov.fecha)
                }
                if (match != null) {
                    idsEmparejados.add(match.id)
                } else {
                    faltan.add(mov)
                }
            }

            _comparacion.value = ComparacionExtracto(
                coinciden = idsEmparejados.size,
                faltan = faltan,
                sobran = emptyList(),
                limpiadosAutomaticamente = 0
            )
        }
    }

    fun eliminarSobrante(transaction: Transaction) {
        viewModelScope.launch { repository.delete(transaction) }
    }

    fun agregarFaltantes(faltan: List<MovimientoExtracto>) {
        viewModelScope.launch {
            for (mov in faltan) {
                repository.insert(
                    Transaction(
                        monto = mov.monto,
                        tipo = mov.tipo,
                        origen = Origen.DIGITAL,
                        descripcion = mov.descripcion,
                        fecha = mov.fecha,
                        esAutomatica = true,
                        necesitaRevision = false,
                        notificacionCruda = "Importado del extracto: ${mov.lineaCruda}",
                        categoria = mov.categoria,
                        entidad = "Nequi"
                    )
                )
                if (mov.esRetiro) {
                    repository.insert(
                        Transaction(
                            monto = mov.monto,
                            tipo = TipoMovimiento.INGRESO,
                            origen = Origen.EFECTIVO,
                            descripcion = "Efectivo retirado en cajero",
                            fecha = mov.fecha,
                            esAutomatica = true,
                            necesitaRevision = false,
                            notificacionCruda = "Importado del extracto: ${mov.lineaCruda}",
                            categoria = "Retiro",
                            entidad = "Nequi"
                        )
                    )
                }
            }
            limpiarDuplicadosExactos()
            _comparacion.value = null
        }
    }

    fun limpiarDuplicadosExactos() {
        viewModelScope.launch {
            val todas = transacciones.value
            val unicas = mutableSetOf<String>()
            val duplicadas = mutableListOf<Transaction>()

            for (t in todas) {
                // Firma inteligente basada en valor, descripción y el mismo día calendario
                val cal = Calendar.getInstance().apply { timeInMillis = t.fecha }
                val fechaKey = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
                val firma = "${t.monto}_${t.descripcion.trim().lowercase()}_${fechaKey}_${t.tipo}_${t.entidad}"
                
                if (unicas.contains(firma)) {
                    duplicadas.add(t)
                } else {
                    unicas.add(firma)
                }
            }

            for (t in duplicadas) {
                repository.delete(t)
            }
        }
    }
}
