import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    allowedHosts: [
        'local.configurationmanager.alwaysmoveforward.com',
        'local.api.configurationmanager.alwaysmoveforward.com',
		'configurationmanager.alwaysmoveforward.com',
		'api.configurationmanager.alwaysmoveforward.com'
    ],
    hmr: {
        overlay: true,
    },
    watch: {
        usePolling: true,
    },
}
})

