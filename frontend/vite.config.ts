import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Lets the browser call /api on the same origin; the backend also allows CORS for :5173.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
