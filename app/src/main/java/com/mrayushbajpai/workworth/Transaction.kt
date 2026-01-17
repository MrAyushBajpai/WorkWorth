package com.mrayushbajpai.workworth

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Label(
    val id: String, // lowercase name as the ID
    val name: String,
    val color: Int // Store color as Int (ARGB)
) {
    companion object {
        fun create(name: String, color: Int): Label {
            return Label(
                id = name.lowercase().trim(),
                name = name.trim(),
                color = color
            )
        }
    }
}

@Serializable
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val name: String,
    val amount: Double,
    val timeCost: Double,
    val iconType: String = "default",
    val monthYear: String,
    val labelIds: List<String> = emptyList()
)
