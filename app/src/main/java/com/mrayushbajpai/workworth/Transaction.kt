package com.mrayushbajpai.workworth

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    val name: String,
    val amount: Double,
    val timeCost: Double,
    val iconType: String = "default",
    val monthYear: String
)
