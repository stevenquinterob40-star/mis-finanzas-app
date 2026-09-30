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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class ComparacionExtracto(
    val coinciden: Int,
    val faltan: List<MovimientoExtracto>,
    val sobran: List<Transaction>,
    val limpiadosAutomaticamente: Int = 0
)

private fun mismoDia(millisA: Long, millisB: Long): Boolean {
    val zona = ZoneId.systemDefault()
    val diaA = Instant.ofEpochMilli(millisA).atZone(zona).toLocalDate()
    val diaB = Instant.ofEpochMilli(millisB).atZone(zona).toLocalDate()
    return diaA == diaB
}

private fun montosCercanos(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) < 1.0

// Las transacciones automáticas se guardan con la hora en que llegó la
// notificación al celular, no la fecha real del movimiento en Nequi. Si el
// celular estuvo apagado, sin señal o la notificación llegó tarde, puede
// caer en un día distinto al del extracto. Por eso se compara con un margen
// de 1 día en vez de exigir el mismo día calendario exacto.
private fun diasCercanos(millisA: Long, millisB: Long): Boolean =
    kotlin.math.abs(millisA - millisB) <= 36 * 60 * 60 * 1000L // 36 horas

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

    val balanceDigital = repository.balancePorOrigen(Origen.DIGITAL)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balanceEfectivo = repository.balancePorOrigen(Origen.EFECTIVO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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

    /**
     * Compara los movimientos leídos de un extracto (CSV o PDF) contra lo que ya
     * hay en la app. Antes de mostrar la pantalla de comparación, primero
     * LIMPIA SOLA los duplicados obvios: si una transacción importada
     * automáticamente (esAutomatica = true) tiene una copia exacta (mismo
     * tipo, monto y día) que ya quedó emparejada con un movimiento real del
     * extracto, esa copia sobrante es un error de importación sin ambigüedad
     * — no hace falta que el usuario la revise ni la borre a mano, se elimina
     * directamente. Lo que queda en "sobran" es solo lo que sí necesita ojo
     * humano (no tiene una pareja exacta ya confirmada).
     *
     * Empareja por (tipo, monto parecido, día ±36h). Con eso arma:
     * - coinciden: ya estaban registrados, no hace falta tocarlos.
     * - faltan: están en el extracto pero no en la app (se pueden agregar).
     * - sobran: están en la app (en el rango de fechas del extracto) pero NO
     *   aparecen en el extracto real y no son duplicados obvios — hay que
     *   revisarlos a mano.
     * - limpiadosAutomaticamente: cuántos duplicados se borraron solos.
     */
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
                        diasCercanos(t.fecha, mov.fecha)
                }
                if (match != null) {
                    idsEmparejados.add(match.id)
                } else {
                    faltan.add(mov)
                }
            }

            val fechaMin = movimientos.minOfOrNull { it.fecha }
            val fechaMax = movimientos.maxOfOrNull { it.fecha }
            val margenUnDia = 86_400_000L

            val candidatosSobran = if (fechaMin != null && fechaMax != null) {
                existentesDigital.filter { t ->
                    t.id !in idsEmparejados && t.fecha in (fechaMin - margenUnDia)..(fechaMax + margenUnDia)
                }
            } else {
                emptyList()
            }

            // De los candidatos a "sobran", separa los que son duplicados
            // exactos y automáticos de algo ya emparejado: esos se borran
            // solos. El resto sigue necesitando revisión manual.
            val autoLimpiables = candidatosSobran.filter { t ->
                t.esAutomatica && existentesDigital.any { emparejada ->
                    emparejada.id in idsEmparejados &&
                        emparejada.tipo == t.tipo &&
                        montosCercanos(emparejada.monto, t.monto) &&
                        diasCercanos(emparejada.fecha, t.fecha)
                }
            }
            val sobranReales = candidatosSobran - autoLimpiables.toSet()

            for (duplicado in autoLimpiables) {
                repository.delete(duplicado)
            }

            _comparacion.value = ComparacionExtracto(
                coinciden = idsEmparejados.size,
                faltan = faltan,
                sobran = sobranReales,
                limpiadosAutomaticamente = autoLimpiables.size
            )
        }
    }

    /**
     * Borra un movimiento "sobrante" Y actualiza la comparación en pantalla al
     * toque — antes se borraba de la base de datos pero la lista de sobrantes
     * seguía mostrando el mismo movimiento, dando la sensación de que la app
     * se había quedado pegada.
     */
    fun eliminarSobrante(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
            val actual = _comparacion.value
            if (actual != null) {
                _comparacion.value = actual.copy(sobran = actual.sobran.filter { it.id != transaction.id })
            }
        }
    }

    /**
     * Agrega todos los movimientos que faltan (y su contraparte en Efectivo si
     * son retiros). Se queda en la pantalla de comparación (igual que
     * eliminarSobrante) en vez de cerrarla, para poder seguir revisando los
     * "sobran" sin perder el contexto.
     */
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
                        categoria = mov.categoria
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
                            categoria = "Retiro"
                        )
                    )
                }
            }
            val actual = _comparacion.value
            if (actual != null) {
                _comparacion.value = actual.copy(
                    coinciden = actual.coinciden + faltan.size,
                    faltan = actual.faltan.filter { it !in faltan }
                )
            }
        }
    }
}
