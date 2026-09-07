/**
 * ==============================================================================
 * THEME SERVICE & DYNAMIC RUNTIME THEMING
 * ==============================================================================
 *
 * This service manages runtime theming from the MDMS Theme API
 * (`/mdms-v2/v1/theme-config/_search`) with fallback to `tailwind.theme.json`.
 *
 * ------------------------------------------------------------------------------
 * HOW IT WORKS AT RUNTIME:
 * ------------------------------------------------------------------------------
 * 1. Fetches active theme for the tenant and user type (CITIZEN / EMPLOYEE).
 * 2. Parses the `config.theme.extend` object.
 * 3. Dynamically injects all CSS variables onto `document.documentElement.style` (:root).
 * 4. All Tailwind utility classes and SCSS rules consume these variables in real time.
 *
 * ------------------------------------------------------------------------------
 * EXAMPLE USAGE IN CODE:
 * ------------------------------------------------------------------------------
 * // 1. Using the React Hook (Recommended in components):
 * const { data, isLoading } = Digit.Hooks.useThemeConfig({
 *   tenantId: "pg.citya",
 *   themeType: "CITIZEN",
 * });
 *
 * // 2. Using the Service Directly:
 * const themeData = await Digit.ThemeService.getTheme({
 *   tenantId: "pg",
 *   themeType: "EMPLOYEE",
 * });
 *
 * // 3. Manually applying any theme JSON:
 * Digit.ThemeService.applyTheme(customThemeJson);
 * ==============================================================================
 */

import Urls from "../atoms/urls";
import { Request } from "../atoms/Utils/Request";
import { ThemeTypes, ThemeStatus } from "../../enums/Theme";

// Temporary fallback until dynamic theme configurations are fully available from MDMS API across all environments.
// Once MDMS Theme API is deployed everywhere, this local JSON import will be replaced with API-only data.
import defaultTheme from "../../../../../../tailwind.theme.json";


/**
 * Helper to convert camelCase keys into kebab-case
 * @example 'borderRadius' -> 'border-radius', 'fontFamily' -> 'font-family'
 */
const toKebabCase = (str) =>
  str.replace(/([a-z0-9]|(?=[A-Z]))([A-Z])/g, "$1-$2").toLowerCase();

/**
 * Recursively extracts CSS variables from theme tokens.
 *
 * @param {Object} extendObj - The `config.theme.extend` object
 * @returns {Object} Key-value map of CSS custom properties (e.g. { '--primary-main': '#a82227' })
 */
export const extractThemeCssVariables = (extendObj) => {
  const vars = {};
  if (!extendObj || typeof extendObj !== "object") return vars;

  const traverse = (obj, prefix = "") => {
    for (const [key, value] of Object.entries(obj)) {
      if (value === null || value === undefined || value === "") continue;

      const tokenKey = toKebabCase(key);
      let currentPrefix = prefix;

      if (!prefix) {
        if (tokenKey === "colors") {
          currentPrefix = "";
        } else if (tokenKey === "font-weight") {
          currentPrefix = "--weight";
        } else {
          currentPrefix = `--${tokenKey}`;
        }
      } else {
        currentPrefix = prefix ? `${prefix}-${tokenKey}` : tokenKey;
      }

      if (typeof value === "object" && !Array.isArray(value)) {
        traverse(value, currentPrefix);
      } else {
        const finalKey = currentPrefix.startsWith("--") ? currentPrefix : `--${currentPrefix}`;
        vars[finalKey] = Array.isArray(value) ? value.join(", ") : String(value);
      }
    }
  };

  traverse(extendObj);
  return vars;
};

/**
 * Extracts the `extend` design tokens from various API response structures.
 *
 * @param {Object} themeData - Raw response from MDMS API
 * @returns {Object|null} The `extend` configuration object
 */
export const getThemeExtend = (themeData) => {
  const data = Array.isArray(themeData?.themeConfigs) ? themeData.themeConfigs[0] : themeData;
  return data?.config?.theme?.extend || null;
};

/**
 * Injects extracted CSS variables into `document.documentElement.style` (:root).
 *
 * @param {Object} extend - Theme extend object containing colors, spacing, etc.
 */
export const applyThemeExtend = (extend) => {
  const root = typeof document !== "undefined" ? document.documentElement : null;
  if (!root || !extend) return;

  const cssVariables = extractThemeCssVariables(extend);
  for (const [name, value] of Object.entries(cssVariables)) {
    root.style.setProperty(name, value);
  }
};

export const ThemeService = {
  /**
   * Returns the static default theme configuration from tailwind.theme.json
   */
  getDefaultTheme: () => defaultTheme,

  /**
   * Applies any theme configuration object to the document root
   *
   * @param {Object} themeData - Theme configuration object
   * @returns {Object} Applied extend tokens
   */
  applyTheme: (themeData) => {
    const extend = getThemeExtend(themeData) || defaultTheme.config.theme.extend;
    applyThemeExtend(extend);
    return extend;
  },

  /**
   * Fetches the theme configuration from MDMS API (/mdms-v2/v1/theme-config/_search).
   * Automatically falls back to `tailwind.theme.json` on error or empty response.
   *
   * @param {Object} params - { tenantId, themeType, status }
   * @returns {Promise<Object>} The resolved theme object
   */
  getTheme: async ({ tenantId, themeType, status = ThemeStatus.DEFAULT } = {}) => {
    try {
      const resolvedThemeType =
        themeType ||
        (typeof window !== "undefined" && window.location.pathname.includes("employee")
          ? ThemeTypes.EMPLOYEE
          : ThemeTypes.CITIZEN);

      const payload = {
        themeConfig: {
          tenantId: tenantId || Digit.ULBService.getStateId(),
          themeType: resolvedThemeType,
          status,
        },
      };


      const response = await Request({
        url: Urls.ThemeConfig,
        data: payload,
        method: "POST",
        userService: true,
        auth: true,
        useCache: true,
      });

      const extend = getThemeExtend(response);
      if (extend) {
        ThemeService.applyTheme(response);
        return response;
      }

      ThemeService.applyTheme(defaultTheme);
      return defaultTheme;
    } catch (error) {
      ThemeService.applyTheme(defaultTheme);
      return defaultTheme;
    }
  },
};

export default ThemeService;
