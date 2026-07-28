import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { fetchEnabledMDMS } from "../hooks/useEnabledMDMS";
import {
  applyThemeVariables,
  clearThemeVariables,
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
let currentState = {
  theme: null,
  validationErrors: [],
  isThemeLoading: false,
  isThemeMissing: false,
  themeError: null,
  source: "uninitialized",
};

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

const publishMissingTheme = () => {
  clearThemeVariables();
  return publish({
    theme: null,
    validationErrors: [],
    isThemeLoading: false,
    isThemeMissing: true,
    themeError: null,
    source: "missing",
  });
};

/** Validate, apply, and publish an MDMS theme in one consistent operation. */
export const updateTheme = (config, source = "runtime") => {
  const { theme, errors: validationErrors } = resolveTheme(config);
  if (validationErrors.length) console.warn("Invalid theme values were ignored:", validationErrors);
  // An existing but empty/invalid master is equivalent to a missing theme:
  // the application must never continue with browser or stylesheet fallbacks.
  if (Object.keys(createThemeVariables(theme)).length === 0) return publishMissingTheme();
  applyThemeVariables(theme);
  return publish({ theme, validationErrors, isThemeLoading: false, isThemeMissing: false, themeError: null, source });
};

const withTimeout = (promise, timeoutMs) =>
  new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error("Theme request timed out")), timeoutMs);
    promise.then(resolve, reject).finally(() => clearTimeout(timeout));
  });

/** Refresh the remote theme while retaining the last MDMS theme on request failure. */
export const refreshTheme = async () => {
  if (!tenantId) return publishMissingTheme();
  if (refreshPromise) return refreshPromise;

  publish({ ...currentState, isThemeLoading: true, isThemeMissing: false, themeError: null });

  refreshPromise = (async () => {
    try {
      const config = await withTimeout(fetchThemeConfig(), REQUEST_TIMEOUT);
      if (!config) return publishMissingTheme();
      return updateTheme(config, "remote");
    } catch (themeError) {
      console.warn("Unable to refresh the MDMS theme:", themeError.message);
      return publish({ ...currentState, isThemeLoading: false, themeError });
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
};

/** Record the tenant synchronously; ThemeProvider loads its theme from MDMS. */
export const initializeTheme = ({ tenantId: initialTenantId } = {}) => {
  tenantId = initialTenantId;
  clearThemeVariables();
  return publish({
    theme: null,
    validationErrors: [],
    isThemeLoading: Boolean(tenantId),
    isThemeMissing: !tenantId,
    themeError: null,
    source: tenantId ? "initial" : "missing",
  });
};

const statusContainerStyle = {
  alignItems: "center",
  background: "#f5f5f5",
  color: "#242424",
  display: "flex",
  justifyContent: "center",
  minHeight: "100vh",
  padding: "24px",
  textAlign: "center",
};

const statusCardStyle = {
  background: "#ffffff",
  border: "1px solid #d6d6d6",
  borderRadius: "8px",
  maxWidth: "480px",
  padding: "32px",
  width: "100%",
  display: "flex",
  flexDirection: 'column',
  gap: '0.5rem'
};

/** Theme status UI cannot depend on theme variables because none are active yet. */
const ThemeStatus = ({ state }) => {
  const isLoading = state.isThemeLoading;
  const isMissing = state.isThemeMissing;
  const heading = isLoading ? "Loading theme" : isMissing ? "Theme missing" : "Unable to load theme";
  const message = isLoading
    ? "Loading the theme configuration from MDMS…"
    : isMissing
      ? "No enabled theme configuration was found in MDMS."
      : "The theme configuration could not be loaded. Please try again.";

  return (
    <main className="ui-rewamp" style={statusContainerStyle}>
      <section style={statusCardStyle} role={isLoading ? "status" : "alert"}>
        <h1>
          <strong>{heading}</strong>
        </h1>
        <p>{message}</p>
        {!isLoading && (
          <button className="button primary" type="button" onClick={() => void refreshTheme()}>
            Retry
          </button>
        )}
      </section>
    </main>
  );
};

/** React bridge for the event-based theme service used by window.Digit.Theme. */
export const ThemeProvider = ({ children, initialState = currentState }) => {
  const [state, setState] = useState(initialState);
  useEffect(() => {
    if (typeof window === "undefined") return undefined;
    const listener = ({ detail }) => setState(detail);
    window.addEventListener("digit:theme-changed", listener);
    // The shared promise deduplicates React Strict Mode mounts.
    if (initialState.source === "initial") {
      void refreshTheme();
    }
    return () => window.removeEventListener("digit:theme-changed", listener);
  }, []);
  const value = useMemo(() => ({ ...state, refreshTheme, updateTheme }), [state]);
  const canRenderApplication = Boolean(state.theme) && !state.isThemeMissing;
  return (
    <ThemeContext.Provider value={value}>
      {canRenderApplication ? children : <ThemeStatus state={state} />}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme must be used inside ThemeProvider");
  return context;
};

export {
  applyThemeVariables,
  clearThemeVariables,
  createThemeVariables,
  mergeThemeConfig,
  resolveTheme,
  themeTokenMap,
  validateThemeConfig,
};

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
