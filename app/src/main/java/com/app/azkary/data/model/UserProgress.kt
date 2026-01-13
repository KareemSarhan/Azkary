package com.app.azkary.data.model

import androidx.room.Entity
import java.time.LocalDate

@Entity(
    tableName = "user_progress",
    primaryKeys = ["itemId", "categoryId", "date"]
)
data class UserProgress(
    val itemId: String,
    val categoryId: String,
    val date: String, // ISO date string YYYY-MM-DD
    val currentRepeats: Int,
    val isCompleted: Boolean
)
