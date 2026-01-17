package com.mrayushbajpai.workworth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class WorkWorthUiState(
    val salary: Double = 0.0,
    val daysWorked: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val labels: List<Label> = emptyList(),
    val currentMonthYear: String = "",
    val isLoading: Boolean = true,
    val editingTransaction: Transaction? = null,
    val editingLabel: Label? = null
) {
    val currentMonthTransactions = transactions.filter { it.monthYear == currentMonthYear }
    val totalSpent = currentMonthTransactions.sumOf { it.amount }
    val remainingMoney = salary - totalSpent
    val moneyDaysLeft = FinancialEngine.calculateRemainingDays(remainingMoney, salary, daysWorked)
    
    val calendarDaysLeft: Int by lazy {
        val today = LocalDate.now()
        val lastDayOfMonth = YearMonth.from(today).atEndOfMonth()
        java.time.temporal.ChronoUnit.DAYS.between(today, lastDayOfMonth).toInt()
    }
}

class MainViewModel(private val repository: WorkworthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkWorthUiState())
    val uiState: StateFlow<WorkWorthUiState> = _uiState.asStateFlow()

    init {
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        
        combine(
            repository.salary,
            repository.daysWorked,
            repository.transactions,
            repository.labels
        ) { salary, days, transactions, labels ->
            _uiState.value.copy(
                salary = salary,
                daysWorked = days,
                transactions = transactions,
                labels = labels,
                currentMonthYear = currentMonth,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun updateSettings(salary: Double, daysWorked: Double) {
        if (salary <= 0 || daysWorked <= 0) return
        
        viewModelScope.launch {
            val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            repository.updateSettings(salary, daysWorked, currentMonth)
        }
    }

    fun startEditingTransaction(transaction: Transaction) {
        _uiState.update { it.copy(editingTransaction = transaction) }
    }

    fun cancelEditingTransaction() {
        _uiState.update { it.copy(editingTransaction = null) }
    }

    fun addOrUpdateTransaction(name: String, amount: Double, selectedLabelIds: List<String>) {
        val state = uiState.value
        if (state.salary <= 0 || state.daysWorked <= 0 || amount <= 0 || name.isBlank()) return

        val timeCost = FinancialEngine.calculateTimeCost(amount, state.salary, state.daysWorked)
        
        viewModelScope.launch {
            val editing = state.editingTransaction
            if (editing != null) {
                val updatedTransaction = editing.copy(
                    name = name,
                    amount = amount,
                    timeCost = timeCost,
                    labelIds = selectedLabelIds
                )
                repository.updateTransaction(updatedTransaction)
                cancelEditingTransaction()
            } else {
                val transaction = Transaction(
                    name = name,
                    amount = amount,
                    timeCost = timeCost,
                    monthYear = state.currentMonthYear,
                    labelIds = selectedLabelIds
                )
                repository.saveTransaction(transaction)
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun startEditingLabel(label: Label) {
        _uiState.update { it.copy(editingLabel = label) }
    }

    fun cancelEditingLabel() {
        _uiState.update { it.copy(editingLabel = null) }
    }

    fun addOrUpdateLabel(name: String, color: Int) {
        if (name.isBlank()) return
        val state = uiState.value
        
        viewModelScope.launch {
            val editing = state.editingLabel
            if (editing != null) {
                val newLabel = Label.create(name, color)
                repository.updateLabel(editing.id, newLabel)
                cancelEditingLabel()
            } else {
                repository.saveLabel(name, color)
            }
        }
    }

    fun deleteLabel(labelId: String) {
        viewModelScope.launch {
            repository.deleteLabel(labelId)
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
