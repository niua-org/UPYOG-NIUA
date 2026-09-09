import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

/**
 * Vite configuration for the Street Vending UI app (production build).
 *
 * - Loads environment variables based on the current mode (development/production).
 * - Configures a proxy for all backend API routes to avoid CORS issues during development.
 * - Sets base URL to "/sv-ui/" so the app is served under that path.
 * - Treats .js files as JSX so React code in .js files works fine.
 * - Builds output into the "build" folder with source maps enabled.
 * - Pre-bundles heavy dependencies like pdfmake and jspdf for faster dev startup.
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxyTarget = env.VITE_PROXY_API;

  const proxyRoutes = [
    "/access/v1/actions/mdms",
    "/egov-mdms-service",
    "/egov-location",
    "/mdms-v2",
    "/localization",
    "/egov-workflow-v2",
    "/filestore",
    "/user-otp",
    "/user",
    "/billing-service",
    "/collection-services",
    "/pdf-service",
    "/pg-service",
    "/inbox",
    "/egov-hrms",
    "/egov-user-event",
    "/sv-services",
    "/employee-dashboard",
  ];

  const proxyConfig = Object.fromEntries(
    proxyRoutes.map((route) => [
      route,
      { target: proxyTarget, changeOrigin: true },
    ])
  );

  return {
    plugins: [
      react({ include: /\.(jsx|js)$/ }),
    ],

    base: "/sv-ui/",

    esbuild: {
      loader: "jsx",
      include: /.*\.(js|jsx)$/,
      exclude: [],
      logOverride: {
        "duplicate-case": "silent",
      },
    },

    server: {
      port: 3000,
      fs: { allow: [".."] },
      proxy: proxyConfig,
    },

    build: {
      sourcemap: false,
      outDir: "build",
      commonjsOptions: {
        transformMixedEsModules: true,
      },
    },

    envPrefix: "VITE_",

    optimizeDeps: {
      include: [
        "pdfmake",
        "pdfmake/build/pdfmake",
        "pdfmake/build/vfs_fonts",
        "jspdf",
        "jspdf-autotable",
      ],
      esbuildOptions: {
        loader: {
          ".js": "jsx",
        },
      },
    },
  };
});
