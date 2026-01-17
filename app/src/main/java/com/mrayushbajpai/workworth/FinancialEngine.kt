package com.mrayushbajpai.workworth

object FinancialEngine {
    /**
     * Calculates the time cost of an expense.
     * Logic: (Amount / (Salary / DaysWorked)) / 8 hours per day (assuming standard 8-hour workday)
     * Actually, if we want "Time is Money" consistency:
     * Hourly Rate = Salary / (DaysWorked * 8)
     * Time Cost (in hours) = Amount / Hourly Rate
     */
    fun calculateTimeCost(amount: Double, salary: Double, daysWorked: Double): Double {
        if (salary <= 0 || daysWorked <= 0) return 0.0
        val hourlyRate = salary / (daysWorked * 8.0)
        return if (hourlyRate > 0) amount / hourlyRate else 0.0
    }

    fun calculateRemainingDays(remainingMoney: Double, salary: Double, daysWorked: Double): Double {
        if (salary <= 0) return 0.0
        return (remainingMoney / salary) * daysWorked
    }
}
