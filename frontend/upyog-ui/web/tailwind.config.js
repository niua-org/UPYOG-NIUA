const semanticPlugin = require("./tailwind.semantic");
const themeConfig = require("./tailwind.theme.json");

const theme = themeConfig.config.theme;
const pageContainer = themeConfig.config.pageContainer || {};
const pageLayouts = pageContainer.layouts || [];

theme.extend = theme.extend || {};
theme.extend.gridTemplateColumns = {
    ...(theme.extend.gridTemplateColumns || {}),
    ...Object.fromEntries(pageLayouts.map((layout) => [layout.key, layout.template])),
};
theme.extend.gap = {
    ...(theme.extend.gap || {}),
    "page-container": pageContainer.gap || "16px",
};

module.exports = {
    content: [
        "./src/**/*.{js,jsx,ts,tsx}",

        "./micro-ui-internals/packages/css/src/**/*.{scss,css}",

        "./micro-ui-internals/packages/react-components/src/**/*.{js,jsx,ts,tsx}",

        "./micro-ui-internals/packages/modules/*/src/**/*.{js,jsx,ts,tsx}",
    ],

    theme,

    plugins: [semanticPlugin],
};
