package com.mrayushbajpai.workworth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val MONTHLY_SALARY = doublePreferencesKey("monthly_salary")
        val DAYS_WORKED = doublePreferencesKey("days_worked")
        val SAVED_MONTH = stringPreferencesKey("saved_month")
        val TRANSACTIONS = stringPreferencesKey("transactions")
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

    suspend fun saveSettings(salary: Double, days: Double, monthYear: String) {
        context.dataStore.edit { preferences ->
            preferences[MONTHLY_SALARY] = salary
            preferences[DAYS_WORKED] = days
            preferences[SAVED_MONTH] = monthYear
        }
    }

    suspend fun saveTransactions(transactions: List<Transaction>) {
        context.dataStore.edit { preferences ->
            preferences[TRANSACTIONS] = Json.encodeToString(transactions)
        }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(MONTHLY_SALARY)
            preferences.remove(DAYS_WORKED)
            preferences.remove(SAVED_MONTH)
            preferences.remove(TRANSACTIONS)
        }
    }
    
    suspend fun updateSavedMonth(monthYear: String) {
        context.dataStore.edit { preferences ->
            preferences[SAVED_MONTH] = monthYear
        }
    }
}
