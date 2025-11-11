import path from "path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    host: true, 
    allowedHosts: ['app.lucidity.sbs', 'localhost'],
    port: 5173,
    strictPort: true,
    cors: true,
    hmr: {
      protocol: 'wss',
      host: 'app.lucidity.sbs',
      clientPort: 443,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  }
})  