package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM match_history ORDER BY timestamp DESC")
    fun getMatchHistory(): Flow<List<MatchEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMatch(match: MatchEntity)

    @Query("DELETE FROM match_history")
    suspend fun clearHistory()
}

@Database(entities = [ProfileEntity::class, MatchEntity::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "guess_country_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRepository(private val db: GameDatabase) {
    val allProfiles: Flow<List<ProfileEntity>> = db.profileDao().getAllProfiles()
    val matchHistory: Flow<List<MatchEntity>> = db.matchDao().getMatchHistory()

    suspend fun getProfile(id: String): ProfileEntity? = db.profileDao().getProfileById(id)

    suspend fun saveProfile(profile: ProfileEntity) {
        db.profileDao().saveProfile(profile)
    }

    suspend fun insertMatch(match: MatchEntity) {
        db.matchDao().insertMatch(match)
    }

    suspend fun incrementWin(profileId: String) {
        val original = db.profileDao().getProfileById(profileId) ?: return
        val updated = original.copy(
            winCount = original.winCount + 1,
            currentStreak = original.currentStreak + 1
        )
        db.profileDao().saveProfile(updated)
    }

    suspend fun incrementLoss(profileId: String) {
        val original = db.profileDao().getProfileById(profileId) ?: return
        val updated = original.copy(
            lossCount = original.lossCount + 1,
            currentStreak = 0
        )
        db.profileDao().saveProfile(updated)
    }
}
