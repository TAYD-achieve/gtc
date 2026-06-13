package com.example.model

import com.squareup.moshi.JsonClass

enum class ComputerDifficulty {
    EASY,
    MEDIUM,
    HARD
}

data class UserAccount(
    val id: String,
    val username: String,
    val avatarEmoji: String,
    val winCount: Int = 0,
    val lossCount: Int = 0
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderEmoji: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GameParticipant(
    val account: UserAccount,
    val secretCountryName: String = "", // Hidden from the opponent
    val guessedLetters: Set<Char> = emptySet(),
    val errorState: String? = null
)

// The overall state of a room
data class GameRoom(
    val id: String,
    val roomName: String,
    val creator: UserAccount,
    val guest: UserAccount? = null,
    val status: RoomStatus = RoomStatus.WAITING_FOR_PLAYERS,
    val p1State: GameParticipant? = null, // creator
    val p2State: GameParticipant? = null, // guest
    val currentTurnPlayerId: String = "",
    val winningPlayerId: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val winnerAnnouncement: String? = null
)

enum class RoomStatus {
    WAITING_FOR_PLAYERS,
    SETTING_COUNTRIES, // Waiting for both players to enter secret countries
    PLAYING,
    FINISHED
}

// Data structures for Gemini REST API integration
@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>?
)
