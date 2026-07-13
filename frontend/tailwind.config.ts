import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        background: "#F7F8FA",
        surface: "#FFFFFF",
        border: "#E5E7EB",
        text: "#111827",
        muted: "#6B7280",
        primary: "#0F766E",
        success: "#16A34A",
        warning: "#D97706",
        danger: "#DC2626"
      },
      borderRadius: {
        card: "8px"
      },
      boxShadow: {
        soft: "0 8px 18px rgba(17, 24, 39, 0.05)"
      }
    }
  },
  plugins: []
} satisfies Config;
