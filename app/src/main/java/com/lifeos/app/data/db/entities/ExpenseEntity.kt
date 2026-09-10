package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethod { CASH, UPI, CARD, BANK, OTHER }

/**
 * Expense Tracker — Section 14/15. Category is a free string keyed against
 * ExpenseCategories (domain layer) so users can still add custom categories
 * later without a schema migration.
 */
@Serializable
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val category: String,
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val merchant: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.OTHER,
    val note: String? = null,
    val tagsCsv: String = "",
    val createdAt: Long
)
