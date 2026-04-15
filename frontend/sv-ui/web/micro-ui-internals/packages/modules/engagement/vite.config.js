import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

/**
 * Vite build config for the "engagement" module.
 * This module handles citizen engagement features like surveys, events, and notifications.
 *
 * plugins   → Adds React support. The "include" option makes it process both .js and .jsx files.
 * esbuild   → Treats .js files as JSX. Also silences two known warnings from legacy code:
 *             - "duplicate-object-key" (object with two same-named keys)
 *             - "duplicate-case" (switch with two identical case values)
 * build.lib → Builds this as a reusable library (not a standalone app).
 *             Entry: src/Module.js | Name: digitUiModule
 *             Output: index.modern.js (ES module only).
 * external  → These packages are NOT bundled — the host app is expected to have them already.
 *             Includes: react, react-dom, react-router-dom, react-redux, redux, react-i18next, i18next, @tanstack/react-query.
 * globals   → Maps package names to browser global variable names.
 *             Needed if this library is ever loaded directly in a browser without a bundler.
 *             e.g. react → React, react-dom → ReactDOM.
 * sourcemap → Off. No debug map files needed for a library build.
 * minify    → Off. Code stays readable (not compressed).
 * outDir    → Built files go into the "dist" folder.
 *             Note: emptyOutDir is not set, so old files in dist are NOT auto-cleared.
 */
export default defineConfig({
  plugins: [react({ include: /\.(jsx|js)$/ })],
  esbuild: {
    loader: "jsx",
    include: /.*\.js$/,
    exclude: [],
    logOverride: { "duplicate-object-key": "silent", "duplicate-case": "silent" },
  },
  build: {
    lib: {
      entry: path.resolve(__dirname, "src/Module.js"),
      name: "digitUiModule",
      formats: ["es"],
      fileName: () => "index.modern.js",
    },
    rollupOptions: {
      external: ["react", "react-dom", "react-router-dom", "react-redux", "redux", "@tanstack/react-query", "react-i18next", "i18next"],
      output: {
        globals: {
          react: "React",
          "react-dom": "ReactDOM",
          "react-router-dom": "ReactRouterDOM",
        },
      },
    },
    sourcemap: false,
    minify: false,
    outDir: "dist",
  },
});
