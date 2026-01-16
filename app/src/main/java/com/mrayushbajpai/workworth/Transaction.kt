package com.mrayushbajpai.workworth

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class Transaction(
    val id: String,
    val name: String,
    val amount: Double,
    val timeCost: Double,
    val iconType: String = "default"
)
