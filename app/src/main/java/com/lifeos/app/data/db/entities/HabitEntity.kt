package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class HabitFrequency { DAILY, WEEKLY, CUSTOM }

/**
 * Habit Tracker — Section 11/12/13.
 * `goalCount` supports quantity-based habits like "Drink Water 8/8" from the
 * Home dashboard mock (Section 5), not just binary done/not-done.
 */
@Serializable
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val category: String? = null,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val customDaysCsv: String? = null,
    val goalCount: Int = 1,
    val reminderEpochMillis: Long? = null,
    val startDateEpochDay: Long,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * One row per (habit, date). `progressCount` vs `goalCount` on the parent habit
 * drives both the checkmark UI and the heatmap intensity levels (Section 13:
 * Missed / Partial / Completed / Exceptional).
 */
@Serializable
@Entity(tableName = "habit_completions", primaryKeys = ["habitId", "dateEpochDay"])
data class HabitCompletionEntity(
    val habitId: String,
    val dateEpochDay: Long,
    val progressCount: Int,
    val completedAtEpochMillis: Long
)
