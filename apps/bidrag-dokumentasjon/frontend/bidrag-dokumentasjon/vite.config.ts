import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: "/bidrag-dokumentasjon/",
  optimizeDeps: {
    include: ["mermaid"],
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("node_modules/mermaid") || id.includes("node_modules/@mermaid")) {
            return "mermaid";
          }
        },
      },
    },
  },
})
