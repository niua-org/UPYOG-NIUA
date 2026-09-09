/**
 * ==============================================================================
 * TAILWIND CONFIGURATION & DYNAMIC THEME SYSTEM
 * ==============================================================================
 *
 * This configuration integrates the central design system tokens from
 * `tailwind.theme.json` into Tailwind CSS utility classes and CSS variables.
 *
 * ------------------------------------------------------------------------------
 * HOW IT WORKS:
 * ------------------------------------------------------------------------------
 * 1. Tokens in `tailwind.theme.json` automatically generate Tailwind utility classes:
 *
 *    JSON Path                      Generated Tailwind Class       CSS Variable
 *    ---------------------------------------------------------------------------
 *    colors.primary.main            bg-primary-main, text-primary-main    --primary-main
 *    colors.secondary               bg-secondary, text-secondary          --secondary
 *    colors.border                  border-border                         --border
 *    spacing.md                     p-md, m-md, gap-md                    --spacing-md
 *    borderRadius.lg                rounded-lg                            --border-radius-lg
 *    borderWidth.sm                 border-sm                             --border-width-sm
 *    fontSize.xl                    text-xl                               --font-size-xl
 *    fontWeight.bold                font-bold                             --weight-bold
 *
 * 2. Example Usage in React Components:
 *    <div className="bg-primary-main text-white p-md rounded-md shadow-md">
 *      <h1 className="text-xl font-bold">Header</h1>
 *      <p className="text-text-secondary text-sm">Description</p>
 *    </div>
 *
 * 3. Example Usage in SCSS Files:
 *    .custom-card {
 *      background-color: var(--primary-main);
 *      padding: var(--spacing-md);
 *      border-radius: var(--border-radius-md);
 *    }
 * ==============================================================================
 */

const plugin = require("tailwindcss/plugin");
const semanticPlugin = require("./tailwind.semantic");
const themeConfig = require("./tailwind.theme.json");

const theme = themeConfig.config.theme;
const extend = theme.extend || {};
const pageContainer = extend.pageContainer || {};
const pageLayouts = pageContainer.layouts || [];

// Register grid template layouts configured in JSON
extend.gridTemplateColumns = {
    ...(extend.gridTemplateColumns || {}),
    ...Object.fromEntries(pageLayouts.map((layout) => [layout.key, layout.template])),
};

// Register page container gap
extend.gap = {
    ...(extend.gap || {}),
    "page-container": pageContainer.gap || "16px",
};

// Mirror all theme colors to border colors so `border-<color-name>` utilities work out of the box
if (extend.colors) {
    extend.borderColor = extend.borderColor || {};
    const flattenColors = (obj, prefix = "") => {
        for (const [key, val] of Object.entries(obj)) {
            const newKey = prefix ? `${prefix}-${key}` : key;
            if (typeof val === "object" && val !== null) {
                flattenColors(val, newKey);
            } else {
                extend.borderColor[newKey] = val;
            }
        }
    };
    flattenColors(extend.colors);
}

/**
 * Helper to convert camelCase strings into kebab-case
 * Example: 'borderRadius' -> 'border-radius', 'pageContainer' -> 'page-container'
 */
const toKebabCase = (str) =>
    str.replace(/([a-z0-9]|(?=[A-Z]))([A-Z])/g, "$1-$2").toLowerCase();

/**
 * Recursively extracts all design tokens from the JSON `extend` object
 * and flattens them into standard CSS custom properties on :root.
 *
 * Example:
 *   extend.colors.primary.main = "#a82227" -> "--primary-main: #a82227"
 *   extend.spacing.md = "16px"             -> "--spacing-md: 16px"
 *   extend.borderRadius.lg = "16px"        -> "--border-radius-lg: 16px"
 */
const extractThemeCssVariables = (extendObj) => {
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
 * Custom Tailwind plugin that writes all design tokens into the :root stylesheet
 * at build-time so the application renders with full styling immediately on load.
 */
const variablesPlugin = plugin(function ({ addBase }) {
    addBase({
        ":root": extractThemeCssVariables(extend),
    });
});

module.exports = {
    content: [
        "./src/**/*.{js,jsx,ts,tsx}",
        "./micro-ui-internals/packages/css/src/**/*.{scss,css}",
        "./micro-ui-internals/packages/react-components/src/**/*.{js,jsx,ts,tsx}",
        "./micro-ui-internals/packages/modules/*/src/**/*.{js,jsx,ts,tsx}",
        "./micro-ui-internals/packages/modules/*/*/src/**/*.{js,jsx,ts,tsx}",
    ],

    theme,

    plugins: [semanticPlugin, variablesPlugin],
};