package com.mrayushbajpai.workworth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

class WorkworthRepository(private val settingsManager: SettingsManager) {

    val transactions: Flow<List<Transaction>> = settingsManager.transactionsFlow
    val labels: Flow<List<Label>> = settingsManager.labelsFlow
    val salary: Flow<Double> = settingsManager.monthlySalaryFlow
    val daysWorked: Flow<Double> = settingsManager.daysWorkedFlow

    suspend fun saveTransaction(transaction: Transaction) {
        val current = settingsManager.transactionsFlow.firstOrNull() ?: emptyList()
        settingsManager.saveTransactions(current + transaction)
    }

    suspend fun deleteTransaction(transactionId: String) {
        val current = settingsManager.transactionsFlow.firstOrNull() ?: emptyList()
        settingsManager.saveTransactions(current.filter { it.id != transactionId })
    }

    suspend fun saveLabel(name: String, color: Int) {
        val newLabel = Label.create(name, color)
        val current = settingsManager.labelsFlow.firstOrNull() ?: emptyList()
        if (current.none { it.id == newLabel.id }) {
            settingsManager.saveLabels(current + newLabel)
        }
    }

    suspend fun deleteLabel(labelId: String) {
        val current = settingsManager.labelsFlow.firstOrNull() ?: emptyList()
        settingsManager.saveLabels(current.filter { it.id != labelId })
    }

    suspend fun updateSettings(salary: Double, daysWorked: Double, monthYear: String) {
        settingsManager.saveSettings(salary, daysWorked, monthYear)
    }

    suspend fun clearAll() {
        settingsManager.clearSettings()
    }

    // Single source of truth for transaction labels mapping
    fun getLabelsForTransaction(transaction: Transaction, allLabels: List<Label>): List<Label> {
        return transaction.labelIds.mapNotNull { id ->
            allLabels.find { it.id == id }
        }
    }
}
