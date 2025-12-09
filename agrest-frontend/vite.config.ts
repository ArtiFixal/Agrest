import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    https: {
      ca: '../certs/root/root-ca.crt',
      key: '../certs/frontend/agrest-frontend.key',
      cert: '../certs/frontend/agrest-frontend.pem',
      minVersion: 'TLSv1.3',
    },
    host: true,
    port: 5173,
  },
})
