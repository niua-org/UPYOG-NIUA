import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

/**
 * Vite build config for the "common" module.
 * This module holds shared components, hooks, and utilities used by all other modules.
 *
 * plugins       → Adds React support so JSX syntax works during build.
 * esbuild       → Treats .js files as JSX too (not just .jsx), so React code in .js files works fine.
 * build.lib     → Builds this as a reusable library (not a standalone app).
 *                 Entry: src/Module.js | Name: digitUiModule
 *                 Output: index.modern.js (ES module only).
 * external      → These packages are NOT bundled — the host app is expected to have them already.
 *                 Keeps the bundle small and avoids duplicate React copies.
 * manualChunks  → Disabled, so everything is packed into a single output file.
 * inlineDynamic → Any dynamic import() calls are merged into the main file instead of split out.
 * sourcemap     → Off. No debug map files needed for a library build.
 * minify        → Off. Code stays readable (not compressed).
 * outDir        → Built files go into the "dist" folder.
 * emptyOutDir   → Clears the "dist" folder before each build to remove old files.
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
        "react-redux",
        "redux",
        "react-i18next",
        "i18next",
        "@tanstack/react-query",
      ],
      output: {
        manualChunks: undefined,
        inlineDynamicImports: true,
      },
    },

    sourcemap: false,
    minify: false,
    outDir: "dist",
    emptyOutDir: true,
  },
});