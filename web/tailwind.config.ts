import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        crypto: {
          bg: "#0a0e17",
          surface: "#111827",
          surface2: "#1f2937",
          accent: "#22c55e",
          "accent-hover": "#16a34a",
          red: "#ef4444",
          "red-hover": "#dc2626",
          blue: "#3b82f6",
          yellow: "#eab308",
          purple: "#a855f7",
          text: "#f9fafb",
          "text-muted": "#9ca3af",
          border: "#374151",
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "Fira Code", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
