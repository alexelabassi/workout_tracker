import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev server proxies API calls to the Spring Boot backend so `npm run dev`
// works against a locally running backend. The production build is emitted to
// `dist/` and bundled into the Spring Boot jar (served at http://localhost:8080).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  build: {
    outDir: "dist",
  },
});
