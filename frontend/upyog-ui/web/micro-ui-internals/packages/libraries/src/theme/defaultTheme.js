/**
 * Safe visual defaults used before remote CONFIG.ThemeConfig is available and
 * as fallbacks when a remote token is missing or invalid.
 */
const defaultTheme = Object.freeze({
  colors: {
    text: {
      default: "#21182C",
      primary: "#151034",
      secondary: "#584F74",
      light: "#5F5875",
      placeholder: "#7E7892",
      error: "#B91C1C",
    },
    brand: {
      primary: "#3D2364",
    },
    common: {
      dark: "#000000",
      light: "#FFFFFF",
    },
    background: {
      default: "#FAF8FC",
      light: "#FAF7FD",
      secondary: "#F7F5FA",
      notification: "#D85A5A",
    },
    border: {
      light: "#C9B8E2",
      dark: "#A69CC6",
      input: "#D6CDE4",
      languages: "#DCD3EA",
    },
    divider: {
      primary: "#E8E1F0",
    },
  },
  shadows: {
    default: "0 18px 44px rgba(32, 24, 44, 0.1)",
  },
  borderRadius: {
    sm: "8px",
    md: "14px",
  },
  layout: {
    sidebar: {
      width: "260px",
      collapsedWidth: "60px",
    },
    header: {
      height: "84px",
    },
  },
  gradients: {
    button: {
      primary: "linear-gradient(90deg, #6A4A91 0%, #584F74 50%, #3E285F 100%)",
      languages: "linear-gradient(90deg, #6A4A91 0%, #3E285F 100%)",
    },
  },
  typography: {
    fontFamily: '"Inter", system-ui, -apple-system, BlinkMacSystemFont, sans-serif',
  },
});

export default defaultTheme;
