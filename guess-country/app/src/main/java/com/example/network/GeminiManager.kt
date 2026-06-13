package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Determines if the Gemini API key is configured with a valid user-set secret.
     */
    private val isApiKeyAvailable: Boolean
        get() = try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        } catch (e: Exception) {
            false
        }

    /**
     * Helper to call the direct REST API of Gemini.
     */
    suspend fun generateGameChatResponse(
        username: String,
        opponentSecret: String,
        guessedLetters: Set<Char>,
        lastUserMessage: String,
        chatHistory: List<ChatMessage>
    ): String {
        if (!isApiKeyAvailable) {
            Log.w(TAG, "Gemini API key is not configured or uses placeholder. Running in fallback mode.")
            return getFallbackResponse(username, opponentSecret, guessedLetters, lastUserMessage)
        }

        val hiddenOpponentName = opponentSecret
        val guessedText = if (guessedLetters.isEmpty()) "none" else guessedLetters.joinToString(", ")
        
        // Build the current secret country mask: e.g. "G _ R _ _ N Y"
        val mask = hiddenOpponentName.map { char ->
            if (char.isWhitespace()) " "
            else if (guessedLetters.contains(char.lowercaseChar())) char.uppercaseChar()
            else "_"
        }.joinToString(" ")

        val systemPrompt = """
            You are a human online player named $username playing a "Guess the Country" game.
            The opponent's secret country is "$hiddenOpponentName". This country is represented on your opponent's screen as: $mask
            Letters guessed so far: [$guessedText].
            Respond naturally to the last message of the user. Keep your message short (1 to 2 sentences max).
            Talk like an enthusiastic, friendly gamer. Feel free to use gamer slang, friendly banter, or emojis.
            Do NOT reveal the country name directly. You can give a subtle hint if they ask, but do not spoil it!
        """.trimIndent()

        // Construct history context
        val contextPrompt = StringBuilder()
        val recentHistory = chatHistory.takeLast(6)
        for (msg in recentHistory) {
            contextPrompt.append("${msg.senderName}: ${msg.text}\n")
        }
        contextPrompt.append("You (as $username): ")

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = "The chat conversation history:\n$contextPrompt\nLast user message: $lastUserMessage\nYour next message:")))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = api.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!generatedText.isNullOrBlank()) {
                generatedText.trim()
            } else {
                getFallbackResponse(username, opponentSecret, guessedLetters, lastUserMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API generation failed: ${e.message}", e)
            getFallbackResponse(username, opponentSecret, guessedLetters, lastUserMessage)
        }
    }

    /**
     * Clever deterministic templates that give rich, fun, contextual chat lines if the API key is offline!
     */
    private fun getFallbackResponse(
        username: String,
        opponentSecret: String,
        guessedLetters: Set<Char>,
        lastUserMessage: String
    ): String {
        val lowerMsg = lastUserMessage.lowercase()
        return when {
            lowerMsg.contains("hello") || lowerMsg.contains("hi") || lowerMsg.contains("hey") -> {
                listOf(
                    "Hey there! Let's see who's better at guessing today! 🌍",
                    "Hey! Hope you're ready to lose! Just kidding, good luck! 😄",
                    "What's up! Ready to roll? Pick a good first letter!"
                ).random()
            }
            lowerMsg.contains("hint") || lowerMsg.contains("help") || lowerMsg.contains("clue") -> {
                val len = opponentSecret.length
                val firstChar = opponentSecret.firstOrNull() ?: '?'
                val lastChar = opponentSecret.lastOrNull() ?: '?'
                val continentHint = getContinentHint(opponentSecret)
                listOf(
                    "Hmm, I shouldn't tell you, but it starts with $firstChar and has $len letters! 😉",
                    "Here is a small clue: I think it's located in $continentHint! 🗺️",
                    "No spoilers, but it's a super cool country! Try guessing a vowel!"
                ).random()
            }
            lowerMsg.contains("who are you") || lowerMsg.contains("your name") -> {
                "I'm $username, your worthy opponent! Bring it on! ⚔️"
            }
            lowerMsg.contains("hard") || lowerMsg.contains("difficult") -> {
                "Oh, definitely! This is high-IQ gameplay right here. 🧠"
            }
            lowerMsg.contains("win") || lowerMsg.contains("winner") -> {
                "We will see about that! One lucky letter can change everything! 🎲"
            }
            else -> {
                listOf(
                    "Nice one! Your turn is coming up, watch out! 😄",
                    "Hmm, interesting move! What letter is next? 🌍",
                    "Let's see if you can solve this one. It's a tricky country! 🤔",
                    "You're playing well! My turn is going to be epic.",
                    "Haha, this is awesome! I love guessing countries! ✈️"
                ).random()
            }
        }
    }

    private fun getContinentHint(countryName: String): String {
        val c = countryName.lowercase().trim()
        return when {
            // Asia
            c in listOf("india", "china", "japan", "indonesia", "pakistan", "vietnam", "thailand", "philippines", "singapore", "south korea", "malaysia") -> "Asia"
            // Europe
            c in listOf("france", "germany", "italy", "spain", "united kingdom", "sweden", "norway", "greece", "belgium", "poland", "switzerland") -> "Europe"
            // Africa
            c in listOf("egypt", "nigeria", "south africa", "kenya", "morocco", "ghana", "ethiopia", "algeria") -> "Africa"
            // America
            c in listOf("united states", "canada", "mexico", "brazil", "argentina", "colombia", "peru", "chile") -> "the Americas"
            else -> "our awesome planet"
        }
    }
}
