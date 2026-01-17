package com.mrayushbajpai.workworth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Serializable
data class MonthlySummary(
    val salary: Double,
    val daysWorked: Double
)

class SettingsManager(private val context: Context) {
    companion object {
        val MONTHLY_SALARY = doublePreferencesKey("monthly_salary")
        val DAYS_WORKED = doublePreferencesKey("days_worked")
        val SAVED_MONTH = stringPreferencesKey("saved_month")
        val TRANSACTIONS = stringPreferencesKey("transactions")
        val MONTHLY_SUMMARIES = stringPreferencesKey("monthly_summaries")
        val LABELS = stringPreferencesKey("labels")
        val DEBUG_MONTH_OFFSET = intPreferencesKey("debug_month_offset")
    }

    val monthlySalaryFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[MONTHLY_SALARY] ?: 0.0
        }

    val daysWorkedFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[DAYS_WORKED] ?: 0.0
        }

    val savedMonthFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SAVED_MONTH]
        }

    val transactionsFlow: Flow<List<Transaction>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[TRANSACTIONS] ?: "[]"
            try {
                Json.decodeFromString<List<Transaction>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }

    val monthlySummariesFlow: Flow<Map<String, MonthlySummary>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[MONTHLY_SUMMARIES] ?: "{}"
            try {
                Json.decodeFromString<Map<String, MonthlySummary>>(jsonString)
            } catch (e: Exception) {
                emptyMap()
            }
        }

    val labelsFlow: Flow<List<Label>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[LABELS] ?: "[]"
            try {
                Json.decodeFromString<List<Label>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }

    val debugMonthOffsetFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DEBUG_MONTH_OFFSET] ?: 0
        }

    suspend fun saveSettings(salary: Double, days: Double, monthYear: String) {
        context.dataStore.edit { preferences ->
            preferences[MONTHLY_SALARY] = salary
            preferences[DAYS_WORKED] = days
            preferences[SAVED_MONTH] = monthYear

            // Update monthly history records
            val currentSummariesJson = preferences[MONTHLY_SUMMARIES] ?: "{}"
            val currentSummaries = try {
                Json.decodeFromString<Map<String, MonthlySummary>>(currentSummariesJson).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
            currentSummaries[monthYear] = MonthlySummary(salary, days)
            preferences[MONTHLY_SUMMARIES] = Json.encodeToString(currentSummaries)
        }
    }

    suspend fun saveMonthlySummaries(summaries: Map<String, MonthlySummary>) {
        context.dataStore.edit { preferences ->
            val currentSummariesJson = preferences[MONTHLY_SUMMARIES] ?: "{}"
            val currentSummaries = try {
                Json.decodeFromString<Map<String, MonthlySummary>>(currentSummariesJson).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
            currentSummaries.putAll(summaries)
            preferences[MONTHLY_SUMMARIES] = Json.encodeToString(currentSummaries)
        }
    }

    suspend fun saveTransactions(transactions: List<Transaction>) {
        context.dataStore.edit { preferences ->
            preferences[TRANSACTIONS] = Json.encodeToString(transactions)
        }
    }

    suspend fun saveLabels(labels: List<Label>) {
        context.dataStore.edit { preferences ->
            preferences[LABELS] = Json.encodeToString(labels)
        }
    }

    suspend fun updateLabelsAndTransactions(labels: List<Label>, transactions: List<Transaction>) {
        context.dataStore.edit { preferences ->
            preferences[LABELS] = Json.encodeToString(labels)
            preferences[TRANSACTIONS] = Json.encodeToString(transactions)
        }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun clearCurrentMonthSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(MONTHLY_SALARY)
            preferences.remove(DAYS_WORKED)
            preferences.remove(SAVED_MONTH)
        }
    }
    
    suspend fun updateSavedMonth(monthYear: String) {
        context.dataStore.edit { preferences ->
            preferences[SAVED_MONTH] = monthYear
        }
    }

    suspend fun updateDebugMonthOffset(offset: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEBUG_MONTH_OFFSET] = offset
        }
    }
}
