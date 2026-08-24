import type { Config } from "tailwindcss";
import plugin from "tailwindcss/plugin";

export default {
  content: ["./app/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      padding: {
        "table-header-v1": "2px 5px 2px 5px",
        "table-body-v1": "2px 2px 2px 0",
        "table-body-tr-v1": "2px 2px 0 5px",
      },
      margin: {
        "section-title": "5px",
      },
    },
    colors: {
      "table-header": "#E6F0FF",
      "table-bg-even": "#F2F3F5",
      "neutral-soft": "#F2F3F5",
    },

    spacing: {
      "1": "4px",
      "2": "8px",
      "3": "12px",
      "4": "16px",
      "5": "20px",
      "6": "24px",
      "7": "28px",
      "8": "32px",
      "9": "36px",
      "10": "40px",
      "16": "64px",
    },
  },
  plugins: [
    plugin(function ({ addComponents }) {
      addComponents({
        ".section-title": {
          margin: "32px 0 0 0",
        },
      });
    }),
  ],
  corePlugins: {
    preflight: false,
  },
} satisfies Config;
