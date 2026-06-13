package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GameRoomScreen(
    room: GameRoom,
    currentUser: UserAccount,
    opponentTyping: Boolean,
    onSubmitSecretCountry: (String) -> Unit,
    onGuessLetter: (Char) -> Unit,
    onPredictCountry: (String) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onLeaveRoomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var countryInputText by remember { mutableStateOf("") }
    var showPredictDialog by remember { mutableStateOf(false) }

    val isCreator = room.creator.id == currentUser.id
    val myParticipantState = if (isCreator) room.p1State else room.p2State
    val opponentParticipantState = if (isCreator) room.p2State else room.p1State
    val opponentAccount = if (isCreator) room.guest else room.creator

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Upper Game Status Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onLeaveRoomClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Leave", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = room.roomName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (room.status == RoomStatus.PLAYING) "Duel In Progress" else "Room Setup",
                            fontSize = 11.sp,
                            color = CyanNeon,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (opponentAccount != null) {
                    // Match opponent display badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = opponentAccount.avatarEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = opponentAccount.username,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 100.dp)
                            )
                        }
                    }
                }
            }
        }

        // Branch UI based on Room state
        AnimatedContent(
            targetState = room.status,
            label = "stateScreenSwap",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { status ->
            when (status) {
                RoomStatus.WAITING_FOR_PLAYERS -> {
                    // Lobby waiting queue loader
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            CircularProgressIndicator(color = CyanNeon)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "MATCHMAKING SYSTEM SCANNING...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Simulated network nodes are seeking an open opponent to join your duel. Hang in tight...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                RoomStatus.SETTING_COUNTRIES -> {
                    // Layout where player enters their secret country word
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = OrangeNeon,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 16.dp)
                        )

                        Text(
                            text = "Set Opponent's Secret Target",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Input a real country name. The system will lock it in secret. Your opponent must guess its letters to survive! spelling error checking will apply.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        if (myParticipantState?.secretCountryName.isNullOrBlank()) {
                            // User hasn't registered a country yet
                            OutlinedTextField(
                                value = countryInputText,
                                onValueChange = { countryInputText = it },
                                label = { Text("Country Name", color = TextSecondary) },
                                singleLine = true,
                                isError = countryInputText.isNotBlank() && countryInputText.length < 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = CyanNeon,
                                    unfocusedBorderColor = CyberSurfaceVariant,
                                    focusedLabelColor = CyanNeon,
                                    unfocusedLabelColor = TextSecondary
                                ),
                                placeholder = { Text("e.g. Switzerland, Japan", color = TextSecondary.copy(alpha = 0.4f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("country_selection_input")
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    onSubmitSecretCountry(countryInputText)
                                    countryInputText = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("lock_country_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = CyberBackground),
                                shape = RoundedCornerShape(12.dp),
                                enabled = countryInputText.isNotBlank()
                            ) {
                                Text("LOCK COUNTRY TARGET", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            // User locked theirs, waiting for opponent to finish setting theirs
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberSurface, RoundedCornerShape(12.dp))
                                    .border(1.dp, CyanNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Ready",
                                        tint = GreenCorrect,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Country Locked in Secret!",
                                        fontWeight = FontWeight.Bold,
                                        color = GreenCorrect,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Locked target: '${myParticipantState?.secretCountryName}'",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrangeNeon, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Waiting for ${opponentAccount?.username ?: "opponent"} to submit theirs...",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                RoomStatus.PLAYING, RoomStatus.FINISHED -> {
                    // Active Gameplay Screens
                    if (myParticipantState != null && opponentParticipantState != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            // Split layout: Upper half contains board statistics and blanks, Bottom half contains Chat
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Turn-Taking Status Card Indicator
                                val isMyTurn = room.currentTurnPlayerId == currentUser.id && room.status == RoomStatus.PLAYING
                                val cardBorderColor = if (isMyTurn) CyanNeon else Color.Transparent
                                val cardBg = if (isMyTurn) CyanNeon.copy(alpha = 0.05f) else CyberSurface

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isMyTurn) Icons.Default.PlayArrow else Icons.Default.Refresh,
                                                contentDescription = "Turn Status",
                                                tint = if (isMyTurn) CyanNeon else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (room.status == RoomStatus.FINISHED) {
                                                    "Match Concluded!"
                                                } else if (isMyTurn) {
                                                    " YOUR TURN TO GUESS!"
                                                } else {
                                                    " ${opponentAccount?.username ?: "Opponent"}'s TURN..."
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMyTurn) CyanNeon else TextPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        if (room.status == RoomStatus.PLAYING) {
                                            // Prediction Trigger Button
                                            Button(
                                                onClick = { showPredictDialog = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = OrangeNeon,
                                                    contentColor = CyberBackground
                                                ),
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .testTag("predict_button"),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                enabled = isMyTurn
                                            ) {
                                                Text(
                                                    "PREDICT FULL WORD",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Blanks Character Display Container
                                Text(
                                    text = "OPPONENT'S SECRET COUNTRY BOARD:",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Real secrets blanks: using myParticipantState.guessedLetters to mask opponentParticipantState.secretCountryName
                                val targetSecretName = opponentParticipantState.secretCountryName
                                val myRevealedSet = myParticipantState.guessedLetters
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CyberSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .border(1.dp, CyberSurfaceVariant, RoundedCornerShape(16.dp))
                                        .padding(vertical = 20.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        targetSecretName.forEach { char ->
                                            if (char.isWhitespace()) {
                                                // Spacer space
                                                Spacer(modifier = Modifier.width(16.dp))
                                            } else {
                                                val charLower = char.lowercaseChar()
                                                val isRevealed = myRevealedSet.contains(charLower)
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (isRevealed || room.status == RoomStatus.FINISHED) {
                                                            char.uppercaseChar().toString()
                                                        } else {
                                                            " "
                                                        },
                                                        fontSize = 22.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isRevealed) CyanNeon else TextPrimary,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .width(16.dp)
                                                            .height(3.dp)
                                                            .background(if (isRevealed) CyanNeon else TextSecondary)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (room.status == RoomStatus.FINISHED && room.winnerAnnouncement != null) {
                                    // Match result banner
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp)
                                            .background(
                                                if (room.winningPlayerId == currentUser.id) GreenCorrect.copy(alpha = 0.12f) else RedIncorrect.copy(alpha = 0.12f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (room.winningPlayerId == currentUser.id) GreenCorrect else RedIncorrect,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = if (room.winningPlayerId == currentUser.id) "🏆 VICTORY!" else "💀 DEFEATED!",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                color = if (room.winningPlayerId == currentUser.id) GreenCorrect else RedIncorrect,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = room.winnerAnnouncement ?: "",
                                                fontSize = 12.sp,
                                                color = TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // A-Z interactive keyboard grid
                                if (room.status == RoomStatus.PLAYING) {
                                    val fullAlphabet = ('a'..'z').toList()
                                    
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(7),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(fullAlphabet) { char ->
                                            val lowercaseChar = char.lowercaseChar()
                                            val isGuessed = myRevealedSet.contains(lowercaseChar)
                                            
                                            // Is this letter actually in the target secret word?
                                            val isInWord = targetSecretName.contains(char, ignoreCase = true)
                                            
                                            val keyBg = when {
                                                isGuessed && isInWord -> GreenCorrect.copy(alpha = 0.15f)
                                                isGuessed && !isInWord -> RedIncorrect.copy(alpha = 0.1f)
                                                else -> CyanNeon
                                            }
                                            
                                            val keyBorderColor = when {
                                                isGuessed && isInWord -> GreenCorrect
                                                isGuessed && !isInWord -> RedIncorrect
                                                else -> CyanNeon
                                            }

                                            val keyTextColor = when {
                                                isGuessed && isInWord -> GreenCorrect
                                                isGuessed && !isInWord -> RedIncorrect.copy(alpha = 0.4f)
                                                else -> Color.White
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(keyBg)
                                                    .border(1.dp, keyBorderColor, RoundedCornerShape(8.dp))
                                                    .clickable(enabled = isMyTurn && !isGuessed) {
                                                        onGuessLetter(char)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = char.toString().uppercase(),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = keyTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Bottom half: Chat Log console
                            Card(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    // Live Chat header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp, end = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Chat",
                                            tint = CyanNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "DUEL TEAM CHANNEL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    // Chat Messages list scrollable
                                    val listState = rememberLazyListState()
                                    
                                    // Auto scroll chat to newest messages
                                    LaunchedEffect(room.chatMessages.size, opponentTyping) {
                                        if (room.chatMessages.isNotEmpty()) {
                                            listState.animateScrollToItem(room.chatMessages.size - 1)
                                        }
                                    }

                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(CyberBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(room.chatMessages) { chat ->
                                            ChatBubbleItem(
                                                chat = chat,
                                                currentUserId = currentUser.id
                                            )
                                        }
                                        if (opponentTyping) {
                                            item {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = opponentAccount?.avatarEmoji ?: "🤖", fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${opponentAccount?.username ?: "Opponent"} is thinking in chat...",
                                                        fontSize = 11.sp,
                                                        color = CyanNeon,
                                                        fontWeight = FontWeight.Medium,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Chat input texting bar
                                    var textInputMessage by remember { mutableStateOf("") }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = textInputMessage,
                                            onValueChange = { textInputMessage = it },
                                            placeholder = { Text("Send chat message...", color = TextSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = CyberSurfaceVariant,
                                                unfocusedBorderColor = CyberSurfaceVariant
                                            ),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                            keyboardActions = KeyboardActions(
                                                onSend = {
                                                    if (textInputMessage.isNotBlank()) {
                                                        onSendChatMessage(textInputMessage)
                                                        textInputMessage = ""
                                                    }
                                                }
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .testTag("chat_input"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Send button
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (textInputMessage.isNotBlank()) CyanNeon else CyberSurfaceVariant)
                                                .clickable(enabled = textInputMessage.isNotBlank()) {
                                                    onSendChatMessage(textInputMessage)
                                                    textInputMessage = ""
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "Send Message",
                                                tint = if (textInputMessage.isNotBlank()) CyberBackground else TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal popup letting player execute full word country predictions
    if (showPredictDialog) {
        var inputPredictCountryName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showPredictDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    "Predict Opponent's Whole Country",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "WARNING: Predicting the entire country name is high stakes! If correct, you win immediately. If incorrect, your opponent wins immediately!",
                        fontSize = 12.sp,
                        color = RedIncorrect,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = inputPredictCountryName,
                        onValueChange = { inputPredictCountryName = it },
                        label = { Text("Country Guess", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = OrangeNeon,
                            unfocusedBorderColor = CyberSurfaceVariant,
                            focusedLabelColor = OrangeNeon,
                            unfocusedLabelColor = TextSecondary
                        ),
                        placeholder = { Text("e.g. Switzerland", color = TextSecondary.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPredictCountry(inputPredictCountryName)
                        showPredictDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = CyberBackground)
                ) {
                    Text("SUBMIT PREDICTION", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPredictDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ChatBubbleItem(
    chat: ChatMessage,
    currentUserId: String
) {
    val isSystem = chat.senderId == "system"
    val isMine = chat.senderId == currentUserId

    if (isSystem) {
        // System Ref announcements
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${chat.senderEmoji} ${chat.text}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        // Human player / bot chatting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            if (!isMine) {
                // Avatar badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CyberSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = chat.senderEmoji, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
                // Sender name
                Text(
                    text = chat.senderName,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isMine) 12.dp else 2.dp,
                                bottomEnd = if (isMine) 2.dp else 12.dp
                            )
                        )
                        .background(if (isMine) BlueNeon else CyberSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = chat.text,
                        fontSize = 13.sp,
                        color = if (isMine) Color.White else TextPrimary
                    )
                }
            }

            if (isMine) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CyberSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = chat.senderEmoji, fontSize = 16.sp)
                }
            }
        }
    }
}
