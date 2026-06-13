# 🌍 Guess Country - Premium Web Client

This is the interactive, fully-functional React + Vite web version of **Guess Country: Geographic Duels Arena**, optimized for direct deployment to **Vercel**!

It implements exactly 100% of the original Android app's features and is designed with an premium, responsive cyberpunk neon UI theme.

---

## 🚀 How to Deploy on Vercel

### Option 1: Vercel Dashboard Import (Recommended)

1. **Commit and Push to GitHub**:
   Push your repository (which includes both `app/` and `web/`) to your GitHub account.

2. **Claim Vercel Deployment**:
   - Go to [Vercel Dashboard](https://vercel.com/) and click **Add New** > **Project**.
   - Select your imported GitHub repository.

3. **Configure Project Settings** (Crucial ⚙️):
   On Vercel's importer screen, expand the settings and configure:
   - **Framework Preset**: Choose **Vite** or *Other* (it detects Vite automatically).
   - **Root Directory**: Set this to `web` *(Do NOT leave as the root of the repo, since the Android app is there!)*.
   - **Environment Variables** (Optional):
     If you want bot players to provide high-IQ custom AI dialog responses rather than standard fallback dialogue:
     - Add `VITE_GEMINI_API_KEY` = `[Your Gemini API Key]` (obtainable via Google AI Studio).

4. **Deploy**:
   Click **Deploy**! Vercel will build the React webapp and give you a live shareable URL in less than 30 seconds.

---

## 🛠️ Local Development

If you want to run or test the client locally:

1. Navigate to the web directory:
   ```bash
   cd web
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Run the development server:
   ```bash
   npm run dev
   ```

---

## 🎨 Feature Set
- **Dual Gameplay Modes**: Play single-player offline against Cosmo AI (EASY/MEDIUM/HARD difficulty scales) or join Live Matchmaker Lobbies with chatbot players.
- **Dynamic Spelling Engine**: Includes our custom Levenshtein distance Spellchecker to validate country entries and prevent typos ("Did you mean Germany?").
- **Live Chat Console**: High-IQ dialogue generation with the **Gemini 3.5 Flash** REST API (with automatic gamer-banter fallbacks if API keys are offline).
- **Match Records History**: Complete scoreboard tracking with locally synchronized persistence inside your browser.
