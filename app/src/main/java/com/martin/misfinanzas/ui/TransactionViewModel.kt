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

data class ComparacionExtracto(
    val coinciden: Int,
    val faltan: List<MovimientoExtracto>,
    val sobran: List<Transaction>,
    val limpiadosAutomaticamente: Int = 0
)

private fun montosCercanos(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) < 1.0
private fun diasCercanos(millisA: Long, millisB: Long): Boolean = kotlin.math.abs(millisA - millisB) <= 36 * 60 * 60 * 1000L

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    init {
        val dao = AppDatabase.getInstance(application).transactionDao()
        repository = TransactionRepository(dao)
    }

    val transacciones = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendientesRevision = repository.pendientesRevision.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val automaticas = repository.automaticas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val balanceEfectivo = repository.balancePorOrigen(Origen.EFECTIVO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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

    fun agregar(transaction: Transaction) = viewModelScope.launch { repository.insert(transaction) }
    fun actualizar(transaction: Transaction) = viewModelScope.launch { repository.update(transaction) }
    fun eliminar(transaction: Transaction) = viewModelScope.launch { repository.delete(transaction) }

    private val _comparacion = MutableStateFlow<ComparacionExtracto?>(null)
    val comparacion: StateFlow<ComparacionExtracto?> = _comparacion

    fun limpiarComparacion() { _comparacion.value = null }

    fun prepararComparacion(movimientos: List<MovimientoExtracto>) {
        viewModelScope.launch {
            val existentesDigital = transacciones.value.filter { it.origen == Origen.DIGITAL }
            val idsEmparejados = mutableSetOf<Long>()
            val faltan = mutableListOf<MovimientoExtracto>()

            for (mov in movimientos) {
                val match = existentesDigital.firstOrNull { t ->
                    t.id !in idsEmparejados && t.tipo == mov.tipo &&
                        montosCercanos(t.monto, mov.monto) && diasCercanos(t.fecha, mov.fecha)
                }
                if (match != null) idsEmparejados.add(match.id) else faltan.add(mov)
            }

            val fechaMin = movimientos.minOfOrNull { it.fecha }
            val fechaMax = movimientos.maxOfOrNull { it.fecha }
            val margenUnDia = 86_400_000L

            val candidatosSobran = if (fechaMin != null && fechaMax != null) {
                existentesDigital.filter { t -> t.id !in idsEmparejados && t.fecha in (fechaMin - margenUnDia)..(fechaMax + margenUnDia) }
            } else emptyList()

            val autoLimpiables = candidatosSobran.filter { t ->
                t.esAutomatica && existentesDigital.any { emparejada ->
                    emparejada.id in idsEmparejados && emparejada.tipo == t.tipo &&
                        montosCercanos(emparejada.monto, t.monto) && diasCercanos(emparejada.fecha, t.fecha)
                }
            }
            val sobranReales = candidatosSobran - autoLimpiables.toSet()

            for (duplicado in autoLimpiables) repository.delete(duplicado)

            _comparacion.value = ComparacionExtracto(idsEmparejados.size, faltan, sobranReales, autoLimpiables.size)
        }
    }

    fun eliminarSobrante(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
            val actual = _comparacion.value
            if (actual != null) _comparacion.value = actual.copy(sobran = actual.sobran.filter { it.id != transaction.id })
        }
    }

    fun agregarFaltantes(faltan: List<MovimientoExtracto>) {
        viewModelScope.launch {
            for (mov in faltan) {
                repository.insert(Transaction(
                    monto = mov.monto, tipo = mov.tipo, origen = Origen.DIGITAL,
                    descripcion = mov.descripcion, fecha = mov.fecha, esAutomatica = true,
                    notificacionCruda = "Importado: ${mov.lineaCruda}", categoria = mov.categoria, entidad = "Nequi"
                ))
                if (mov.esRetiro) {
                    repository.insert(Transaction(
                        monto = mov.monto, tipo = TipoMovimiento.INGRESO, origen = Origen.EFECTIVO,
                        descripcion = "Retiro cajero", fecha = mov.fecha, esAutomatica = true,
                        notificacionCruda = "Importado: ${mov.lineaCruda}", categoria = "Retiro", entidad = "Nequi"
                    ))
                }
            }
            val actual = _comparacion.value
            if (actual != null) _comparacion.value = actual.copy(
                coinciden = actual.coinciden + faltan.size, faltan = actual.faltan.filter { it !in faltan }
            )
        }
    }
}
