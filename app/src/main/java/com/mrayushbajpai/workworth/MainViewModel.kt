package com.mrayushbajpai.workworth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class WorkWorthUiState(
    val salary: Double = 0.0,
    val daysWorked: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val labels: List<Label> = emptyList(),
    val monthlySummaries: Map<String, MonthlySummary> = emptyMap(),
    val currentMonthYear: String = "",
    val debugMonthOffset: Int = 0,
    val isLoading: Boolean = true,
    val editingTransaction: Transaction? = null,
    val editingLabel: Label? = null,
    val transactionToDelete: Transaction? = null,
    val labelToDelete: Label? = null
) {
    val currentMonthTransactions = transactions.filter { it.monthYear == currentMonthYear }
    val totalSpent = currentMonthTransactions.sumOf { it.amount }
    val remainingMoney = salary - totalSpent
    val moneyDaysLeft = FinancialEngine.calculateRemainingDays(remainingMoney, salary, daysWorked)
    
    val calendarDaysLeft: Int by lazy {
        val today = LocalDate.now().plusMonths(debugMonthOffset.toLong())
        val lastDayOfMonth = YearMonth.from(today).atEndOfMonth()
        java.time.temporal.ChronoUnit.DAYS.between(today, lastDayOfMonth).toInt()
    }
}

class MainViewModel(private val repository: WorkworthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkWorthUiState())
    val uiState: StateFlow<WorkWorthUiState> = _uiState.asStateFlow()

    init {
        combine(
            repository.salary,
            repository.daysWorked,
            repository.transactions,
            repository.labels,
            repository.debugMonthOffset,
            repository.monthlySummaries
        ) { args ->
            val salary = args[0] as Double
            val days = args[1] as Double
            @Suppress("UNCHECKED_CAST")
            val transactions = args[2] as List<Transaction>
            @Suppress("UNCHECKED_CAST")
            val labels = args[3] as List<Label>
            val offset = args[4] as Int
            @Suppress("UNCHECKED_CAST")
            val summaries = args[5] as Map<String, MonthlySummary>

            val today = LocalDate.now().plusMonths(offset.toLong())
            val currentMonth = today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            
            WorkWorthUiState(
                salary = salary,
                daysWorked = days,
                transactions = transactions,
                labels = labels,
                currentMonthYear = currentMonth,
                debugMonthOffset = offset,
                monthlySummaries = summaries,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun updateSettings(salary: Double, daysWorked: Double) {
        if (salary <= 0 || daysWorked <= 0) return
        
        viewModelScope.launch {
            val today = LocalDate.now().plusMonths(uiState.value.debugMonthOffset.toLong())
            val currentMonth = today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
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

    fun confirmDeleteTransaction(transaction: Transaction) {
        _uiState.update { it.copy(transactionToDelete = transaction) }
    }

    fun dismissDeleteTransaction() {
        _uiState.update { it.copy(transactionToDelete = null) }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            dismissDeleteTransaction()
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
        
        viewModelScope.launch {
            val editing = uiState.value.editingLabel
            if (editing != null) {
                val newLabel = Label.create(name, color)
                repository.updateLabel(editing.id, newLabel)
                cancelEditingLabel()
            } else {
                repository.saveLabel(name, color)
            }
        }
    }

    fun confirmDeleteLabel(label: Label) {
        _uiState.update { it.copy(labelToDelete = label) }
    }

    fun dismissDeleteLabel() {
        _uiState.update { it.copy(labelToDelete = null) }
    }

    fun deleteLabel(labelId: String) {
        viewModelScope.launch {
            repository.deleteLabel(labelId)
            dismissDeleteLabel()
        }
    }

    fun resetCurrentMonth() {
        viewModelScope.launch {
            repository.resetCurrentMonth()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun fastForwardTime() {
        viewModelScope.launch {
            repository.updateDebugMonthOffset(uiState.value.debugMonthOffset + 1)
        }
    }

    fun seedMockData() {
        viewModelScope.launch {
            val mockLabels = listOf(
                Label.create("Groceries", 0xFF4CAF50.toInt()),
                Label.create("Rent", 0xFF2196F3.toInt()),
                Label.create("Tech", 0xFF9C27B0.toInt()),
                Label.create("Dining", 0xFFFF9800.toInt()),
                Label.create("Travel", 0xFF00BCD4.toInt()),
                Label.create("Health", 0xFFE91E63.toInt()),
                Label.create("Subscription", 0xFF607D8B.toInt())
            )
            
            // Save labels first
            mockLabels.forEach { repository.saveLabel(it.name, it.color) }
            
            val labelIds = mockLabels.map { it.id }
            val transactions = mutableListOf<Transaction>()
            val summaries = mutableMapOf<String, MonthlySummary>()
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
            val today = LocalDate.now().plusMonths(uiState.value.debugMonthOffset.toLong())
            
            val mockSalary = 6000.0 // Higher salary to ensure positive balance
            val mockDaysWorked = 22.0
            
            // Generate data for current month + last 2 months
            for (i in 0..2) {
                val date = today.minusMonths(i.toLong())
                val monthYear = date.format(formatter)
                
                // Add summary for this month
                summaries[monthYear] = MonthlySummary(mockSalary, mockDaysWorked)
                
                // Add ~16 transactions per month
                repeat(16) {
                    val amount = Random.nextDouble(20.0, 300.0)
                    val timeCost = FinancialEngine.calculateTimeCost(amount, mockSalary, mockDaysWorked)
                    
                    transactions.add(
                        Transaction(
                            name = listOf("Coffee", "Netflix", "Amazon", "Groceries", "Gas", "Dinner", "Gym", "Internet", "Water Bill", "Electricity").random(),
                            amount = amount,
                            timeCost = timeCost,
                            monthYear = monthYear,
                            labelIds = listOf(labelIds.random())
                        )
                    )
                }
            }
            
            // Save everything to repository
            repository.saveSeedData(uiState.value.transactions + transactions, summaries)
            
            // Also update the active current month settings
            repository.updateSettings(mockSalary, mockDaysWorked, today.format(formatter))
        }
    }
}
