import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

/**
 * Vite build config for the "sv" (Street Vending) module.
 * This module handles street vending features — applications, approvals, certificates, and vendor management.
 *
 * plugins   → Adds React support so JSX syntax works during build.
 * esbuild   → Treats .js files as JSX too (not just .jsx), so React code in .js files works fine.
 * build.lib → Builds this as a reusable library (not a standalone app).
 *             Entry: src/Module.js | Name: digitUiModule
 *             Output: index.modern.js (ES module only).
 * external  → These packages are NOT bundled — the host app is expected to have them already.
 *             Includes: react, react-dom, react-router-dom, react-i18next, i18next, @tanstack/react-query.
 *             Note: react-redux and redux are NOT in this list, so they get bundled into this module.
 * sourcemap → Off. No debug map files needed for a library build.
 * minify    → Off. Code stays readable (not compressed).
 * outDir    → Built files go into the "dist" folder.
 * emptyOutDir → Clears the "dist" folder before each build to remove old files.
 */
export default defineConfig({
  plugins: [react()],

  esbuild: {
    loader: "jsx",
    include: /.*\.js$/,
    exclude: [],
  },

  build: {
    lib: {
      entry: path.resolve(__dirname, "src/Module.js"),
      name: "digitUiModule",
      formats: ["es"],
      fileName: () => "index.modern.js",
    },

    rollupOptions: {
      external: [
        "react",
        "react-dom",
        "react-router-dom",
        "@tanstack/react-query",
        "react-i18next",
        "i18next"
      ],
    },

    sourcemap: false,
    minify: false,
    outDir: "dist",
    emptyOutDir: true,
  },
});