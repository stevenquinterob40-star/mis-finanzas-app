package com.martin.misfinanzas.data

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions = dao.getAll()
    val pendientesRevision = dao.getPendientesRevision()
    val automaticas = dao.getAutomaticas()

    fun balancePorOrigen(origen: Origen) = dao.getBalancePorOrigen(origen)

    suspend fun insert(transaction: Transaction) = dao.insert(transaction)
    suspend fun update(transaction: Transaction) = dao.update(transaction)
    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
}
