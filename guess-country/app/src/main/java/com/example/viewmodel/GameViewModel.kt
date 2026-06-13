package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.*
import com.example.network.GeminiManager
import kotlinx.coroutines.Delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {

    // Available Screens
    enum class Screen {
        LOGIN,
        LOBBY,
        ROOM,
        SINGLE_PLAYER_SETUP,
        HISTORY
    }

    private val _currentScreen = MutableStateFlow(Screen.LOGIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Logged-in User Info
    private val _userProfile = MutableStateFlow<UserAccount?>(null)
    val userProfile: StateFlow<UserAccount?> = _userProfile.asStateFlow()

    // Available online rooms in the Lobby
    private val _lobbyRooms = MutableStateFlow<List<GameRoom>>(emptyList())
    val lobbyRooms: StateFlow<List<GameRoom>> = _lobbyRooms.asStateFlow()

    // Currently joined room (null if in Lobby/Login)
    private val _activeRoom = MutableStateFlow<GameRoom?>(null)
    val activeRoom: StateFlow<GameRoom?> = _activeRoom.asStateFlow()

    // Past matches list
    val matchHistory: StateFlow<List<MatchEntity>> = repository.matchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback state (to show toast-like errors, validation, hints)
    private val _uiToast = MutableStateFlow<String?>(null)
    val uiToast: StateFlow<String?> = _uiToast.asStateFlow()

    // Is the opponent typing a chat message or processing?
    private val _opponentTyping = MutableStateFlow(false)
    val opponentTyping: StateFlow<Boolean> = _opponentTyping.asStateFlow()

    // Single Player Setup Details
    private val _singlePlayerDifficulty = MutableStateFlow(ComputerDifficulty.MEDIUM)
    val singlePlayerDifficulty: StateFlow<ComputerDifficulty> = _singlePlayerDifficulty.asStateFlow()

    // Mock bot players list to simulate full lobby activity
    private val botOpponents = listOf(
        UserAccount("bot_liam", "Liam_UK", "🇬🇧", 47, 30),
        UserAccount("bot_sofia", "Sofia_BR", "🇧🇷", 52, 29),
        UserAccount("bot_koki", "Koki_JP", "🇯🇵", 61, 35),
        UserAccount("bot_mia", "Mia_FR", "🇫🇷", 38, 41)
    )

    init {
        // Initialize the lobby rooms list with interesting simulated pending games
        refreshLobbyRooms()
    }

    fun showScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setDifficulty(difficulty: ComputerDifficulty) {
        _singlePlayerDifficulty.value = difficulty
    }

    fun clearToast() {
        _uiToast.value = null
    }

    fun showToast(msg: String) {
        _uiToast.value = msg
    }

    private fun refreshLobbyRooms() {
        val liam = botOpponents[0]
        val koki = botOpponents[2]
        
        _lobbyRooms.value = listOf(
            GameRoom(
                id = "room_liam",
                roomName = "London Pub Trivia 🌍",
                creator = liam,
                status = RoomStatus.WAITING_FOR_PLAYERS,
                chatMessages = listOf(
                    ChatMessage("c1", liam.id, liam.username, liam.avatarEmoji, "Welcome mates! Anyone down for a proper country quiz? 🍻")
                )
            ),
            GameRoom(
                id = "room_koki",
                roomName = "Quick Guess Tokyo ⚡",
                creator = koki,
                status = RoomStatus.WAITING_FOR_PLAYERS,
                chatMessages = listOf(
                    ChatMessage("c2", koki.id, koki.username, koki.avatarEmoji, "Hi! Let's play a high-speed guessing battle! 🚅")
                )
            )
        )
    }

    // --- Account / Log-in actions ---
    fun login(username: String, avatarEmoji: String) {
        val formattedName = username.trim()
        if (formattedName.isEmpty()) {
            _uiToast.value = "Username cannot be empty"
            return
        }

        viewModelScope.launch {
            // Check if profile already exists in Room
            val id = UUID.nameUUIDFromBytes(formattedName.lowercase().toByteArray()).toString()
            val existing = repository.getProfile(id)
            if (existing != null) {
                val acc = UserAccount(existing.id, existing.username, existing.avatarEmoji, existing.winCount, existing.lossCount)
                _userProfile.value = acc
            } else {
                val newProfile = ProfileEntity(id, formattedName, avatarEmoji, 0, 0, 0)
                repository.saveProfile(newProfile)
                _userProfile.value = UserAccount(id, formattedName, avatarEmoji, 0, 0)
            }
            refreshLobbyRooms()
            _currentScreen.value = Screen.LOBBY
        }
    }

    fun logout() {
        _userProfile.value = null
        _activeRoom.value = null
        _currentScreen.value = Screen.LOGIN
    }

    // --- Multi-Player Lobby actions ---
    fun createRoom(roomNameInput: String) {
        val user = _userProfile.value ?: return
        val name = if (roomNameInput.trim().isEmpty()) "${user.username}'s Arena 🏆" else roomNameInput.trim()
        
        val newRoom = GameRoom(
            id = "room_${UUID.randomUUID()}",
            roomName = name,
            creator = user,
            status = RoomStatus.WAITING_FOR_PLAYERS,
            chatMessages = listOf(
                ChatMessage("m1", "system", "Narrator", "📢", "Room created! Waiting for an opponent to join...")
            )
        )

        _activeRoom.value = newRoom
        _currentScreen.value = Screen.ROOM

        // Simulate an online bot player joining the room after a few seconds
        viewModelScope.launch {
            delay(2500)
            // Pick a bot that isn't the user
            val bot = botOpponents.filter { it.id != user.id }.random()
            val currentRoomVal = _activeRoom.value
            if (currentRoomVal != null && currentRoomVal.id == newRoom.id && currentRoomVal.status == RoomStatus.WAITING_FOR_PLAYERS) {
                // Join bot as guest
                val joinedRoom = currentRoomVal.copy(
                    guest = bot,
                    status = RoomStatus.SETTING_COUNTRIES,
                    chatMessages = currentRoomVal.chatMessages + listOf(
                        ChatMessage("m2", "system", "Narrator", "📢", "${bot.username} joined the room!"),
                        ChatMessage("m3", bot.id, bot.username, bot.avatarEmoji, "Hello there! Ready to set countries? Good luck! Let's go! 🚀")
                    )
                )
                _activeRoom.value = joinedRoom
            }
        }
    }

    fun joinRoom(room: GameRoom) {
        val user = _userProfile.value ?: return
        if (room.status != RoomStatus.WAITING_FOR_PLAYERS) {
            _uiToast.value = "Room is already full or in play"
            return
        }

        val updatedRoom = room.copy(
            guest = user,
            status = RoomStatus.SETTING_COUNTRIES,
            chatMessages = room.chatMessages + listOf(
                ChatMessage("m_join", "system", "Narrator", "📢", "${user.username} joined the room!"),
                ChatMessage("m_greet", user.id, user.username, user.avatarEmoji, "Hey! Happy to join, setting code-word country now! 🗺️")
            )
        )

        _activeRoom.value = updatedRoom
        _currentScreen.value = Screen.ROOM

        // Since the user joined the bot's room, let's trigger the host bot to submit their country word!
        viewModelScope.launch {
            delay(1500)
            // Bot submits country
            val currentRoomVal = _activeRoom.value
            if (currentRoomVal != null && currentRoomVal.id == updatedRoom.id && currentRoomVal.status == RoomStatus.SETTING_COUNTRIES) {
                val randomCountry = CountryDb.countries.random()
                
                val p1State = GameParticipant(updatedRoom.creator, randomCountry, emptySet())
                val partialRoom = currentRoomVal.copy(
                    p1State = p1State,
                    chatMessages = currentRoomVal.chatMessages + ChatMessage(
                        UUID.randomUUID().toString(),
                        "system", "System", "⚙️",
                        "${updatedRoom.creator.username} has submitted their secret country!"
                    )
                )
                _activeRoom.value = partialRoom
                
                // If user also submitted, start the game
                checkIfGameCanStart(partialRoom)
            }
        }
    }

    fun leaveRoom() {
        _activeRoom.value = null
        refreshLobbyRooms()
        _currentScreen.value = Screen.LOBBY
    }

    // --- Game Setup Configuration ---
    fun submitSecretCountry(inputName: String) {
        val room = _activeRoom.value ?: return
        val user = _userProfile.value ?: return

        // 1. Spell Check / Validation using CountryDb spellchecker
        when (val result = CountryDb.validateCountry(inputName)) {
            is CountryDb.ValidationResult.NotFound -> {
                _uiToast.value = "Country not found."
                return
            }
            is CountryDb.ValidationResult.SpellingError -> {
                _uiToast.value = "Spelling error: Did you mean '${result.suggestedName}'?"
                return
            }
            is CountryDb.ValidationResult.Exact -> {
                val correctName = result.actualName
                
                // Proceed to update state
                val isCreator = room.creator.id == user.id
                val updatedPart = GameParticipant(user, correctName, emptySet())
                
                val midRoom = if (isCreator) {
                    room.copy(
                        p1State = updatedPart,
                        chatMessages = room.chatMessages + ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderId = "system", senderName = "System", senderEmoji = "⚙️",
                            text = "${user.username} has locked in their country selection! 🔒"
                        )
                    )
                } else {
                    room.copy(
                        p2State = updatedPart,
                        chatMessages = room.chatMessages + ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderId = "system", senderName = "System", senderEmoji = "⚙️",
                            text = "${user.username} has locked in their country selection! 🔒"
                        )
                    )
                }

                _activeRoom.value = midRoom
                
                // Trigger Simulated Bot to submit country if they haven't already
                val opponent = if (isCreator) room.guest else room.creator
                if (opponent != null && opponent.id.startsWith("bot_")) {
                    val opponentState = if (isCreator) midRoom.p2State else midRoom.p1State
                    if (opponentState == null) {
                        viewModelScope.launch {
                            delay(1500)
                            val innerRoom = _activeRoom.value ?: return@launch
                            val randomCountry = CountryDb.countries.random()
                            val botParticipant = GameParticipant(opponent, randomCountry, emptySet())
                            
                            val postBotRoom = if (isCreator) {
                                innerRoom.copy(
                                    p2State = botParticipant,
                                    chatMessages = innerRoom.chatMessages + ChatMessage(
                                        UUID.randomUUID().toString(),
                                        "system", "System", "⚙️",
                                        "${opponent.username} has locked in their country selection! 🔒"
                                    )
                                )
                            } else {
                                innerRoom.copy(
                                    p1State = botParticipant,
                                    chatMessages = innerRoom.chatMessages + ChatMessage(
                                        UUID.randomUUID().toString(),
                                        "system", "System", "⚙️",
                                        "${opponent.username} has locked in their country selection! 🔒"
                                    )
                                )
                            }
                            _activeRoom.value = postBotRoom
                            checkIfGameCanStart(postBotRoom)
                        }
                        return
                    }
                }
                
                checkIfGameCanStart(midRoom)
            }
        }
    }

    private fun checkIfGameCanStart(room: GameRoom) {
        if (room.p1State != null && room.p2State != null) {
            // Both ready! Assign random first turn
            val firstTurnId = listOf(room.p1State.account.id, room.p2State.account.id).random()
            val finalRoom = room.copy(
                status = RoomStatus.PLAYING,
                currentTurnPlayerId = firstTurnId,
                chatMessages = room.chatMessages + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = "system", senderName = "Narrator", senderEmoji = "📢",
                    text = "A Match is live! ${if (firstTurnId == room.creator.id) room.creator.username else room.guest?.username} gets the first guess!"
                )
            )
            _activeRoom.value = finalRoom
            
            // If it's the bot's turn, trigger bot play immediately!
            if (firstTurnId.startsWith("bot_")) {
                triggerBotTurn(finalRoom)
            }
        }
    }

    // --- Live Turn-based Guessing & Prediction Gameplay ---
    fun guessLetter(char: Char) {
        val room = _activeRoom.value ?: return
        val user = _userProfile.value ?: return
        
        if (room.status != RoomStatus.PLAYING) return
        if (room.currentTurnPlayerId != user.id) {
            _uiToast.value = "It's not your turn!"
            return
        }

        val alphabet = char.lowercaseChar()
        
        // Check if user has already guessed this letter
        val isCreator = room.creator.id == user.id
        val myState = if (isCreator) room.p1State else room.p2State
        val opponentState = if (isCreator) room.p2State else room.p1State
        
        if (myState == null || opponentState == null) return
        
        if (myState.guessedLetters.contains(alphabet)) {
            _uiToast.value = "Letter '$char' has already been guessed by you!"
            return
        }

        // Add guess to player state. Match checks against Opponent's secret Country name!
        val newGuesses = myState.guessedLetters + alphabet
        val updatedMyState = myState.copy(guessedLetters = newGuesses)

        // Check if letter exists in opponent country name
        val letterFound = opponentState.secretCountryName.contains(alphabet, ignoreCase = true)
        val textNotification = if (letterFound) {
            "${user.username} guessed '$char' — and it was FOUND! 🎉"
        } else {
            "${user.username} guessed '$char' — not inside word. ❌"
        }

        val updatedRoom = if (isCreator) {
            room.copy(
                p1State = updatedMyState,
                chatMessages = room.copyChatWithSystem(textNotification),
                currentTurnPlayerId = opponentState.account.id
            )
        } else {
            room.copy(
                p2State = updatedMyState,
                chatMessages = room.copyChatWithSystem(textNotification),
                currentTurnPlayerId = opponentState.account.id
            )
        }

        _activeRoom.value = updatedRoom

        // Check if full country name is now revealed on player's screen!
        if (checkIfNameFullyGuessed(opponentState.secretCountryName, newGuesses)) {
            declareWinner(user.id, "solved the full word '${opponentState.secretCountryName}'! 🏆")
            return
        }

        // Trigger bot turn if the turn passed to bot opponent
        if (updatedRoom.currentTurnPlayerId.startsWith("bot_")) {
            triggerBotTurn(updatedRoom)
        }
    }

    fun predictCountry(prediction: String) {
        val room = _activeRoom.value ?: return
        val user = _userProfile.value ?: return

        if (room.status != RoomStatus.PLAYING) return
        if (room.currentTurnPlayerId != user.id) {
            _uiToast.value = "It's not your turn!"
            return
        }

        val rawPredict = prediction.trim()
        if (rawPredict.isEmpty()) {
            _uiToast.value = "Prediction empty"
            return
        }

        // Match prediction exactly (ignoring spacing or casing spelling discrepancies)
        val isCreator = room.creator.id == user.id
        val opponentState = if (isCreator) room.p2State else room.p1State
        if (opponentState == null) return

        val correctSecretName = opponentState.secretCountryName
        val isCorrect = correctSecretName.equals(rawPredict, ignoreCase = true)

        if (isCorrect) {
            declareWinner(user.id, "made a brave whole country prediction of '$correctSecretName' and it was CORRECT! 🎉")
        } else {
            // High-stakes gamble: lose instantly, opponent wins!
            declareWinner(opponentState.account.id, "won because ${user.username} made a brave prediction of '$rawPredict' which was incorrect! Selected country was '$correctSecretName'. ☠️")
        }
    }

    private fun checkIfNameFullyGuessed(actualName: String, guesses: Set<Char>): Boolean {
        for (char in actualName) {
            if (char.isWhitespace()) continue
            if (!guesses.contains(char.lowercaseChar())) {
                return false
            }
        }
        return true
    }

    private fun declareWinner(winnerId: String, reason: String) {
        val room = _activeRoom.value ?: return
        val user = _userProfile.value ?: return

        val winnerName = if (room.creator.id == winnerId) room.creator.username else room.guest?.username ?: "The Bot"
        val announcementText = "Game Over! $winnerName $reason"

        val finalRoom = room.copy(
            status = RoomStatus.FINISHED,
            winningPlayerId = winnerId,
            winnerAnnouncement = announcementText,
            chatMessages = room.chatMessages + ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = "system", senderName = "Narrator", senderEmoji = "📢",
                text = announcementText
            )
        )

        _activeRoom.value = finalRoom

        // Save progress to local persistent user database (Profile wins/losses + Match History record)
        viewModelScope.launch {
            val isUserWinner = winnerId == user.id
            val opponent = if (room.creator.id == user.id) room.guest else room.creator
            
            // Increment statistics in Database
            if (isUserWinner) {
                repository.incrementWin(user.id)
            } else {
                repository.incrementLoss(user.id)
            }

            // Record match history details block
            repository.insertMatch(
                MatchEntity(
                    opponentName = opponent?.username ?: "Computer",
                    opponentEmoji = opponent?.avatarEmoji ?: "🤖",
                    secretWord = if (room.creator.id == user.id) room.p2State?.secretCountryName ?: "" else room.p1State?.secretCountryName ?: "",
                    outcome = if (isUserWinner) "WON" else "LOST",
                    isOnlineRoom = !opponent?.id.equals("bot_computer")
                )
            )

            // Sync updated profile statistics in UI model
            val freshProfile = repository.getProfile(user.id)
            if (freshProfile != null) {
                _userProfile.value = UserAccount(freshProfile.id, freshProfile.username, freshProfile.avatarEmoji, freshProfile.winCount, freshProfile.lossCount)
            }
        }
    }

    // --- Automated Simulated Bot Brains ---
    private fun triggerBotTurn(room: GameRoom) {
        val botId = room.currentTurnPlayerId
        val user = _userProfile.value ?: return
        if (!botId.startsWith("bot_")) return

        viewModelScope.launch {
            // Typing delays
            delay(2800)
            val freshRoom = _activeRoom.value ?: return@launch
            if (freshRoom.status != RoomStatus.PLAYING || freshRoom.currentTurnPlayerId != botId) return@launch

            val isBotCreator = freshRoom.creator.id == botId
            val botState = if (isBotCreator) freshRoom.p1State else freshRoom.p2State
            val humanState = if (isBotCreator) freshRoom.p2State else freshRoom.p1State
            
            if (botState == null || humanState == null) return@launch

            // Bot strategy selection based on difficulty/persona logic
            val letterToGuess = selectBotLetterGuess(botState.guessedLetters, humanState.secretCountryName)
            if (letterToGuess == null) {
                // No letters left, resolve as draw or skip
                return@launch
            }

            // Dynamic prediction chance: Hard bot has standard 15% rate of choosing entire prediction if many letters are found
            val openRatio = humanState.secretCountryName.count { char ->
                char.isWhitespace() || botState.guessedLetters.contains(char.lowercaseChar())
            }.toFloat() / humanState.secretCountryName.length

            val isHardMode = botId == "bot_computer" && _singlePlayerDifficulty.value == ComputerDifficulty.HARD
            val isMediumMode = botId == "bot_computer" && _singlePlayerDifficulty.value == ComputerDifficulty.MEDIUM
            
            val shouldPredictCountry = when {
                isHardMode && openRatio >= 0.45f -> listOf(true, false, false).random() // 33% chance
                isMediumMode && openRatio >= 0.70f -> listOf(true, false, false, false).random() // 25% chance
                openRatio >= 0.85f -> listOf(true, false).random() // high chance for general bots
                else -> false
            }

            if (shouldPredictCountry) {
                // CPU makes a whole country guess of the human's secret country (correct guess!)
                declareWinner(botId, "makes a perfect, masterclass whole country prediction of '${humanState.secretCountryName}'! 🤖🧠")
                return@launch
            }

            // Normal letter guess
            val newGuesses = botState.guessedLetters + letterToGuess
            val updatedBotState = botState.copy(guessedLetters = newGuesses)
            val matchFound = humanState.secretCountryName.contains(letterToGuess, ignoreCase = true)
            
            val botName = botState.account.username
            val notificationText = if (matchFound) {
                "$botName guessed '$letterToGuess' — and it was FOUND! 🤖🎉"
            } else {
                "$botName guessed '$letterToGuess' — not inside word. 🤖❌"
            }

            val nextTurnId = user.id
            val botTurnResultRoom = if (isBotCreator) {
                freshRoom.copy(
                    p1State = updatedBotState,
                    chatMessages = freshRoom.copyChatWithSystem(notificationText),
                    currentTurnPlayerId = nextTurnId
                )
            } else {
                freshRoom.copy(
                    p2State = updatedBotState,
                    chatMessages = freshRoom.copyChatWithSystem(notificationText),
                    currentTurnPlayerId = nextTurnId
                )
            }

            _activeRoom.value = botTurnResultRoom

            // Did bot solve it by letter reveals?
            if (checkIfNameFullyGuessed(humanState.secretCountryName, newGuesses)) {
                declareWinner(botId, "solved the full word '${humanState.secretCountryName}'! 🤖🏆")
                return@launch
            }

            // Bot chatting wittily after they play!
            delay(1500)
            generateOpponentBotReactionMessage(botState.account, botState, humanState, letterToGuess, matchFound)
        }
    }

    private fun selectBotLetterGuess(guessedSet: Set<Char>, wordToGuess: String): Char? {
        val available = ('a'..'z').filter { !guessedSet.contains(it) }
        if (available.isEmpty()) return null

        // Easy mode profile or Liam / Mia just choose a completely random letter
        // Hard mode and Sophia search for standard popular vowels and letters
        val popularOrder = if (listOf(true, false).random()) {
            listOf('e', 'a', 'o', 'i', 't', 'n', 's', 'r', 'l', 'c', 'd', 'p', 'm', 'h', 'u', 'y', 'g', 'f', 'b', 'w', 'v', 'k', 'x', 'q', 'j', 'z')
        } else {
            listOf('a', 'e', 'i', 'o', 'u', 'y', 't', 'n', 's', 'r', 'h', 'l', 'd', 'c', 'm', 'f', 'p', 'g', 'w', 'b', 'v', 'k', 'x', 'j', 'q', 'z')
        }

        for (ch in popularOrder) {
            if (available.contains(ch)) {
                // If bot is Hard, high probability of choosing a valid letter if it exists in word
                val isCharInSecret = wordToGuess.contains(ch, ignoreCase = true)
                if (isCharInSecret && listOf(true, true, false).random()) {
                    return ch
                }
            }
        }
        
        return available.random()
    }

    private suspend fun generateOpponentBotReactionMessage(
        botAccount: UserAccount,
        botState: GameParticipant,
        humanState: GameParticipant,
        letterGuessed: Char,
        isFound: Boolean
    ) {
        val chatLogs = _activeRoom.value?.chatMessages ?: return
        
        // Let's create an intuitive chat response
        val botMsg = if (isFound) {
            listOf(
                "Haha, got you! '$letterGuessed' was a lucky guess for me! 🥳",
                "Nice, that fills a nice gap on my screen! 🗺️",
                "Yes! Count that. The board is turning green!",
                "Feeling confident about this country now! 😁"
            ).random()
        } else {
            listOf(
                "Ah shoot, I thought for sure '$letterGuessed' was in there! 😅",
                "Unlucky! You picked a difficult word my friend.",
                "Wait, no '$letterGuessed'? Super tricky! 🤔",
                "Alright, your turn. Show me what you've got!"
            ).random()
        }

        val freshRoom = _activeRoom.value ?: return
        val finalChatRoom = freshRoom.copy(
            chatMessages = freshRoom.chatMessages + ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = botAccount.id,
                senderName = botAccount.username,
                senderEmoji = botAccount.avatarEmoji,
                text = botMsg
            )
        )
        _activeRoom.value = finalChatRoom
    }

    // --- Live Chat functions ---
    fun sendUserChatMessage(text: String) {
        val room = _activeRoom.value ?: return
        val user = _userProfile.value ?: return
        val textTrim = text.trim()
        if (textTrim.isEmpty()) return

        // 1. Post local user message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = user.id,
            senderName = user.username,
            senderEmoji = user.avatarEmoji,
            text = textTrim
        )

        val updatedRoom = room.copy(
            chatMessages = room.chatMessages + userMsg
        )
        _activeRoom.value = updatedRoom

        // Find the opponent bot
        val isCreator = room.creator.id == user.id
        val opponent = if (isCreator) room.guest else room.creator
        
        if (opponent != null && opponent.id.startsWith("bot_")) {
            // Initiate reactive AI typing using Gemini
            _opponentTyping.value = true
            viewModelScope.launch {
                val opponentState = if (isCreator) room.p2State else room.p1State
                val opponentSecret = opponentState?.secretCountryName ?: ""
                val guessedLetters = opponentState?.guessedLetters ?: emptySet()

                // Call intelligent Gemini Manager
                val aiReply = GeminiManager.generateGameChatResponse(
                    username = opponent.username,
                    opponentSecret = opponentSecret,
                    guessedLetters = guessedLetters,
                    lastUserMessage = textTrim,
                    chatHistory = updatedRoom.chatMessages
                )

                // Typing suspense delay
                delay(1200)
                _opponentTyping.value = false

                val innerRoom = _activeRoom.value
                if (innerRoom != null && innerRoom.id == room.id) {
                    _activeRoom.value = innerRoom.copy(
                        chatMessages = innerRoom.chatMessages + ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderId = opponent.id,
                            senderName = opponent.username,
                            senderEmoji = opponent.avatarEmoji,
                            text = aiReply
                        )
                    )
                }
            }
        }
    }

    // --- Single-Player Mode vs Computer ---
    fun startSinglePlayerComputerMatch() {
        val user = _userProfile.value ?: return
        val difficulty = _singlePlayerDifficulty.value
        
        val cpuAccount = UserAccount(
            id = "bot_computer",
            username = "Cosmo AI (${difficulty.name})",
            avatarEmoji = "🤖",
            winCount = 99,
            lossCount = 99
        )

        val privateRoom = GameRoom(
            id = "computer_match",
            roomName = "Single Player Sandbox 🤖",
            creator = user,
            guest = cpuAccount,
            status = RoomStatus.SETTING_COUNTRIES,
            chatMessages = listOf(
                ChatMessage("c1", "system", "Narrator", "📢", "Single Player vs Cosmo AI started! Setup your secret country word to begin!"),
                ChatMessage("c2", cpuAccount.id, cpuAccount.username, cpuAccount.avatarEmoji, "Self-initializing... Ready! Enter your country and I will locks mine. Let's see your intelligence!")
            )
        )

        _activeRoom.value = privateRoom
        _currentScreen.value = Screen.ROOM

        // Lock in computer's secret country instantly
        viewModelScope.launch {
            delay(1000)
            val currentRoomVal = _activeRoom.value
            if (currentRoomVal != null && currentRoomVal.id == "computer_match" && currentRoomVal.status == RoomStatus.SETTING_COUNTRIES) {
                // Select random country for CPU
                val randomCountry = CountryDb.countries.random()
                val cpuParticipant = GameParticipant(cpuAccount, randomCountry, emptySet())

                val midCpuRoom = currentRoomVal.copy(
                    p2State = cpuParticipant,
                    chatMessages = currentRoomVal.chatMessages + ChatMessage(
                        UUID.randomUUID().toString(),
                        "system", "System", "⚙️",
                        "Cosmo AI has selected its secret country! 🤐"
                    )
                )
                _activeRoom.value = midCpuRoom
                checkIfGameCanStart(midCpuRoom)
            }
        }
    }

    // Extension properties to avoid boilerplate
    private fun GameRoom.copyChatWithSystem(text: String): List<ChatMessage> {
        return this.chatMessages + ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = "system", senderName = "Ref", senderEmoji = "⚖️",
            text = text
        )
    }
}

class GameViewModelFactory(
    private val application: Application,
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
