package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculation_history ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllHistory(): Flow<List<CalculationEntity>>

    @Query("SELECT * FROM calculation_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteHistory(): Flow<List<CalculationEntity>>

    @Query("SELECT * FROM calculation_history WHERE expression LIKE '%' || :query || '%' OR result LIKE '%' || :query || '%' OR label LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<CalculationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CalculationEntity): Long

    @Update
    suspend fun updateHistory(item: CalculationEntity)

    @Query("UPDATE calculation_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Delete
    suspend fun deleteHistory(item: CalculationEntity)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAll()
}

@Dao
interface SavedEquationDao {
    @Query("SELECT * FROM saved_equations ORDER BY isFavorite DESC, category ASC, id ASC")
    fun getAllEquations(): Flow<List<SavedEquationEntity>>

    @Query("SELECT * FROM saved_equations WHERE category = :category ORDER BY id ASC")
    fun getEquationsByCategory(category: String): Flow<List<SavedEquationEntity>>

    @Query("SELECT * FROM saved_equations WHERE title LIKE '%' || :query || '%' OR expression LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchEquations(query: String): Flow<List<SavedEquationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquation(equation: SavedEquationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(equations: List<SavedEquationEntity>)

    @Update
    suspend fun updateEquation(equation: SavedEquationEntity)

    @Query("UPDATE saved_equations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Delete
    suspend fun deleteEquation(equation: SavedEquationEntity)

    @Query("DELETE FROM saved_equations WHERE isBuiltIn = 0")
    suspend fun clearUserEquations()
}

@Dao
interface GraphDao {
    @Query("SELECT * FROM saved_graph_functions ORDER BY id ASC")
    fun getAllFunctions(): Flow<List<SavedGraphFunction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFunction(function: SavedGraphFunction): Long

    @Update
    suspend fun updateFunction(function: SavedGraphFunction)

    @Delete
    suspend fun deleteFunction(function: SavedGraphFunction)

    @Query("DELETE FROM saved_graph_functions")
    suspend fun clearAll()
}

@Database(
    entities = [CalculationEntity::class, SavedEquationEntity::class, SavedGraphFunction::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calculationDao(): CalculationDao
    abstract fun savedEquationDao(): SavedEquationDao
    abstract fun graphDao(): GraphDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scigraph_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default reference equations in background
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.savedEquationDao()?.insertAll(defaultReferenceEquations)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val defaultReferenceEquations = listOf(
            SavedEquationEntity(
                title = "Quadratic Formula",
                expression = "(-b + sqrt(b^2 - 4*a*c)) / (2*a)",
                description = "Solutions to ax² + bx + c = 0",
                category = "Algebra",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Pythagorean Theorem",
                expression = "sqrt(a^2 + b^2)",
                description = "Hypotenuse c = √(a² + b²)",
                category = "Geometry",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Euler's Identity",
                expression = "exp(i * pi) + 1",
                description = "e^(iπ) + 1 = 0 (Evaluates to 0)",
                category = "Complex",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Kinetic Energy",
                expression = "0.5 * m * v^2",
                description = "Energy of motion: Ek = ½mv²",
                category = "Physics",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Gravitational Potential Energy",
                expression = "m * 9.80665 * h",
                description = "Ep = mgh with g = 9.81 m/s²",
                category = "Physics",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Ohm's Law (Voltage)",
                expression = "I * R",
                description = "V = I × R (Current × Resistance)",
                category = "Physics",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Area of a Circle",
                expression = "pi * r^2",
                description = "A = πr²",
                category = "Geometry",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Compound Interest",
                expression = "P * (1 + r / n)^(n * t)",
                description = "Future value with compounding",
                category = "Finance",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Complex Modulus |z|",
                expression = "sqrt(a^2 + b^2)",
                description = "Magnitude of complex number a + bi",
                category = "Complex",
                isBuiltIn = true
            ),
            SavedEquationEntity(
                title = "Law of Cosines (Side c)",
                expression = "sqrt(a^2 + b^2 - 2*a*b*cos(C))",
                description = "c = √(a² + b² - 2ab cos C)",
                category = "Geometry",
                isBuiltIn = true
            )
        )
    }
}
