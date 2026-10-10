package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expression: String,
    val result: String,
    val isComplex: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val angleMode: String = "DEG",
    val isFavorite: Boolean = false,
    val label: String = ""
)

@Entity(tableName = "saved_equations")
data class SavedEquationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val expression: String,
    val description: String = "",
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false,
    val isFavorite: Boolean = false
)

@Entity(tableName = "saved_graph_functions")
data class SavedGraphFunction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expression: String,
    val colorHex: String,
    val isVisible: Boolean = true,
    val name: String = ""
)
