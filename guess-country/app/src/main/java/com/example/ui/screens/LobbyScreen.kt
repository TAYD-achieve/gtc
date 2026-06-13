package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchEntity
import com.example.model.ComputerDifficulty
import com.example.model.GameRoom
import com.example.model.UserAccount
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    userProfile: UserAccount,
    lobbyRooms: List<GameRoom>,
    matchHistory: List<MatchEntity>,
    singleDifficulty: ComputerDifficulty,
    onDifficultyChange: (ComputerDifficulty) -> Unit,
    onCreateRoomClick: (roomName: String) -> Unit,
    onJoinRoomClick: (GameRoom) -> Unit,
    onStartComputerMatchClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var lobbyTabState by remember { mutableStateOf(0) } // 0 = Live Lobby, 1 = History Log

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Player Information Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Icon
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(CyberSurfaceVariant, CircleShape)
                            .border(2.dp, CyanNeon, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = userProfile.avatarEmoji, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = userProfile.username,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Score",
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Wins: ${userProfile.winCount}  |  Losses: ${userProfile.lossCount}",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Log out icon button
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .background(CyberSurfaceVariant, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log out",
                        tint = RedIncorrect,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Horizontal Segmented Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(CyberSurface, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            TabButton(
                title = "LIVE DETECTOR LOBBY",
                isSelected = lobbyTabState == 0,
                modifier = Modifier.weight(1f),
                onClick = { lobbyTabState = 0 }
            )
            TabButton(
                title = "DUEL RECORDS",
                isSelected = lobbyTabState == 1,
                modifier = Modifier.weight(1f),
                onClick = { lobbyTabState = 1 }
            )
        }

        Crossfade(
            targetState = lobbyTabState,
            label = "tabSwitch",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { tabIndex ->
            when (tabIndex) {
                0 -> {
                    // Live Multiplayers Lobby
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Section 1: Single Player vs AI Bot Console
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .border(1.dp, BlueNeon.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(0.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = "CPU Setup",
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Single-Player Vs Cosmo AI",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Text(
                                    text = "Challenge the computer system. Pick a difficulty caliber to match your country spelling vocabulary speeds.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Difficulty Toggles Selector Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ComputerDifficulty.values().forEach { d ->
                                        val isSelected = singleDifficulty == d
                                        val color = when (d) {
                                            ComputerDifficulty.EASY -> CyanNeon
                                            ComputerDifficulty.MEDIUM -> OrangeNeon
                                            ComputerDifficulty.HARD -> RedIncorrect
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) color.copy(alpha = 0.15f) else CyberSurfaceVariant)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) color else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onDifficultyChange(d) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = d.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) color else TextSecondary
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = onStartComputerMatchClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("single_player_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BlueNeon,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Start game"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "START COMPUTER MATCH",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Section 2: Online Rooms Lobby
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Lobbies",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Available Duels Lobby",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyanNeon.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${lobbyRooms.size} Rooms Live",
                                    fontSize = 11.sp,
                                    color = CyanNeon,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Room creation card trigger line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberSurfaceVariant)
                                .clickable { showCreateRoomDialog = true }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create Custom Duel Room",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (lobbyRooms.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.5f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Waiting",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Connecting to matchmaking servers...",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(lobbyRooms) { room ->
                                    RoomListItem(
                                        room = room,
                                        onJoinClick = { onJoinRoomClick(room) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Duel History Stats Records Log
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        if (matchHistory.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "No Matches History",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No recorded duel outcomes yet.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Compete against Cosmo AI or online bots to record stats.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 20.dp)
                            ) {
                                items(matchHistory) { match ->
                                    MatchHistoryItem(match = match)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal popup to create custom named duel rooms
    if (showCreateRoomDialog) {
        var inputRoomName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    "Custom Duel Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "Set up an online country guessing lobby for other players around the globe to discover and join.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = inputRoomName,
                        onValueChange = { if (it.length <= 25) inputRoomName = it },
                        label = { Text("Room Name", color = TextSecondary) },
                        singleLine = true,
                        placeholder = { Text("My Legendary Lobby...", color = TextSecondary.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CyberSurfaceVariant,
                            focusedLabelColor = CyanNeon,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateRoomClick(inputRoomName)
                        showCreateRoomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = CyberBackground)
                ) {
                    Text("CREATE ROOM", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyberSurfaceVariant else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CyanNeon else TextSecondary,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun RoomListItem(
    room: GameRoom,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.roomName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(CyanNeon.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OPEN",
                            fontSize = 8.sp,
                            color = CyanNeon,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(CyberSurfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = room.creator.avatarEmoji, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hosted by ${room.creator.username}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Join Room button with min touch target size
            Button(
                onClick = onJoinClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = CyberBackground),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("join_room_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "JOIN DUEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun MatchHistoryItem(match: MatchEntity) {
    val isWin = match.outcome == "WON"
    val dateString = remember(match.timestamp) {
        try {
            val format = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
            format.format(Date(match.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Large status circle (WIN or LOSS icon)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (isWin) GreenCorrect.copy(alpha = 0.1f) else RedIncorrect.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWin) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = if (isWin) "Win" else "Loss",
                        tint = if (isWin) GreenCorrect else RedIncorrect,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isWin) "VICTORY" else "DEFEAT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isWin) GreenCorrect else RedIncorrect,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (match.isOnlineRoom) "• Online Room" else "• vs Computer",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Opponent: ${match.opponentEmoji} ${match.opponentName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Text(
                        text = "Secret Country word: '${match.secretWord}'",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = dateString,
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }
}
