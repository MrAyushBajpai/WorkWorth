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

    suspend fun updateTransaction(transaction: Transaction) {
        val current = settingsManager.transactionsFlow.firstOrNull() ?: emptyList()
        val updated = current.map { if (it.id == transaction.id) transaction else it }
        settingsManager.saveTransactions(updated)
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

    suspend fun updateLabel(oldId: String, newLabel: Label) {
        val currentLabels = settingsManager.labelsFlow.firstOrNull() ?: emptyList()
        val currentTransactions = settingsManager.transactionsFlow.firstOrNull() ?: emptyList()

        // Update Labels list
        val updatedLabels = currentLabels.map { if (it.id == oldId) newLabel else it }

        // If ID changed (renaming), migrate references in Transactions
        val finalTransactions = if (oldId != newLabel.id) {
            currentTransactions.map { transaction ->
                if (transaction.labelIds.contains(oldId)) {
                    transaction.copy(
                        labelIds = transaction.labelIds.map { if (it == oldId) newLabel.id else it }
                    )
                } else {
                    transaction
                }
            }
        } else {
            currentTransactions
        }

        // Save both in one DataStore transaction for atomicity
        settingsManager.updateLabelsAndTransactions(updatedLabels, finalTransactions)
    }

    suspend fun deleteLabel(labelId: String) {
        val currentLabels = settingsManager.labelsFlow.firstOrNull() ?: emptyList()
        val currentTransactions = settingsManager.transactionsFlow.firstOrNull() ?: emptyList()

        val updatedLabels = currentLabels.filter { it.id != labelId }
        
        // Also remove references from transactions
        val updatedTransactions = currentTransactions.map { transaction ->
            if (transaction.labelIds.contains(labelId)) {
                transaction.copy(labelIds = transaction.labelIds.filter { it != labelId })
            } else {
                transaction
            }
        }
        
        settingsManager.updateLabelsAndTransactions(updatedLabels, updatedTransactions)
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
