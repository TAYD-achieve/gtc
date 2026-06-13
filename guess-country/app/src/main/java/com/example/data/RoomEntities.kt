package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val username: String,
    val avatarEmoji: String,
    val winCount: Int = 0,
    val lossCount: Int = 0,
    val currentStreak: Int = 0
)

@Entity(tableName = "match_history")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val matchId: Int = 0,
    val opponentName: String,
    val opponentEmoji: String,
    val secretWord: String,
    val outcome: String, // "WON" or "LOST"
    val isOnlineRoom: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
