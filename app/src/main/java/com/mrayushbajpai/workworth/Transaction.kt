package com.mrayushbajpai.workworth

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Label(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Int // Store color as Int (ARGB)
)

@Serializable
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val timeCost: Double,
    val iconType: String = "default",
    val monthYear: String,
    val labelIds: List<String> = emptyList()
)
