import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { fetchEnabledMDMS } from "../hooks/useEnabledMDMS";
import {
  applyThemeVariables,
  createThemeVariables,
  mergeThemeConfig,
  resolveTheme,
  themeTokenMap,
  validateThemeConfig,
} from "./themeUtils";

const ThemeContext = createContext(null);
const REQUEST_TIMEOUT = 2500;
let tenantId;
let refreshPromise = null;
// Keep the raw defaults private to themeUtils. Consumers receive the resolved
// theme state instead of maintaining another public reference to defaultTheme.
let currentState = { theme: resolveTheme().theme, isThemeLoading: false, themeError: null, source: "default" };

/** Fetch the enabled tenant theme without caching so runtime refreshes are current. */
export const fetchThemeConfig = async () => {
  const response = await fetchEnabledMDMS(tenantId, "CONFIG", [{ name: "ThemeConfig" }], { useCache: false });
  return response?.CONFIG?.ThemeConfig?.[0];
};

const publish = (state) => {
  currentState = state;
  // The event keeps imperative window.Digit.Theme updates and React consumers synchronized.
  if (typeof window !== "undefined") window.dispatchEvent(new CustomEvent("digit:theme-changed", { detail: state }));
  return state;
};

/** Validate, merge, apply, and publish a theme in one consistent operation. */
export const updateTheme = (config, source = "runtime") => {
  const { theme, errors: validationErrors } = resolveTheme(config);
  if (validationErrors.length) console.warn("Invalid theme values were ignored:", validationErrors);
  applyThemeVariables(theme);
  return publish({ theme, validationErrors, isThemeLoading: false, themeError: null, source });
};

const withTimeout = (promise, timeoutMs) =>
  new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error("Theme request timed out")), timeoutMs);
    promise.then(resolve, reject).finally(() => clearTimeout(timeout));
  });

/** Refresh the remote theme while retaining the last usable theme on failure. */
export const refreshTheme = async () => {
  if (!tenantId) return currentState;
  if (refreshPromise) return refreshPromise;

  publish({ ...currentState, isThemeLoading: true, themeError: null });

  refreshPromise = (async () => {
    try {
      const config = await withTimeout(fetchThemeConfig(), REQUEST_TIMEOUT);
      if (!config) throw new Error("Theme configuration was empty");
      return updateTheme(config, "remote");
    } catch (themeError) {
      console.warn("Using the default theme:", themeError.message);
      return publish({ ...currentState, isThemeLoading: false, themeError });
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
};

/** Apply defaults synchronously; ThemeProvider refreshes remote tokens after mount. */
export const initializeTheme = ({ tenantId: initialTenantId } = {}) => {
  tenantId = initialTenantId;
  return updateTheme({}, "default");
};

/** React bridge for the event-based theme service used by window.Digit.Theme. */
export const ThemeProvider = ({ children, initialState = currentState }) => {
  const [state, setState] = useState(initialState);
  useEffect(() => {
    if (typeof window === "undefined") return undefined;
    const listener = ({ detail }) => setState(detail);
    window.addEventListener("digit:theme-changed", listener);
    // Render with safe defaults first; remote MDMS must never block the app
    // bootstrap. The shared promise also deduplicates React Strict Mode mounts.
    if (tenantId && initialState.source === "default") {
      void refreshTheme();
    }
    return () => window.removeEventListener("digit:theme-changed", listener);
  }, []);
  const value = useMemo(() => ({ ...state, refreshTheme, updateTheme }), [state]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme must be used inside ThemeProvider");
  return context;
};

export { applyThemeVariables, createThemeVariables, mergeThemeConfig, resolveTheme, themeTokenMap, validateThemeConfig };

const Theme = {
  fetchThemeConfig,
  initializeTheme,
  refreshTheme,
  ThemeProvider,
  themeTokenMap,
  updateTheme,
  useTheme,
};

export default Theme;
