import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Pengembangan lokal: peramban hanya melihat origin Vite, /api diteruskan ke backend
// sehingga tidak perlu CORS (SAD-DDB-001 Tabel 8.1). Ganti target dengan env DDB_API bila perlu.
const target = process.env.DDB_API || "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { "/api": { target, changeOrigin: true } },
  },
  preview: {
    port: 4173,
    proxy: { "/api": { target, changeOrigin: true } },
  },
});
