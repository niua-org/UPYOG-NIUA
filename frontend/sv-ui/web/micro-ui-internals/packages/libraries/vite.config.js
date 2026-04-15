import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

/**
 * Vite build config for the "libraries" package.
 * This package contains shared hooks, API services, Redux utilities, and data-fetching helpers
 * used across all SV-UI modules.
 *
 * plugins   → Adds React support so JSX syntax works during build.
 * esbuild   → Treats .js files as JSX too (not just .jsx), so React code in .js files works fine.
 * build.lib → Builds this as a reusable library (not a standalone app).
 *             Entry: src/index.js | Name: digitUiLibraries
 *             Output: index.modern.js (ES module only).
 * external  → These packages are NOT bundled — the host app is expected to have them already.
 *             Includes: react, react-dom, react-router-dom, react-redux, redux,
 *             react-i18next, i18next, @tanstack/react-query.
 * manualChunks  → Disabled, so everything is packed into a single output file.
 * inlineDynamic → Any dynamic import() calls are merged into the main file instead of split out.
 * sourcemap → Off. No debug map files needed for a library build.
 * minify    → On. Code is compressed to reduce bundle size for production.
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
      entry: path.resolve(__dirname, "src/index.js"),
      name: "digitUiLibraries",
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

    outDir: "dist",
    sourcemap: false,
    minify: true,
    emptyOutDir: true,
  },
});