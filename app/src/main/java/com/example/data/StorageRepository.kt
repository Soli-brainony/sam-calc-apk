package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val dao: CalculationDao) {
    val allHistory: Flow<List<CalculationEntity>> = dao.getAllHistory()
    val favoriteHistory: Flow<List<CalculationEntity>> = dao.getFavoriteHistory()

    fun searchHistory(query: String): Flow<List<CalculationEntity>> {
        return if (query.isBlank()) {
            dao.getAllHistory()
        } else {
            dao.searchHistory(query)
        }
    }

    suspend fun insert(item: CalculationEntity): Long = dao.insertHistory(item)

    suspend fun update(item: CalculationEntity) = dao.updateHistory(item)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = dao.setFavorite(id, isFavorite)

    suspend fun delete(item: CalculationEntity) = dao.deleteHistory(item)

    suspend fun clearAll() = dao.clearAll()
}

class EquationRepository(private val dao: SavedEquationDao) {
    val allEquations: Flow<List<SavedEquationEntity>> = dao.getAllEquations()

    fun getByCategory(category: String): Flow<List<SavedEquationEntity>> {
        return if (category == "All") {
            dao.getAllEquations()
        } else {
            dao.getEquationsByCategory(category)
        }
    }

    fun searchEquations(query: String): Flow<List<SavedEquationEntity>> {
        return if (query.isBlank()) {
            dao.getAllEquations()
        } else {
            dao.searchEquations(query)
        }
    }

    suspend fun insert(equation: SavedEquationEntity): Long = dao.insertEquation(equation)

    suspend fun update(equation: SavedEquationEntity) = dao.updateEquation(equation)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = dao.setFavorite(id, isFavorite)

    suspend fun delete(equation: SavedEquationEntity) = dao.deleteEquation(equation)

    suspend fun clearUserEquations() = dao.clearUserEquations()

    suspend fun populateDefaults(equations: List<SavedEquationEntity>) = dao.insertAll(equations)
}
