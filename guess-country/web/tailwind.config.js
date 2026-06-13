/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        cyber: {
          background: "#0A0E17",
          surface: "#121824",
          variant: "#1E293B",
          primary: "#00F0FF", // Cyan neon
          secondary: "#3B82F6", // Blue neon
          accent: "#F59E0B", // Orange neon
          correct: "#10B981", // Green neon
          incorrect: "#EF4444", // Red neon
          textPrimary: "#F8FAFC",
          textSecondary: "#94A3B8"
        }
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'Fira Code', 'monospace']
      }
    },
  },
  plugins: [],
}
