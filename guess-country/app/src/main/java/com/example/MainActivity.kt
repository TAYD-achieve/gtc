package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.ui.screens.GameRoomScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.GameViewModelFactory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Instantiate persistent SQL game database and repository
        val database = GameDatabase.getDatabase(this)
        val repository = GameRepository(database)

        // 2. Instantiate Main State Controller ViewModel
        val viewModelFactory = GameViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[GameViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val lobbyRooms by viewModel.lobbyRooms.collectAsStateWithLifecycle()
                val activeRoom by viewModel.activeRoom.collectAsStateWithLifecycle()
                val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
                val uiToast by viewModel.uiToast.collectAsStateWithLifecycle()
                val opponentTyping by viewModel.opponentTyping.collectAsStateWithLifecycle()
                val singleDifficulty by viewModel.singlePlayerDifficulty.collectAsStateWithLifecycle()

                // Auto dismiss toast notifications after 3 seconds
                LaunchedEffect(uiToast) {
                    if (uiToast != null) {
                        delay(3500)
                        viewModel.clearToast()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberBackground
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Standard Content Switcher
                        Crossfade(
                            targetState = currentScreen,
                            animationSpec = tween(350),
                            label = "MainScreenCrossfade",
                            modifier = Modifier.fillMaxSize()
                        ) { screen ->
                            when (screen) {
                                GameViewModel.Screen.LOGIN -> {
                                    LoginScreen(
                                        onLoginClick = { name, avatar ->
                                            viewModel.login(name, avatar)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                GameViewModel.Screen.LOBBY -> {
                                    if (userProfile != null) {
                                        LobbyScreen(
                                            userProfile = userProfile!!,
                                            lobbyRooms = lobbyRooms,
                                            matchHistory = matchHistory,
                                            singleDifficulty = singleDifficulty,
                                            onDifficultyChange = { viewModel.setDifficulty(it) },
                                            onCreateRoomClick = { viewModel.createRoom(it) },
                                            onJoinRoomClick = { viewModel.joinRoom(it) },
                                            onStartComputerMatchClick = { viewModel.startSinglePlayerComputerMatch() },
                                            onLogoutClick = { viewModel.logout() },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                GameViewModel.Screen.ROOM -> {
                                    if (activeRoom != null && userProfile != null) {
                                        GameRoomScreen(
                                            room = activeRoom!!,
                                            currentUser = userProfile!!,
                                            opponentTyping = opponentTyping,
                                            onSubmitSecretCountry = { viewModel.submitSecretCountry(it) },
                                            onGuessLetter = { viewModel.guessLetter(it) },
                                            onPredictCountry = { viewModel.predictCountry(it) },
                                            onSendChatMessage = { viewModel.sendUserChatMessage(it) },
                                            onLeaveRoomClick = { viewModel.leaveRoom() },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        // Fallback if state leaks
                                        LaunchedEffect(Unit) {
                                            viewModel.showScreen(GameViewModel.Screen.LOBBY)
                                        }
                                    }
                                }

                                else -> {
                                    // Handle missing paths gracefully
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Initializing Arena Systems...", color = CyanNeon)
                                    }
                                }
                            }
                        }

                        // 2. High-Fidelity Floating Insets Toast Notification Overlay
                        AnimatedVisibility(
                            visible = uiToast != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 80.dp)
                                .padding(horizontal = 24.dp)
                        ) {
                            uiToast?.let { message ->
                                val isSpellingError = message.contains("spelling", ignoreCase = true) || message.contains("not found", ignoreCase = true)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 500.dp)
                                        .shadow(16.dp, RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.clearToast() },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSpellingError) RedIncorrect else CyanNeon
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSpellingError) Icons.Default.Warning else Icons.Default.Info,
                                            contentDescription = "Alert",
                                            tint = if (isSpellingError) Color.White else CyberBackground,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isSpellingError) "WORD SECURITY CHALLENGE" else "ARENA ALERT",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isSpellingError) Color.White.copy(alpha = 0.8f) else CyberBackground.copy(alpha = 0.8f),
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = message,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = if (isSpellingError) Color.White else CyberBackground,
                                                modifier = Modifier.padding(top = 2.dp)
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
}
