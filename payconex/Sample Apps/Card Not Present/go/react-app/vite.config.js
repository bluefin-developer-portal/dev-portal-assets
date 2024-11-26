import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 9104,
    proxy: {
      '/api': 'http://127.0.0.1:3000'
    }
  }
})
