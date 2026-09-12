/** Supported remote tokens and the CSS variables exposed to application CSS. */
export const themeTokenMap = Object.freeze({
  "colors.text.default": { cssVariable: "--theme-text-default", type: "color" },
  "colors.text.primary": { cssVariable: "--theme-text-primary", type: "color" },
  "colors.text.secondary": { cssVariable: "--theme-text-secondary", type: "color" },
  "colors.text.light": { cssVariable: "--theme-text-light", type: "color" },
  "colors.text.placeholder": { cssVariable: "--theme-text-placeholder", type: "color" },
  "colors.text.error": { cssVariable: "--theme-text-error", type: "color" },
  "colors.brand.primary": { cssVariable: "--theme-color-primary", type: "color" },
  "colors.common.dark": { cssVariable: "--theme-color-dark", type: "color" },
  "colors.common.light": { cssVariable: "--theme-color-light", type: "color" },
  "colors.background.default": { cssVariable: "--theme-background-default", type: "color" },
  "colors.background.light": { cssVariable: "--theme-background-light", type: "color" },
  "colors.background.secondary": { cssVariable: "--theme-background-secondary", type: "color" },
  "colors.background.notification": { cssVariable: "--theme-background-notification", type: "color" },
  "colors.border.light": { cssVariable: "--theme-border-light", type: "color" },
  "colors.border.dark": { cssVariable: "--theme-border-dark", type: "color" },
  "colors.border.input": { cssVariable: "--theme-border-input", type: "color" },
  "colors.border.languages": { cssVariable: "--theme-border-languages", type: "color" },
  "colors.divider.primary": { cssVariable: "--theme-divider-primary", type: "color" },
  "shadows.default": { cssVariable: "--theme-shadow-default", type: "shadow" },
  "borderRadius.sm": { cssVariable: "--theme-border-radius-sm", type: "length" },
  "borderRadius.md": { cssVariable: "--theme-border-radius-md", type: "length" },
  "layout.sidebar.width": { cssVariable: "--theme-sidebar-width", type: "length" },
  "layout.sidebar.collapsedWidth": { cssVariable: "--theme-sidebar-collapsed-width", type: "length" },
  "layout.header.height": { cssVariable: "--theme-header-height", type: "length" },
  "gradients.button.primary": { cssVariable: "--theme-gradient-button-primary", type: "gradient" },
  "gradients.button.languages": { cssVariable: "--theme-gradient-button-languages", type: "gradient" },
  "typography.fontFamily": { cssVariable: "--theme-font-family-primary", type: "fontFamily" },
});

const isPlainObject = (value) => Object.prototype.toString.call(value) === "[object Object]";

/** Read a nested token using its dot-delimited public path. */
export const getValueAtPath = (object, path) => path.split(".").reduce((value, key) => value?.[key], object);

/** Build validated nested theme output without mutating the caller's object. */
const setValueAtPath = (object, path, value) => {
  const keys = path.split(".");
  const lastKey = keys.pop();
  const target = keys.reduce((current, key) => {
    current[key] = isPlainObject(current[key]) ? current[key] : {};
    return current[key];
  }, object);
  target[lastKey] = value;
  return object;
};

/** Deeply merge theme objects while replacing (rather than concatenating) arrays. */
export const mergeThemeConfig = (base, override) => {
  if (!isPlainObject(override)) return isPlainObject(base) ? { ...base } : base;

  return Object.entries(override).reduce(
    (merged, [key, value]) => {
      if (isPlainObject(value) && isPlainObject(merged[key])) {
        merged[key] = mergeThemeConfig(merged[key], value);
      } else if (Array.isArray(value)) {
        merged[key] = [...value];
      } else {
        merged[key] = value;
      }
      return merged;
    },
    isPlainObject(base) ? { ...base } : {},
  );
};

const UNSAFE_VALUE = /[;{}<>]|@import|expression\s*\(|url\s*\(|var\s*\(/i;

// Validators deliberately accept a narrow CSS subset because MDMS values become inline CSS variables.
const validators = {
  color: (value) =>
    typeof value === "string" &&
    value.length <= 80 &&
    !UNSAFE_VALUE.test(value) &&
    (/^#(?:[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$/i.test(value) || /^(?:rgb|rgba|hsl|hsla)\([\d\s.,%+\-/]+\)$/i.test(value)),
  length: (value) => typeof value === "string" && /^(?:0|\d+(?:\.\d+)?(?:px|rem|em|%|vh|vw))$/i.test(value),
  fontFamily: (value) => typeof value === "string" && value.length <= 200 && !UNSAFE_VALUE.test(value) && /^[a-z0-9\s,'"-]+$/i.test(value),
  shadow: (value) => typeof value === "string" && value.length <= 200 && !UNSAFE_VALUE.test(value) && /^[#a-z0-9\s.,()%+\-/]+$/i.test(value),
  gradient: (value) =>
    typeof value === "string" && value.length <= 300 && !UNSAFE_VALUE.test(value) && /^linear-gradient\([#a-z0-9\s.,()%+\-/]+\)$/i.test(value),
};

/** Keep only recognized, safe tokens and report every ignored remote value. */
export const validateThemeConfig = (config) => {
  const candidate = isPlainObject(config?.theme) ? config.theme : config;
  const theme = {};
  const errors = [];

  if (!isPlainObject(candidate)) return { theme, errors: ["Theme configuration must be an object"] };

  Object.entries(themeTokenMap).forEach(([path, definition]) => {
    const value = getValueAtPath(candidate, path);
    if (value === undefined) return;
    if (!validators[definition.type]?.(value)) {
      errors.push(`Unsupported value at ${path}`);
      return;
    }
    setValueAtPath(theme, path, value);
  });

  return { theme, errors };
};

/** Resolve only the validated theme supplied by MDMS. */
export const resolveTheme = (config = {}) => {
  const { theme, errors } = validateThemeConfig(config);
  return { theme, errors };
};

/** Convert nested theme tokens into the flat CSS custom-property contract. */
export const createThemeVariables = (theme) =>
  Object.entries(themeTokenMap).reduce((variables, [path, definition]) => {
    const value = getValueAtPath(theme, path);
    if (value !== undefined) variables[definition.cssVariable] = value;
    return variables;
  }, {});

export const applyThemeVariables = (theme, target) => {
  const root = target || (typeof document !== "undefined" ? document.documentElement : null);
  const variables = createThemeVariables(theme);
  if (!root?.style) return variables;

  // A refreshed MDMS theme may omit a value that existed in the previous
  // response. Remove the old remote values before applying the current one.
  Object.values(themeTokenMap).forEach(({ cssVariable }) => root.style.removeProperty(cssVariable));
  Object.entries(variables).forEach(([property, value]) => root.style.setProperty(property, value));
  root.dataset.themeReady = "true";
  return variables;
};

/** Remove the active MDMS theme when the master itself is no longer available. */
export const clearThemeVariables = (target) => {
  const root = target || (typeof document !== "undefined" ? document.documentElement : null);
  if (!root?.style) return;

  Object.values(themeTokenMap).forEach(({ cssVariable }) => root.style.removeProperty(cssVariable));
  delete root.dataset.themeReady;
};
