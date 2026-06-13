// client-side Gemini 3.5 Flash REST API Integration
const BASE_URL = "https://generativelanguage.googleapis.com/";

function getContinentHint(countryName) {
  const c = countryName.toLowerCase().trim();
  if (["india", "china", "japan", "indonesia", "pakistan", "vietnam", "thailand", "philippines", "singapore", "south korea", "malaysia"].includes(c)) {
    return "Asia";
  }
  if (["france", "germany", "italy", "spain", "united kingdom", "sweden", "norway", "greece", "belgium", "poland", "switzerland"].includes(c)) {
    return "Europe";
  }
  if (["egypt", "nigeria", "south africa", "kenya", "morocco", "ghana", "ethiopia", "algeria"].includes(c)) {
    return "Africa";
  }
  if (["united states", "canada", "mexico", "brazil", "argentina", "colombia", "peru", "chile"].includes(c)) {
    return "the Americas";
  }
  return "our awesome planet";
}

function getFallbackResponse(username, opponentSecret, guessedLetters, lastUserMessage) {
  const lowerMsg = lastUserMessage.toLowerCase();
  
  if (lowerMsg.includes("hello") || lowerMsg.includes("hi") || lowerMsg.includes("hey")) {
    return [
      "Hey there! Let's see who's better at guessing today! 🌍",
      "Hey! Hope you're ready to lose! Just kidding, good luck! 😄",
      "What's up! Ready to roll? Pick a good first letter!"
    ][Math.floor(Math.random() * 3)];
  }

  if (lowerMsg.includes("hint") || lowerMsg.includes("help") || lowerMsg.includes("clue")) {
    const len = opponentSecret.length;
    const firstChar = opponentSecret[0] || '?';
    const continentHint = getContinentHint(opponentSecret);
    return [
      `Hmm, I shouldn't tell you, but it starts with "${firstChar.toUpperCase()}" and has ${len} letters! 😉`,
      `Here is a small clue: I think it's located in ${continentHint}! 🗺️`,
      "No spoilers, but it's a super cool country! Try guessing a vowel!"
    ][Math.floor(Math.random() * 3)];
  }

  if (lowerMsg.includes("who are you") || lowerMsg.includes("your name")) {
    return `I'm ${username}, your worthy opponent! Bring it on! ⚔️`;
  }

  if (lowerMsg.includes("hard") || lowerMsg.includes("difficult")) {
    return "Oh, definitely! This is high-IQ gameplay right here. 🧠";
  }

  if (lowerMsg.includes("win") || lowerMsg.includes("winner")) {
    return "We will see about that! One lucky letter can change everything! 🎲";
  }

  return [
    "Nice one! Your turn is coming up, watch out! 😄",
    "Hmm, interesting move! What letter is next? 🌍",
    "Let's see if you can solve this one. It's a tricky country! 🤔",
    "You're playing well! My turn is going to be epic.",
    "Haha, this is awesome! I love guessing countries! ✈️"
  ][Math.floor(Math.random() * 5)];
}

export async function generateGameChatResponse({
  username,
  opponentSecret,
  guessedLetters,
  lastUserMessage,
  chatHistory,
  customApiKey = null
}) {
  // Check primary VITE environment variable or manual input key
  const apiKey = customApiKey || import.meta.env.VITE_GEMINI_API_KEY || "";

  if (!apiKey || apiKey === "MY_GEMINI_API_KEY") {
    // Elegant fallback simulation
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(getFallbackResponse(username, opponentSecret, guessedLetters, lastUserMessage));
      }, 1000);
    });
  }

  const guessedText = guessedLetters.length === 0 ? "none" : guessedLetters.join(", ");
  const mask = opponentSecret
    .split("")
    .map((char) => {
      if (char.trim() === "") return " ";
      return guessedLetters.includes(char.toLowerCase()) ? char.toUpperCase() : "_";
    })
    .join(" ");

  const systemPrompt = `
    You are a human online player named ${username} playing a "Guess the Country" game.
    The opponent's secret country is "${opponentSecret}". This country is represented on your opponent's screen as: ${mask}
    Letters guessed so far: [${guessedText}].
    Respond naturally to the last message of the user. Keep your message short (1 to 2 sentences max).
    Talk like an enthusiastic, friendly gamer. Feel free to use gamer slang, friendly banter, or emojis.
    Do NOT reveal the country name directly. You can give a subtle hint if they ask, but do not spoil it!
  `;

  const contextPrompt = chatHistory
    .slice(-6)
    .map((msg) => `${msg.senderName}: ${msg.text}`)
    .join("\n") + `\nYou (as ${username}): `;

  const requestBody = {
    contents: [
      {
        parts: [
          {
            text: `The chat conversation history:\n${contextPrompt}\nLast user message: ${lastUserMessage}\nYour next message:`
          }
        ]
      }
    ],
    systemInstruction: {
      parts: [
        {
          text: systemPrompt
        }
      ]
    }
  };

  try {
    const response = await fetch(`${BASE_URL}v1beta/models/gemini-3.5-flash:generateContent?key=${apiKey}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
      throw new Error(`HTTP error ${response.status}`);
    }

    const data = await response.json();
    const generatedText = data?.candidates?.[0]?.content?.parts?.[0]?.text;
    
    if (generatedText) {
      return generatedText.trim();
    }
    return getFallbackResponse(username, opponentSecret, guessedLetters, lastUserMessage);
  } catch (error) {
    console.error("Gemini API Error in Webapp:", error);
    return getFallbackResponse(username, opponentSecret, guessedLetters, lastUserMessage);
  }
}
