# 🌎 Guess Country - Dual Target System (Android & Web)

A premium cyber-punk geographic dueling game centered on country guessing with live chat, AI integration, and local scoreboards.

To meet your requirements perfectly, this project is configured into two distinct target environments:
1. **📱 Android App (`/app`)**: Powered by Jetpack Compose. Stays fully compliant with AI Studio's compile pipelines and emulator stream.
2. **💻 Web Client (`/web`)**: Powered by React, Vite, and Tailwind. Optimized for instant, single-click deployment to **Vercel** with static hosting!

---

## 🚀 Deploying to Vercel

You can deliver this app live on the web via Vercel in 30 seconds!

### Step-by-step Configuration:
1. Push this repository to your **GitHub** account.
2. Open your [Vercel Dashboard](https://vercel.com/) and click **Add New** > **Project**.
3. Select your repository.
4. **Important**: In the configuration card, locate the **Root Directory** field and set it to:
   ```text
   web
   ```
   *(This tells Vercel to build the modern React application rather than the mobile Android project)*.
5. Click **Deploy**! Vercel will host your application live at a free, secure HTTPS subdomain.

---

## 🪐 Core Features

- **Cyberpunk UI Theme**: Beautiful glowing borders, animated visual feedback, and crisp dark backgrounds.
- **Passcode Secrets Target**: Lock a secret country word that opponents must guess or spell to survive.
- **Dynamic Spelling Validation**: Built-in Levenshtein fuzzy matching algorithm validates and corrects spelling mistakes.
- **Cosmo AI and Room Lobbies**: Play single-player offline games against the computer with difficulty settings, or join pre-filled online client sessions.
- **Gemini Chat Integration**: Bot players reply with live chat messages using the **Gemini 3.5 Flash** API (with deterministically styled offline response templates as fallback).
