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
    val isLoading: Boolean = true
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
            WorkWorthUiState(
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
        // Validation Layer
        if (salary <= 0 || daysWorked <= 0) return
        
        viewModelScope.launch {
            val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            repository.updateSettings(salary, daysWorked, currentMonth)
        }
    }

    fun addTransaction(name: String, amount: Double, selectedLabelIds: List<String>) {
        val state = uiState.value
        // Validation Layer
        if (state.salary <= 0 || state.daysWorked <= 0 || amount <= 0 || name.isBlank()) return

        val timeCost = FinancialEngine.calculateTimeCost(amount, state.salary, state.daysWorked)
        
        viewModelScope.launch {
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

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun addLabel(name: String, color: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveLabel(name, color)
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
