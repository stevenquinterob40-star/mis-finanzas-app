package com.martin.misfinanzas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY fecha DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE necesitaRevision = 1 ORDER BY fecha DESC")
    fun getPendientesRevision(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE esAutomatica = 1 ORDER BY fecha DESC")
    fun getAutomaticas(): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE -monto END), 0)
        FROM transactions
        WHERE origen = :origen AND necesitaRevision = 0
        """
    )
    fun getBalancePorOrigen(origen: Origen): Flow<Double>
}
