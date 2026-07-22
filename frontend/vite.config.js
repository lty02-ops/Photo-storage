import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Proxy API and OAuth2 routes to backend running on 8090
      '/api': 'http://localhost:8080',
      '/oauth2': 'http://localhost:8080'
    }
  }
})
