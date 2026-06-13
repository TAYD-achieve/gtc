import React, { useState, useEffect, useRef } from 'react';
import { 
  Compass, LogOut, Trophy, Sparkles, Plus, Play, ArrowLeft, 
  Send, Keyboard, History, User, Lock, Unlock, MessageSquare, 
  Check, X, AlertTriangle, Cpu, Globe, Info, RefreshCw
} from 'lucide-react';
import { countries, validateCountry } from './utils/countryDb';
import { generateGameChatResponse } from './utils/gemini';

// Local storage helpers
const SAVE_KEYS = {
  USER: 'guess_country_user_profile',
  HISTORY: 'guess_country_matches_history'
};

const BOT_OPPONENTS = [
  { id: "bot_liam", username: "Liam_UK", avatarEmoji: "🇬🇧", winCount: 47, lossCount: 30 },
  { id: "bot_sofia", username: "Sofia_BR", avatarEmoji: "🇧🇷", winCount: 52, lossCount: 29 },
  { id: "bot_koki", username: "Koki_JP", avatarEmoji: "🇯🇵", winCount: 61, lossCount: 35 },
  { id: "bot_mia", username: "Mia_FR", avatarEmoji: "🇫🇷", winCount: 38, lossCount: 41 }
];

export default function App() {
  // Navigation states: 'LOGIN', 'LOBBY', 'ROOM'
  const [screen, setScreen] = useState('LOGIN');
  
  // User profiles
  const [profile, setProfile] = useState(null);
  const [customKey, setCustomKey] = useState('');
  
  // Lobby rooms lists
  const [lobbyRooms, setLobbyRooms] = useState([]);
  const [lobbyTab, setLobbyTab] = useState(0); // 0 = MATCHES, 1 = HISTORY
  
  // Active Room state
  const [activeRoom, setActiveRoom] = useState(null);
  const [singleDifficulty, setSingleDifficulty] = useState('MEDIUM');
  
  // Match history
  const [history, setHistory] = useState([]);
  
  // Game state UI controllers
  const [toast, setToast] = useState(null);
  const [predictText, setPredictText] = useState('');
  const [showPredictModal, setShowPredictModal] = useState(false);
  const [opponentTyping, setOpponentTyping] = useState(false);

  // Chat scroll anchor
  const chatBottomRef = useRef(null);

  // Initial Load from LocalStorage
  useEffect(() => {
    const savedUser = localStorage.getItem(SAVE_KEYS.USER);
    const savedHistory = localStorage.getItem(SAVE_KEYS.HISTORY);
    
    if (savedUser) {
      setProfile(JSON.parse(savedUser));
      setScreen('LOBBY');
    }
    if (savedHistory) {
      setHistory(JSON.parse(savedHistory));
    }
    
    refreshLobby();
  }, []);

  // Sync profile edits
  const saveUserProfileChanged = (updatedProfile) => {
    setProfile(updatedProfile);
    localStorage.setItem(SAVE_KEYS.USER, JSON.stringify(updatedProfile));
  };

  // Add round outcome to database history log
  const recordMatchOutcome = (outcome, opponentName, opponentEmoji, secretWord, isOnline) => {
    const freshMatch = {
      id: "match_" + Date.now(),
      outcome, // 'WON' or 'LOST'
      opponentName,
      opponentEmoji,
      secretWord,
      isOnlineRoom: isOnline,
      timestamp: Date.now()
    };
    
    const nextHistory = [freshMatch, ...history];
    setHistory(nextHistory);
    localStorage.setItem(SAVE_KEYS.HISTORY, JSON.stringify(nextHistory));

    // Update win/loss records
    if (profile) {
      const up = {
        ...profile,
        winCount: outcome === 'WON' ? profile.winCount + 1 : profile.winCount,
        lossCount: outcome === 'LOST' ? profile.lossCount + 1 : profile.lossCount
      };
      saveUserProfileChanged(up);
    }
  };

  // Toast notifier
  const triggerToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  };

  const refreshLobby = () => {
    setLobbyRooms([
      {
        id: "room_liam",
        roomName: "London Pub Trivia 🌍",
        creator: BOT_OPPONENTS[0],
        status: "WAITING_FOR_PLAYERS",
        chatMessages: [
          { id: "c1", senderId: BOT_OPPONENTS[0].id, senderName: BOT_OPPONENTS[0].username, senderEmoji: BOT_OPPONENTS[0].avatarEmoji, text: "Welcome mates! Anyone down for a proper country quiz? 🍻", timestamp: Date.now() - 50000 }
        ]
      },
      {
        id: "room_koki",
        roomName: "Quick Guess Tokyo ⚡",
        creator: BOT_OPPONENTS[2],
        status: "WAITING_FOR_PLAYERS",
        chatMessages: [
          { id: "c2", senderId: BOT_OPPONENTS[2].id, senderName: BOT_OPPONENTS[2].username, senderEmoji: BOT_OPPONENTS[2].avatarEmoji, text: "Hi! Let's play a high-speed guessing battle! 🚅", timestamp: Date.now() - 30000 }
        ]
      }
    ]);
  };

  // Trigger scroll to chat bottom
  useEffect(() => {
    if (chatBottomRef.current) {
      chatBottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [activeRoom?.chatMessages, opponentTyping]);

  // LOGIN HANDLERS
  const handleLoginSubmit = (usernameInput, avatarSelected) => {
    const formatted = usernameInput.trim();
    if (!formatted) {
      triggerToast("Passport nickname cannot be empty!");
      return;
    }
    const id = "user_" + formatted.toLowerCase().replace(/\s+/g, '_');
    const newProfile = { id, username: formatted, avatarEmoji: avatarSelected, winCount: 0, lossCount: 0 };
    saveUserProfileChanged(newProfile);
    setScreen('LOBBY');
  };

  const handleLogout = () => {
    localStorage.removeItem(SAVE_KEYS.USER);
    setProfile(null);
    setScreen('LOGIN');
  };

  // MULTIPLAYER ROOM CREATORS
  const handleLaunchCustomRoom = (name) => {
    if (!profile) return;
    const rName = name.trim() || `${profile.username}'s Arena 🏆`;
    const newRoom = {
      id: "room_" + Math.random().toString(36).substr(2, 9),
      roomName: rName,
      creator: profile,
      guest: null,
      status: "WAITING_FOR_PLAYERS",
      p1State: null,
      p2State: null,
      currentTurnPlayerId: "",
      winningPlayerId: null,
      chatMessages: [
        { id: "msg_init", senderId: "system", senderName: "Narrator", senderEmoji: "📢", text: "Room opened. Seeking online nodes over geographic nets..." }
      ],
      isOnline: true
    };

    setActiveRoom(newRoom);
    setScreen('ROOM');

    // Simulated matchmaking joining after 3 seconds
    setTimeout(() => {
      setActiveRoom(latest => {
        if (!latest || latest.guest || latest.status !== 'WAITING_FOR_PLAYERS') return latest;
        const bot = BOT_OPPONENTS[Math.floor(Math.random() * BOT_OPPONENTS.length)];
        return {
          ...latest,
          guest: bot,
          status: "SETTING_COUNTRIES",
          chatMessages: [
            ...latest.chatMessages,
            { id: "m2", senderId: "system", senderName: "Narrator", senderEmoji: "📢", text: `${bot.username} entered the room fight!` },
            { id: "m3", senderId: bot.id, senderName: bot.username, senderEmoji: bot.avatarEmoji, text: "Hey! Ready to trade country secrets? Show me what you've got! 🌍" }
          ]
        };
      });
    }, 2500);
  };

  const handleJoinLobbyRoom = (selectedRoom) => {
    if (!profile) return;
    const updated = {
      ...selectedRoom,
      guest: profile,
      status: "SETTING_COUNTRIES",
      chatMessages: [
        ...selectedRoom.chatMessages,
        { id: "msg_p2_joined", senderId: "system", senderName: "Narrator", senderEmoji: "📢", text: `${profile.username} registered as the opponent.` },
        { id: "msg_p2_greet", senderId: profile.id, senderName: profile.username, senderEmoji: profile.avatarEmoji, text: "Geographic challenger logged in! Setting my passcode word. 🗺️" }
      ],
      isOnline: true
    };

    setActiveRoom(updated);
    setScreen('ROOM');

    // Bot sets their secret country immediately
    setTimeout(() => {
      setActiveRoom(latest => {
        if (!latest) return latest;
        const secretWord = countries[Math.floor(Math.random() * countries.length)];
        return {
          ...latest,
          p1State: { account: latest.creator, secretCountryName: secretWord, guessedLetters: [] },
          chatMessages: [
            ...latest.chatMessages,
            { id: "msg_p1_set", senderId: "system", senderName: "System", senderEmoji: "⚙️", text: `${latest.creator.username} secured their country target!` }
          ]
        };
      });
    }, 1500);
  };

  const handleStartVsCPU = () => {
    if (!profile) return;
    const bot = { id: "cpu_cosmo", username: "Cosmo AI", avatarEmoji: "⚙️" };
    const cpuTargetCountry = countries[Math.floor(Math.random() * countries.length)];
    
    const cpuRoom = {
      id: "room_cpu_" + Date.now(),
      roomName: `Cosmo AI Simulator [${singleDifficulty}]`,
      creator: profile,
      guest: bot,
      status: "SETTING_COUNTRIES",
      p1State: null,
      p2State: { account: bot, secretCountryName: cpuTargetCountry, guessedLetters: [] },
      currentTurnPlayerId: "",
      winningPlayerId: null,
      chatMessages: [
        { id: "msg_cpu_init", senderId: "system", senderName: "System", senderEmoji: "⚙️", text: `Connected directly with Cosmo AI Protocol [${singleDifficulty}]!` },
        { id: "msg_cpu_greet", senderId: bot.id, senderName: bot.username, senderEmoji: bot.avatarEmoji, text: `I have preloaded a secret country from our atlas coordinates. Enter your secret country code-word below!` }
      ],
      isOnline: false
    };

    setActiveRoom(cpuRoom);
    setScreen('ROOM');
  };

  // SECRETS SUBMISSIONS
  const handleLockInCountry = (wordInput) => {
    if (!activeRoom || !profile) return;
    const check = validateCountry(wordInput);
    
    if (check.status === 'NOT_FOUND') {
      triggerToast("This doesn't seem to be a recognized country name! Check spelling.");
      return;
    }
    if (check.status === 'SPELLING_ERROR') {
      triggerToast(`Did you mean "${check.suggestions}"? Check spelling!`);
      return;
    }

    const officialWord = check.name;
    const isHost = activeRoom.creator.id === profile.id;

    setActiveRoom(latest => {
      if (!latest) return latest;
      const mine = { account: profile, secretCountryName: officialWord, guessedLetters: [] };
      const nextRoom = isHost 
        ? { ...latest, p1State: mine } 
        : { ...latest, p2State: mine };

      // Append system message
      nextRoom.chatMessages = [
        ...nextRoom.chatMessages,
        { id: "msg_user_lock", senderId: "system", senderName: "System", senderEmoji: "⚙️", text: `${profile.username} locked their country! 🎯 [Secret Length: ${officialWord.length}]` }
      ];

      return checkGameplayInitiate(nextRoom);
    });
  };

  const checkGameplayInitiate = (roomState) => {
    // If both players have registered their targets, kick off!
    if (roomState.p1State && roomState.p2State) {
      const turnHolder = Math.random() > 0.5 ? roomState.creator.id : roomState.guest.id;
      
      return {
        ...roomState,
        status: "PLAYING",
        currentTurnPlayerId: turnHolder,
        chatMessages: [
          ...roomState.chatMessages,
          { id: "msg_g_start", senderId: "system", senderName: "Narrator", senderEmoji: "📢", text: "TARGET SELECTIONS CONCLUDED. MATCH LOGGED IN PLAYSTATE." },
          { id: "msg_turn_alert", senderId: "system", senderName: "System", senderEmoji: "📢", text: `${turnHolder === profile.id ? 'Your turn to guess a character' : "Opponent's turn to guess"}` }
        ]
      };
    }
    
    // In multiplayer context, if host locked, check if bot guest needs to lock too
    const isHost = roomState.creator.id === profile.id;
    if (isHost && roomState.guest && roomState.guest.id.startsWith('bot_') && !roomState.p2State) {
      setTimeout(() => {
        setActiveRoom(current => {
          if (!current || current.p2State) return current;
          const randomSecret = countries[Math.floor(Math.random() * countries.length)];
          const guestState = { account: current.guest, secretCountryName: randomSecret, guessedLetters: [] };
          const nextVal = {
            ...current,
            p2State: guestState,
            chatMessages: [
              ...current.chatMessages,
              { id: "msg_bot_lock", senderId: "system", senderName: "System", senderEmoji: "⚙️", text: `${current.guest.username} locked their secret country target! 🎯` }
            ]
          };
          return checkGameplayInitiate(nextVal);
        });
      }, 1500);
    }

    return roomState;
  };

  // PLAYTIME CONTROLLERS
  const handleGuessCharacter = (char) => {
    if (!activeRoom || !profile) return;
    const isHost = activeRoom.creator.id === profile.id;
    const myParticipant = isHost ? activeRoom.p1State : activeRoom.p2State;
    const opponentParticipant = isHost ? activeRoom.p2State : activeRoom.p1State;
    const targetSecret = opponentParticipant.secretCountryName;

    if (activeRoom.currentTurnPlayerId !== profile.id || activeRoom.status !== 'PLAYING') return;

    // Check if letter was already guessed
    if (myParticipant.guessedLetters.includes(char.toLowerCase())) return;

    const lowerChar = char.toLowerCase();
    const updatedGuessed = [...myParticipant.guessedLetters, lowerChar];
    const isCorrect = targetSecret.toLowerCase().includes(lowerChar);

    setActiveRoom(latest => {
      if (!latest) return latest;
      const me = isHost ? latest.p1State : latest.p2State;
      const updatedParticipant = { ...me, guessedLetters: updatedGuessed };
      let newRoom = isHost 
        ? { ...latest, p1State: updatedParticipant } 
        : { ...latest, p2State: updatedParticipant };

      newRoom.chatMessages = [
        ...newRoom.chatMessages,
        { id: "char_g_" + Date.now(), senderId: "system", senderName: "Referee", senderEmoji: "🎯", text: `${profile.username} guessed '${char.toUpperCase()}' — ${isCorrect ? 'SUCCESS!' : 'MISS!'}` }
      ];

      return handleCheckMatchEndConditions(newRoom, profile.id);
    });
  };

  const handlePredictCountryName = (e) => {
    e.preventDefault();
    if (!activeRoom || !profile) return;
    const isHost = activeRoom.creator.id === profile.id;
    const opponentParticipant = isHost ? activeRoom.p2State : activeRoom.p1State;
    const absoluteTarget = opponentParticipant.secretCountryName;
    const sanitizedGuess = predictText.trim().toLowerCase();

    if (activeRoom.currentTurnPlayerId !== profile.id || activeRoom.status !== 'PLAYING') return;

    setShowPredictModal(false);
    setPredictText('');

    if (sanitizedGuess === absoluteTarget.toLowerCase()) {
      // Direct exact guess wins the duel!
      setActiveRoom(latest => {
        if (!latest) return latest;
        let finalRoom = {
          ...latest,
          chatMessages: [
            ...latest.chatMessages,
            { id: "pred_win_" + Date.now(), senderId: "system", senderName: "Narrator", senderEmoji: "🏆", text: `${profile.username} correctly predicted the full country name: "${absoluteTarget.toUpperCase()}"!` }
          ]
        };
        return markWinningMatch(finalRoom, profile.id);
      });
    } else {
      // Missed prediction forfeits current turn, system alerts
      setActiveRoom(latest => {
        if (!latest) return latest;
        let penalRoom = {
          ...latest,
          chatMessages: [
            ...latest.chatMessages,
            { id: "pred_fail_" + Date.now(), senderId: "system", senderName: "Referee", senderEmoji: "❌", text: `${profile.username} predicted "${predictText.toUpperCase()}" but it's INCORRECT!` }
          ]
        };
        return toggleCurrentTurn(penalRoom);
      });
      triggerToast("Incorrect target prediction! Turn forfeited.");
    }
  };

  // Active turn swapper
  const toggleCurrentTurn = (roomRef) => {
    if (roomRef.status !== 'PLAYING') return roomRef;
    const opponent = roomRef.creator.id === profile.id ? roomRef.guest : roomRef.creator;
    const nextTurnHolder = roomRef.currentTurnPlayerId === profile.id ? opponent.id : profile.id;
    
    const outcome = {
      ...roomRef,
      currentTurnPlayerId: nextTurnHolder,
      chatMessages: [
        ...roomRef.chatMessages,
        { id: "turn_rotate_" + Date.now(), senderId: "system", senderName: "System", senderEmoji: "📢", text: `Next turn rotating to: ${nextTurnHolder === profile.id ? 'Your Turn' : opponent.username}` }
      ]
    };

    // If turn has swapped to bot/computer opponent, trigger AI action
    if (nextTurnHolder === opponent.id) {
      triggerOpponentAISimulation(outcome);
    }

    return outcome;
  };

  const handleCheckMatchEndConditions = (roomRef, playerId) => {
    const isHost = playerId === roomRef.creator.id;
    const selfParticipant = isHost ? roomRef.p1State : roomRef.p2State;
    const targetSecretName = (isHost ? roomRef.p2State : roomRef.p1State).secretCountryName;

    // Check if player solved the puzzle fully
    const cleanedSecret = targetSecretName.toLowerCase().replace(/\s+/g, '');
    const isSolved = cleanedSecret.split('').every(c => selfParticipant.guessedLetters.includes(c));

    if (isSolved) {
      return markWinningMatch(roomRef, playerId);
    } else {
      // Rotate turn if target is not solved fully
      return toggleCurrentTurn(roomRef);
    }
  };

  const markWinningMatch = (roomRef, winnerId) => {
    const winnerName = winnerId === roomRef.creator.id ? roomRef.creator.username : roomRef.guest.username;
    const isOwnVictory = winnerId === profile.id;
    const secretWord = winnerId === roomRef.creator.id ? roomRef.p2State.secretCountryName : roomRef.p1State.secretCountryName;
    const opponent = winnerId === roomRef.creator.id ? roomRef.guest : roomRef.creator;

    setTimeout(() => {
      recordMatchOutcome(
        isOwnVictory ? 'WON' : 'LOST',
        opponent.username,
        opponent.avatarEmoji,
        secretWord,
        roomRef.isOnline
      );
    }, 100);

    return {
      ...roomRef,
      status: "FINISHED",
      currentTurnPlayerId: "",
      winningPlayerId: winnerId,
      winnerAnnouncement: `${winnerName} has solved the coordinates first and stands victorious in this duel! Secret country was indeed "${secretWord.toUpperCase()}".`
    };
  };

  // CHAT ROUTERS & SUBMISSON FEEDBACK
  const handleSendChatMessage = (text) => {
    if (!activeRoom || !profile) return;
    const message = {
      id: "msg_" + Date.now(),
      senderId: profile.id,
      senderName: profile.username,
      senderEmoji: profile.avatarEmoji,
      text: text,
      timestamp: Date.now()
    };

    const nextRoom = {
      ...activeRoom,
      chatMessages: [...activeRoom.chatMessages, message]
    };
    setActiveRoom(nextRoom);

    // Trigger AI / Bot chatbot answers
    const bot = activeRoom.creator.id === profile.id ? activeRoom.guest : activeRoom.creator;
    if (bot && (bot.id.startsWith("bot_") || bot.id === "cpu_cosmo")) {
      handleTriggerCounterChat(nextRoom, text, bot);
    }
  };

  const handleTriggerCounterChat = (roomState, lastUserText, botAccount) => {
    setOpponentTyping(true);
    
    const isHost = botAccount.id === roomState.creator.id;
    const botParticipant = isHost ? roomState.p1State : roomState.p2State;
    const userParticipant = isHost ? roomState.p2State : roomState.p1State;
    const opponentSecretWord = userParticipant ? userParticipant.secretCountryName : "none";
    const guessed = botParticipant ? botParticipant.guessedLetters : [];

    setTimeout(async () => {
      try {
        const replyText = await generateGameChatResponse({
          username: botAccount.username,
          opponentSecret: opponentSecretWord,
          guessedLetters: guessed,
          lastUserMessage: lastUserText,
          chatHistory: roomState.chatMessages,
          customApiKey: customKey
        });

        setActiveRoom(current => {
          if (!current || current.id !== roomState.id) return current;
          return {
            ...current,
            chatMessages: [
              ...current.chatMessages,
              {
                id: "reply_" + Date.now(),
                senderId: botAccount.id,
                senderName: botAccount.username,
                senderEmoji: botAccount.avatarEmoji,
                text: replyText,
                timestamp: Date.now()
              }
            ]
          };
        });
      } catch (err) {
        console.error(err);
      } finally {
        setOpponentTyping(false);
      }
    }, 1500);
  };

  // BOT GAMEPLAY AI PROCESSORS
  const triggerOpponentAISimulation = (roomState) => {
    const cpuBot = roomState.creator.id === profile.id ? roomState.guest : roomState.creator;
    const isHost = cpuBot.id === roomState.creator.id;
    const botState = isHost ? roomState.p1State : roomState.p2State;
    const userState = isHost ? roomState.p2State : roomState.p1State;
    const targetSecretLower = userState.secretCountryName.toLowerCase();

    setOpponentTyping(true);

    setTimeout(() => {
      setOpponentTyping(false);
      setActiveRoom(latest => {
        if (!latest || latest.currentTurnPlayerId !== cpuBot.id || latest.status !== 'PLAYING') return latest;

        // Choose letter using difficulty metrics
        let chosenLetter = 'e';
        const alphaRange = 'abcdefghijklmnopqrstuvwxyz'.split('');
        const unused = alphaRange.filter(c => !botState.guessedLetters.includes(c));

        if (unused.length === 0) return latest;

        let level = singleDifficulty;
        if (latest.isOnline) {
          level = 'MEDIUM'; // Normal lobby bots are balanced
        }

        const isHard = level === 'HARD';
        const isEasy = level === 'EASY';

        // Select coordinates based on intelligence level
        const hitCandidates = unused.filter(c => targetSecretLower.includes(c));
        const missCandidates = unused.filter(c => !targetSecretLower.includes(c));

        if (isHard && hitCandidates.length > 0) {
          // Hard always aims for 85% success on hit letters
          chosenLetter = Math.random() < 0.85 ? hitCandidates[Math.floor(Math.random() * hitCandidates.length)] : unused[Math.floor(Math.random() * unused.length)];
        } else if (isEasy) {
          // Easy makes random blind selections (65% miss skew)
          chosenLetter = Math.random() < 0.65 && missCandidates.length > 0 
            ? missCandidates[Math.floor(Math.random() * missCandidates.length)] 
            : unused[Math.floor(Math.random() * unused.length)];
        } else {
          // Medium standard balancing (approx 50/50 precision)
          chosenLetter = Math.random() < 0.5 && hitCandidates.length > 0 
            ? hitCandidates[Math.floor(Math.random() * hitCandidates.length)] 
            : unused[Math.floor(Math.random() * unused.length)];
        }

        const lowerCh = chosenLetter.toLowerCase();
        const nextGuessedList = [...botState.guessedLetters, lowerCh];
        const hit = targetSecretLower.includes(lowerCh);

        let innerRoom = {
          ...latest,
          p1State: isHost ? { ...latest.p1State, guessedLetters: nextGuessedList } : latest.p1State,
          p2State: !isHost ? { ...latest.p2State, guessedLetters: nextGuessedList } : latest.p2State,
          chatMessages: [
            ...latest.chatMessages,
            { id: "cpu_guess_" + Date.now(), senderId: "system", senderName: "Referee", senderEmoji: "⚙️", text: `${cpuBot.username} guessed letter "${chosenLetter.toUpperCase()}"! — ${hit? 'SUCCESS!' : 'MISS!'}` }
          ]
        };

        // Determine if bot makes full prediction
        const secretsSolved = targetSecretLower.replace(/\s+/g, '').split('').every(c => nextGuessedList.includes(c));
        
        // Random prediction chance for High level machines if 70% elements are guessed
        const progressCount = targetSecretLower.replace(/\s+/g, '').split('').filter(c => nextGuessedList.includes(c)).length;
        const progressRatio = progressCount / targetSecretLower.replace(/\s+/g, '').length;
        
        if (progressRatio > 0.7 && Math.random() < (isHard ? 0.6 : 0.25) && !secretsSolved) {
          // Bot predicts full word
          innerRoom.chatMessages.push({
            id: "cpu_predict_" + Date.now(),
            senderId: "system", senderName: "Narrator", senderEmoji: "🧠",
            text: `${cpuBot.username} predicts the full name coordinates are "${userState.secretCountryName.toUpperCase()}"!`
          });
          return markWinningMatch(innerRoom, cpuBot.id);
        }

        return handleCheckMatchEndConditions(innerRoom, cpuBot.id);
      });

    }, 2000);
  };

  const handleExitRoom = () => {
    setActiveRoom(null);
    setScreen('LOBBY');
  };

  return (
    <div className="min-h-screen bg-[#0A0E17] text-slate-100 flex flex-col selection:bg-[#00F0FF] selection:text-[#0A0E17]">
      {/* Toast Notice bar */}
      {toast && (
        <div className="fixed top-6 left-1/2 -translate-x-1/2 bg-cyber-variant border border-cyber-primary rounded-xl px-4 py-3 flex items-center space-x-3 shadow-2xl z-50 animate-bounce">
          <Sparkles className="text-cyber-primary w-5 h-5" />
          <span className="font-mono text-sm tracking-wide text-white">{toast}</span>
        </div>
      )}

      {/* LOGIN VIEW */}
      {screen === 'LOGIN' && (
        <div className="flex-1 flex flex-col justify-center items-center px-4 py-12 bg-gradient-to-b from-[#0A0E17] to-[#121824]">
          <div className="w-full max-w-md p-8 rounded-3xl bg-cyber-surface border border-slate-800 shadow-2xl text-center space-y-8">
            
            {/* Logo Title */}
            <div className="p-6 bg-cyber-variant/50 rounded-2xl border border-cyber-primary/20 neon-glow-primary">
              <h1 className="text-3xl font-extrabold text-cyber-primary tracking-widest font-orbitron">
                GUESS COUNTRY
              </h1>
              <p className="text-xs font-mono font-bold tracking-widest text-cyber-textSecondary mt-2">
                GEOGRAPHIC DUELS ARENA
              </p>
            </div>

            {/* Passport card box */}
            <form onSubmit={(e) => {
              e.preventDefault();
              const nickname = e.target.elements.nickname.value;
              const emoji = e.target.elements.emojiSelected.value;
              handleLoginSubmit(nickname, emoji);
            }} className="space-y-6 text-left">
              
              <div>
                <label className="block text-sm font-semibold tracking-wide text-cyber-textSecondary mb-2 font-mono">
                  Create Passcode Nickname
                </label>
                <input 
                  type="text" 
                  name="nickname"
                  maxLength={16}
                  required
                  placeholder="Enter nickname..."
                  className="w-full px-4 py-3 rounded-xl bg-cyber-variant border border-slate-700 text-white placeholder-slate-600 focus:outline-none focus:border-cyber-primary text-sm tracking-wide font-mono transition-colors"
                />
              </div>

              {/* Avatar Selector */}
              <div>
                <label className="block text-sm font-semibold tracking-wide text-cyber-textSecondary mb-3 font-mono">
                  Select Passport Emblem
                </label>
                <div className="grid grid-cols-5 gap-3">
                  {["🌎", "🎮", "🤠", "🦁", "🍕", "🚀", "🐼", "🌶️", "🦄", "⚽"].map((em, idx) => (
                    <label key={idx} className="cursor-pointer relative flex items-center justify-center">
                      <input 
                        type="radio" 
                        name="emojiSelected" 
                        value={em} 
                        defaultChecked={idx === 0}
                        className="peer sr-only" 
                      />
                      <div className="w-12 h-12 text-2xl flex items-center justify-center rounded-xl bg-cyber-variant border border-transparent peer-checked:bg-cyber-primary/10 peer-checked:border-cyber-primary hover:bg-[#1E293B]/60 transition-all">
                        {em}
                      </div>
                    </label>
                  ))}
                </div>
              </div>

              {/* API KEY CONFIGURATION */}
              <div className="pt-2">
                <details className="cursor-pointer text-xs font-mono text-cyber-textSecondary group">
                  <summary className="hover:text-cyber-primary transition-colors focus:outline-none">
                    Configure Gemini API Integration (Optional)
                  </summary>
                  <div className="mt-3 cursor-default space-y-2">
                    <p className="leading-relaxed scale-95 text-slate-500">
                      Provide a personal Gemini API Key if you want high-quality generated dialog banter from bot opponents. Fallbacks will apply if blank.
                    </p>
                    <input 
                      type="password" 
                      placeholder="AI Studio API Key..."
                      value={customKey}
                      onChange={(e) => setCustomKey(e.target.value)}
                      className="w-full px-3 py-2 rounded-lg bg-cyber-variant border border-slate-800 text-white text-xs placeholder-slate-750 focus:outline-none focus:border-cyber-primary tracking-widest"
                    />
                  </div>
                </details>
              </div>

              <button 
                type="submit"
                className="w-full py-4 bg-cyber-primary hover:bg-[#00D8E6] text-cyber-background rounded-xl font-bold font-mono tracking-widest text-sm transition-transform active:scale-[0.98] flex items-center justify-center space-x-2"
              >
                <Compass className="w-5 h-5" />
                <span>ENTER DUEL ARENA</span>
              </button>
            </form>
          </div>
        </div>
      )}

      {/* LOBBY VIEW */}
      {screen === 'LOBBY' && profile && (
        <div className="flex-1 flex flex-col h-screen overflow-hidden">
          {/* Header Card */}
          <header className="bg-cyber-surface border-b border-slate-850 py-4 px-6 flex items-center justify-between shadow-md shrink-0">
            <div className="flex items-center space-x-4">
              <div className="w-12 h-12 rounded-full bg-cyber-variant border-2 border-cyber-primary flex items-center justify-center text-2xl">
                {profile.avatarEmoji}
              </div>
              <div>
                <h2 className="text-base font-bold font-orbitron">{profile.username}</h2>
                <div className="flex items-center space-x-2 text-xs font-mono text-cyber-textSecondary mt-0.5">
                  <Trophy className="w-3.5 h-3.5 text-cyber-accent" />
                  <span>Wins: {profile.winCount} | Losses: {profile.lossCount}</span>
                </div>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <h1 className="text-sm font-black text-cyber-primary hidden md:inline-block tracking-wider font-orbitron mr-4">
                GUESS THE COUNTRY
              </h1>
              <button 
                onClick={handleLogout}
                className="p-3 bg-cyber-variant/80 hover:bg-cyber-incorrect/15 text-cyber-incorrect outline-none rounded-full transition-colors"
                title="Sign out Profile"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          </header>

          {/* Tab Button Segment */}
          <div className="w-full max-w-4xl mx-auto px-4 mt-6 shrink-0">
            <div className="flex bg-cyber-surface p-1 rounded-xl">
              <button 
                onClick={() => setLobbyTab(0)}
                className={`flex-1 py-3 text-xs font-mono font-bold tracking-wider rounded-lg transition-all ${lobbyTab === 0 ? 'bg-cyber-variant text-cyber-primary shadow-sm' : 'text-cyber-textSecondary hover:text-white'}`}
              >
                LIVE DETECTOR LOBBY
              </button>
              <button 
                onClick={() => setLobbyTab(1)}
                className={`flex-1 py-3 text-xs font-mono font-bold tracking-wider rounded-lg transition-all ${lobbyTab === 1 ? 'bg-cyber-variant text-cyber-primary shadow-sm' : 'text-cyber-textSecondary hover:text-white'}`}
              >
                DUEL RECORDS
              </button>
            </div>
          </div>

          {/* Lobby Content Scroll Area */}
          <main className="flex-1 overflow-y-auto max-w-4xl w-full mx-auto p-4 space-y-6">
            
            {lobbyTab === 0 ? (
              <div className="space-y-6">
                {/* Single Player setup Console */}
                <section className="bg-cyber-surface border border-cyber-secondary/30 rounded-2xl p-5 space-y-4">
                  <div className="flex items-center space-x-3">
                    <Cpu className="text-cyber-accent w-6 h-6" />
                    <h3 className="text-sm font-bold font-mono text-white">Single-Player Vs Cosmo AI</h3>
                  </div>
                  <p className="text-xs text-cyber-textSecondary leading-relaxed">
                    Challenge the computer system. Pick an intelligence difficulty matching your country coordinates vocabulary and spelling response times.
                  </p>

                  <div className="flex space-x-3 py-1">
                    {["EASY", "MEDIUM", "HARD"].map((diff) => {
                      const active = singleDifficulty === diff;
                      const borderCol = diff === 'EASY' ? 'border-cyber-primary' : diff === 'MEDIUM' ? 'border-cyber-accent' : 'border-cyber-incorrect';
                      const textCol = diff === 'EASY' ? 'text-cyber-primary' : diff === 'MEDIUM' ? 'text-cyber-accent' : 'text-cyber-incorrect';
                      const bgCol = diff === 'EASY' ? 'bg-cyber-primary/10' : diff === 'MEDIUM' ? 'bg-cyber-accent/10' : 'bg-cyber-incorrect/10';
                      
                      return (
                        <button 
                          key={diff}
                          onClick={() => setSingleDifficulty(diff)}
                          className={`flex-1 py-2 text-xs font-bold tracking-wider font-mono border rounded-lg transition-all ${active ? `${bgCol} ${borderCol} ${textCol}` : 'border-transparent bg-cyber-variant text-cyber-textSecondary'}`}
                        >
                          {diff}
                        </button>
                      );
                    })}
                  </div>

                  <button 
                    onClick={handleStartVsCPU}
                    className="w-full py-3 bg-cyber-secondary hover:bg-blue-500 text-white rounded-xl text-xs font-mono font-bold tracking-wider flex items-center justify-center space-x-2"
                  >
                    <Play className="w-4 h-4 fill-current" />
                    <span>START COMPUTER MATCH</span>
                  </button>
                </section>

                {/* Available Duels list */}
                <section className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <Globe className="text-cyber-primary w-5 h-5 animate-spin" />
                      <h3 className="text-sm font-bold font-mono">Available Matchmaker Lobbies</h3>
                    </div>
                    <span className="text-xs font-mono text-cyber-primary bg-cyber-primary/10 px-2.5 py-1 rounded-full font-bold">
                      {lobbyRooms.length} Rooms Live
                    </span>
                  </div>

                  <button 
                    onClick={() => {
                      const input = prompt("Enter a name for your custom online room:");
                      if (input !== null) {
                        handleLaunchCustomRoom(input);
                      }
                    }}
                    className="w-full py-4.5 bg-cyber-variant hover:bg-cyber-variant/50 border border-dashed border-cyber-primary/30 rounded-xl text-xs font-mono font-bold tracking-widest text-cyber-primary flex items-center justify-center space-x-2 transition-colors"
                  >
                    <Plus className="w-4 h-4" />
                    <span>CREATE CUSTOM DUEL ROOM</span>
                  </button>

                  {lobbyRooms.length === 0 ? (
                    <div className="text-center py-12 text-cyber-textSecondary space-y-2">
                      <AlertTriangle className="w-8 h-8 mx-auto text-slate-600" />
                      <p className="text-xs font-mono">Connecting with active arena lobby services...</p>
                    </div>
                  ) : (
                    <div className="grid gap-3">
                      {lobbyRooms.map((room) => (
                        <div key={room.id} className="bg-cyber-surface border border-slate-800 rounded-xl p-4 flex items-center justify-between hover:border-slate-700 transition-colors">
                          <div className="space-y-2 max-w-[70%]">
                            <div className="flex items-center space-x-2">
                              <h4 className="text-sm font-bold text-white truncate max-w-full">{room.roomName}</h4>
                              <span className="text-[10px] font-black tracking-widest text-[#00F0FF] bg-[#00F0FF]/15 px-1.5 py-0.5 rounded">OPEN</span>
                            </div>
                            <div className="flex items-center space-x-2 text-xs text-cyber-textSecondary font-mono">
                              <span className="text-sm">{room.creator.avatarEmoji}</span>
                              <span className="truncate">Host: {room.creator.username}</span>
                            </div>
                          </div>
                          
                          <button 
                            onClick={() => handleJoinLobbyRoom(room)}
                            className="px-4 py-2 bg-cyber-primary hover:bg-[#00D8E6] text-cyber-background rounded-lg text-xs font-mono font-extrabold tracking-wider"
                          >
                            JOIN DUEL
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </section>
              </div>
            ) : (
              /* HISTORY RECORDS TAB */
              <div className="space-y-3">
                {history.length === 0 ? (
                  <div className="text-center py-16 bg-cyber-surface border border-slate-800 rounded-2xl text-cyber-textSecondary space-y-3">
                    <Info className="w-10 h-10 mx-auto text-slate-700" />
                    <h4 className="text-sm font-bold text-slate-400 font-mono">No duel logs available</h4>
                    <p className="text-xs text-slate-500">Compete against computer models or multiplayer lobbies to write records.</p>
                  </div>
                ) : (
                  history.map((hItem) => (
                    <div key={hItem.id} className="bg-cyber-surface rounded-xl p-4 flex items-center justify-between border border-slate-800">
                      <div className="flex items-center space-x-4">
                        <div className={`w-11 h-11 rounded-full flex items-center justify-center text-xl font-black ${hItem.outcome === 'WON' ? 'bg-cyber-correct/10 border border-cyber-correct' : 'bg-cyber-incorrect/10 border border-cyber-incorrect'}`}>
                          {hItem.outcome === 'WON' ? <Check className="text-cyber-correct w-5 h-5" /> : <X className="text-cyber-incorrect w-5 h-5" />}
                        </div>
                        <div>
                          <div className="flex items-center space-x-2 font-mono">
                            <span className={`text-xs font-bold tracking-widest ${hItem.outcome === 'WON' ? 'text-cyber-correct' : 'text-cyber-incorrect'}`}>
                              {hItem.outcome === 'WON' ? 'VICTORY' : 'DEFEAT'}
                            </span>
                            <span className="text-[10px] text-slate-500">• {hItem.isOnlineRoom ? 'Online Room' : 'vs Computer'}</span>
                          </div>
                          <h4 className="text-sm font-semibold text-slate-200 mt-1">
                            Opponent: {hItem.opponentEmoji} {hItem.opponentName}
                          </h4>
                          <p className="text-[11px] text-cyber-textSecondary mt-0.5">
                            Secret Target word: <span className="font-mono text-white">'{hItem.secretWord}'</span>
                          </p>
                        </div>
                      </div>
                      
                      <span className="text-[10px] font-mono text-slate-500 self-end">
                        {new Date(hItem.timestamp).toLocaleDateString()}
                      </span>
                    </div>
                  ))
                )}
              </div>
            )}
          </main>
        </div>
      )}

      {/* GAME ROOM SCREEN VIEW */}
      {screen === 'ROOM' && activeRoom && profile && (
        <div className="flex-1 flex flex-col h-screen overflow-hidden bg-[#0A0E17]">
          {/* Room Header Banner */}
          <header className="bg-cyber-surface border-b border-slate-800 py-3.5 px-4 flex items-center justify-between shrink-0">
            <div className="flex items-center space-x-1 max-w-[65%]">
              <button 
                onClick={handleExitRoom}
                className="p-2 hover:bg-slate-800 rounded-lg text-slate-400 hover:text-white"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <div className="ml-1 truncate">
                <h3 className="text-sm font-bold text-white truncate">{activeRoom.roomName}</h3>
                <span className="text-[10px] font-mono text-cyber-primary font-bold">
                  {activeRoom.status === 'PLAYING' ? 'DUEL IN PROGRESS' : 'SETTING LOG TARGETS'}
                </span>
              </div>
            </div>

            {/* Opponent Identity Card */}
            {activeRoom.guest && (
              <div className="bg-cyber-variant border border-slate-800 rounded-lg px-3 py-1 flex items-center space-x-2">
                <span className="text-base">
                  {activeRoom.creator.id === profile.id ? activeRoom.guest.avatarEmoji : activeRoom.creator.avatarEmoji}
                </span>
                <span className="text-xs font-mono font-bold text-slate-300 max-w-[100px] truncate">
                  {activeRoom.creator.id === profile.id ? activeRoom.guest.username : activeRoom.creator.username}
                </span>
              </div>
            )}
          </header>

          {/* SPLIT SCREEN BODY */}
          <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
            
            {/* STAGE A -- WAITING FOR matchmaking CONNECTION */}
            {activeRoom.status === 'WAITING_FOR_PLAYERS' && (
              <div className="flex-1 flex flex-col items-center justify-center p-6 text-center space-y-6">
                <div className="w-12 h-12 border-4 border-cyber-primary border-t-transparent rounded-full animate-spin"></div>
                <div className="space-y-4 max-w-sm">
                  <h4 className="text-sm font-bold font-mono tracking-widest text-cyber-primary font-orbitron">MATCHMAKING SYSTEM SCANNING...</h4>
                  <p className="text-xs text-cyber-textSecondary leading-relaxed">
                    Connecting node routes and looking for challenger bots in active database areas. This will take a brief moment.
                  </p>
                </div>
              </div>
            )}

            {/* STAGE B -- SELECTING / LOCKING TARGET WORDS */}
            {activeRoom.status === 'SETTING_COUNTRIES' && (
              <div className="flex-1 flex flex-col justify-center max-w-lg mx-auto p-6 space-y-6 text-center h-full overflow-y-auto">
                <div className="p-4 w-16 h-16 bg-cyber-accent/10 border border-cyber-accent rounded-2xl flex items-center justify-center mx-auto text-cyber-accent animate-pulse">
                  <Lock className="w-8 h-8" />
                </div>
                
                <div className="space-y-2">
                  <h3 className="text-lg font-bold font-orbitron">Set Opponent's Secret Country Word</h3>
                  <p className="text-xs text-cyber-textSecondary leading-relaxed">
                    Input a recognized country name. The system will lock it in coordinates. Your opponent must spell it out inside the active dueling grid!
                  </p>
                </div>

                {/* Submitting Inputs */}
                {!(activeRoom.creator.id === profile.id ? activeRoom.p1State : activeRoom.p2State)?.secretCountryName ? (
                  <form onSubmit={(e) => {
                    e.preventDefault();
                    handleLockInCountry(e.target.elements.countryWord.value);
                    e.target.reset();
                  }} className="space-y-4 text-left">
                    <input 
                      type="text" 
                      name="countryWord"
                      required
                      placeholder="e.g. Netherlands, Switzerland..."
                      className="w-full px-4 py-3 rounded-xl bg-cyber-variant border border-slate-700 font-mono text-sm tracking-wide text-white focus:outline-none focus:border-cyber-primary"
                    />
                    <button 
                      type="submit"
                      className="w-full py-3.5 bg-cyber-primary hover:bg-[#00D8E6] text-cyber-background rounded-xl text-xs font-mono font-black tracking-widest"
                    >
                      LOCK TARGET COORDINATE
                    </button>
                  </form>
                ) : (
                  <div className="bg-cyber-surface border border-cyber-correct/20 rounded-2xl p-6 space-y-4">
                    <Check className="w-8 h-8 text-cyber-correct mx-auto" />
                    <h4 className="text-xs font-bold font-mono text-cyber-correct">Target Locked in Coordinates Slot</h4>
                    <p className="text-xs text-slate-400">
                      Target selection: <span className="text-white font-mono font-bold">"{(activeRoom.creator.id === profile.id ? activeRoom.p1State : activeRoom.p2State).secretCountryName.toUpperCase()}"</span>
                    </p>
                    <div className="flex items-center justify-center space-x-2 text-[10px] font-mono text-slate-500">
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      <span>Awaiting opponent setup...</span>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* STAGE C -- PLAYING & FINISHED (MAIN SPLIT LAYOUT) */}
            {(activeRoom.status === 'PLAYING' || activeRoom.status === 'FINISHED') && (
              <>
                {/* 1. LEFT COLUMN -- THE GUESSING INTERFACE */}
                <div className="flex-1 flex flex-col p-4 space-y-5 overflow-y-auto border-r border-[#121824] h-full">
                  
                  {/* TURN HEADER BANNER */}
                  {activeRoom.status === 'PLAYING' ? (
                    <div className={`p-3 rounded-xl flex items-center justify-between border ${activeRoom.currentTurnPlayerId === profile.id ? 'bg-cyber-primary/5 border-cyber-primary/20' : 'bg-cyber-surface border-slate-800'}`}>
                      <div className="flex items-center space-x-2">
                        <span className={`w-2.5 h-2.5 rounded-full ${activeRoom.currentTurnPlayerId === profile.id ? 'bg-cyber-primary animate-ping' : 'bg-slate-600'}`} />
                        <span className="text-xs font-mono font-bold">
                          {activeRoom.currentTurnPlayerId === profile.id ? 'YOUR TURN TO REVEAL COORDINATES' : 'OPPONENT DECODING TARGET...'}
                        </span>
                      </div>
                      
                      <button 
                        disabled={activeRoom.currentTurnPlayerId !== profile.id}
                        onClick={() => setShowPredictModal(true)}
                        className="px-3 py-1 bg-cyber-accent hover:bg-amber-500 disabled:opacity-40 text-cyber-background text-[10px] font-mono font-black rounded"
                      >
                        PREDICT FULL WORD
                      </button>
                    </div>
                  ) : (
                    <div className="p-4 rounded-xl text-center border border-slate-800 bg-cyber-surface">
                      <h4 className={`text-base font-black font-mono tracking-widest ${activeRoom.winningPlayerId === profile.id ? 'text-cyber-correct' : 'text-cyber-incorrect'}`}>
                        {activeRoom.winningPlayerId === profile.id ? '🏆 DUEL VICTORY!' : '💀 DEFEAT ON CODES'}
                      </h4>
                      <p className="text-xs text-slate-300 mt-2 leading-relaxed">
                        {activeRoom.winnerAnnouncement}
                      </p>
                      <button 
                        onClick={handleExitRoom}
                        className="mt-4 px-4 py-2 bg-cyber-primary hover:bg-[#00D8E6] text-cyber-background text-xs font-mono font-bold rounded-lg"
                      >
                        Back to Lobby
                      </button>
                    </div>
                  )}

                  {/* SECRET WRAPPED SLOTS */}
                  <div className="space-y-2 text-center py-2.5">
                    <span className="text-[10px] font-mono font-bold tracking-widest text-cyber-textSecondary">OPPONENT'S SECRETS MATRIX:</span>
                    
                    <div className="bg-[#121824]/50 border border-slate-850 rounded-2xl py-6 px-4 flex flex-wrap items-center justify-center gap-1.5 min-h-[90px]">
                      {(() => {
                        const targetCountryName = (activeRoom.creator.id === profile.id ? activeRoom.p2State : activeRoom.p1State).secretCountryName;
                        const myGLetters = (activeRoom.creator.id === profile.id ? activeRoom.p1State : activeRoom.p2State).guessedLetters;
                        
                        return targetCountryName.split('').map((char, index) => {
                          if (char.trim() === '') {
                            return <div key={index} className="w-6 h-10 border-b border-transparent" />;
                          }
                          const decoded = myGLetters.includes(char.toLowerCase()) || activeRoom.status === 'FINISHED';
                          return (
                            <div key={index} className="flex flex-col items-center mx-0.5">
                              <span className={`text-2xl font-black font-mono transition-colors duration-300 ${decoded ? 'text-cyber-primary scale-105' : 'text-slate-700'}`}>
                                {decoded ? char.toUpperCase() : '?'}
                              </span>
                              <div className={`w-5 h-1 mt-1 rounded-full ${decoded ? 'bg-cyber-primary shadow-sm' : 'bg-slate-700'}`} />
                            </div>
                          );
                        });
                      })()}
                    </div>
                  </div>

                  {/* ACTIVE SPELLING ALPHABET GRID */}
                  {activeRoom.status === 'PLAYING' && (
                    <div className="space-y-2">
                      <span className="text-[10px] font-mono font-bold tracking-widest text-[#94A3B8] block">CLICK ON-SCREEN CHARACTER KEYS:</span>
                      <div className="grid grid-cols-7 gap-2">
                        {'abcdefghijklmnopqrstuvwxyz'.split('').map((char) => {
                          const isHost = activeRoom.creator.id === profile.id;
                          const selfParticipant = isHost ? activeRoom.p1State : activeRoom.p2State;
                          const opponentParticipant = isHost ? activeRoom.p2State : activeRoom.p1State;
                          const isGuessed = selfParticipant.guessedLetters.includes(char);
                          const inSecret = opponentParticipant.secretCountryName.toLowerCase().includes(char);

                          let keyColor = 'bg-cyber-surface border-slate-800 text-white hover:border-cyber-primary';
                          if (isGuessed) {
                            keyColor = inSecret 
                              ? 'bg-cyber-correct/10 border-cyber-correct text-cyber-correct' 
                              : 'bg-cyber-incorrect/5 border-cyber-incorrect/30 text-cyber-incorrect/35 cursor-not-allowed';
                          }

                          return (
                            <button 
                              key={char}
                              disabled={isGuessed || activeRoom.currentTurnPlayerId !== profile.id}
                              onClick={() => handleGuessCharacter(char)}
                              className={`aspect-square rounded-xl border text-sm font-bold font-mono transition-all flex items-center justify-center shrink-0 active:scale-95 disabled:active:scale-100 ${keyColor}`}
                            >
                              {char.toUpperCase()}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>

                {/* 2. RIGHT COLUMN -- CHAT LOGS */}
                <div className="w-full md:w-[360px] bg-cyber-surface border-t md:border-t-0 md:border-l border-slate-850 flex flex-col h-[340px] md:h-full overflow-hidden">
                  <div className="bg-[#121824]/60 px-4 py-2 flex items-center space-x-2 border-b border-slate-800">
                    <MessageSquare className="w-4 h-4 text-cyber-primary" />
                    <span className="text-[10px] font-mono font-black tracking-widest text-slate-300">DUEL TEAM CHANNEL</span>
                  </div>

                  {/* messages body */}
                  <div className="flex-1 overflow-y-auto p-4 space-y-3">
                    {activeRoom.chatMessages.map((msg) => {
                      const own = msg.senderId === profile.id;
                      const system = msg.senderId === 'system';
                      
                      if (system) {
                        return (
                          <div key={msg.id} className="text-center">
                            <span className="inline-block text-[10px] font-mono text-cyber-accent bg-cyber-accent/15 border border-cyber-accent/10 px-2.5 py-1 rounded-md max-w-[90%] leading-normal">
                              {msg.senderEmoji} {msg.text}
                            </span>
                          </div>
                        );
                      }

                      return (
                        <div key={msg.id} className={`flex items-start gap-2.5 ${own ? 'flex-row-reverse' : ''}`}>
                          <div className="w-8 h-8 rounded-full bg-cyber-variant border border-slate-700 flex items-center justify-center text-base shrink-0">
                            {msg.senderEmoji}
                          </div>
                          
                          <div className={`max-w-[70%] rounded-xl p-2.5 text-xs ${own ? 'bg-cyber-secondary text-white rounded-tr-none' : 'bg-cyber-variant text-slate-200 rounded-tl-none'}`}>
                            <span className="font-mono text-[9px] text-[#00F0FF] block mb-1 leading-none">{msg.senderName}</span>
                            <p className="leading-snug">{msg.text}</p>
                          </div>
                        </div>
                      );
                    })}

                    {opponentTyping && (
                      <div className="flex items-start gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-cyber-variant border border-slate-700 flex items-center justify-center text-base animate-bounce">
                          🌍
                        </div>
                        <div className="bg-cyber-variant rounded-xl rounded-tl-none p-3 text-xs w-[60px] flex justify-center items-center gap-1">
                          <span className="w-1.5 h-1.5 bg-cyber-primary rounded-full animate-bounce" />
                          <span className="w-1.5 h-1.5 bg-cyber-primary rounded-full animate-bounce [animation-delay:0.2s]" />
                          <span className="w-1.5 h-1.5 bg-cyber-primary rounded-full animate-bounce [animation-delay:0.4s]" />
                        </div>
                      </div>
                    )}
                    <div ref={chatBottomRef} />
                  </div>

                  {/* input form footer */}
                  <form onSubmit={(e) => {
                    e.preventDefault();
                    const text = e.target.elements.messageText.value.trim();
                    if (text) {
                      handleSendChatMessage(text);
                    }
                    e.target.reset();
                  }} className="p-3 border-t border-slate-800 bg-[#0A0E17] flex items-center gap-2">
                    <input 
                      type="text" 
                      name="messageText"
                      required
                      placeholder="Send chat..."
                      autoComplete="off"
                      className="flex-1 px-3 py-2 text-xs rounded-lg bg-cyber-variant text-white border border-slate-800 placeholder-slate-600 focus:outline-none focus:border-cyber-primary"
                    />
                    <button 
                      type="submit"
                      className="p-2 bg-cyber-primary hover:bg-[#00D8E6] text-cyber-background rounded-lg transition-transform active:scale-95 shrink-0"
                    >
                      <Send className="w-4 h-4 fill-current" />
                    </button>
                  </form>
                </div>
              </>
            )}
          </div>

          {/* D. DIALOG MODAL FOR PREDICTING THE FULL COORDINATE */}
          {showPredictModal && (
            <div className="fixed inset-0 bg-[#0A0E17]/80 backdrop-blur-sm flex items-center justify-center p-6 z-40">
              <div className="w-full max-w-sm bg-cyber-surface border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-200">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2 text-cyber-accent">
                    <Sparkles className="w-5 h-5" />
                    <h4 className="text-sm font-bold font-mono">Predict Full Country Word</h4>
                  </div>
                  <button 
                    onClick={() => setShowPredictModal(false)}
                    className="p-1 hover:bg-slate-800 rounded text-slate-400 hover:text-white"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                <p className="text-xs text-cyber-textSecondary leading-normal">
                  You are guessing the full country secret. A correct prediction wins the duel instantly, but an incorrect target spelling forfeits your turn.
                </p>

                <form onSubmit={handlePredictCountryName} className="space-y-4">
                  <input 
                    type="text"
                    required
                    value={predictText}
                    onChange={(e) => setPredictText(e.target.value)}
                    placeholder="Enter whole country name guess..."
                    className="w-full px-3 py-2.5 rounded-xl bg-cyber-variant border border-slate-700 font-mono text-xs text-white focus:outline-none focus:border-cyber-accent"
                  />
                  <div className="flex items-center gap-3">
                    <button 
                      type="button"
                      onClick={() => setShowPredictModal(false)}
                      className="flex-1 py-2 rounded-lg bg-slate-850 hover:bg-slate-800 text-xs text-slate-400 font-bold"
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit"
                      className="flex-1 py-2 rounded-lg bg-cyber-accent hover:bg-amber-500 text-cyber-background text-xs font-bold font-mono tracking-widest"
                    >
                      SUBMIT GUESS
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
