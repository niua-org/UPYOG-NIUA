/**
 * Color fields configurations for Employee Theme Builder.
 * Defines groups of related palette select fields as declarative arrays.
 */

// Step 2: Theme Colors -> Primary Colors
export const PRIMARY_COLOR_FIELDS = [
  { label: "Primary Main", path: "colors.primary.main", defaultToken: "Color01" },
  { label: "Primary Light", path: "colors.primary.light", defaultToken: "Color02" },
  { label: "Primary Dark", path: "colors.primary.dark", defaultToken: "Color03" },
  { label: "Secondary", path: "colors.secondary", defaultToken: "Color04" },
];

// Step 2: Theme Colors -> Text & Links
export const TEXT_LINK_COLOR_FIELDS = [
  { label: "Text Primary", path: "colors.text.primary", defaultToken: "Color05" },
  { label: "Text Secondary", path: "colors.text.secondary", defaultToken: "Color06" },
  { label: "Link Normal", path: "colors.link.normal", defaultToken: "Color07" },
  { label: "Link Hover", path: "colors.link.hover", defaultToken: "Color08" },
];

// Step 2: Theme Colors -> Borders & Feedback
export const BORDER_FEEDBACK_COLOR_FIELDS = [
  { label: "Border Default", path: "colors.border", defaultToken: "Color09" },
  { label: "Input Border", path: "colors.inputBorder", extraPaths: ["colors.input-border"], defaultToken: "Color10" },
  { label: "Focus Color", path: "colors.focus", defaultToken: "Color11" },
  { label: "Error Color", path: "colors.error", defaultToken: "Color12" },
  { label: "Success Color", path: "colors.success", defaultToken: "Color13" },
];

// Step 5: Spacing, Border & Radius -> Border Colors
export const BORDER_COLOR_TOKENS = [
  { label: "Primary Border Color", path: "borderColor.primary", defaultToken: "Color01" },
  { label: "Secondary Border Color", path: "borderColor.secondary", defaultToken: "Color04" },
  { label: "Tertiary Border Color", path: "borderColor.tertiary", defaultToken: "Color06" },
  { label: "Quaternary Border Color", path: "borderColor.quaternary", defaultToken: "Color14" },
  { label: "Quinary Border Color", path: "borderColor.quinary", defaultToken: "Color09" },
  { label: "Senary Border Color", path: "borderColor.senary", defaultToken: "Color15" },
];

// Step 6: Shadows -> Box Shadow Colors
export const BOX_SHADOW_COLOR_TOKENS = [
  { label: "Primary Shadow Color", path: "boxShadowColor.primary", defaultToken: "Color01" },
  { label: "Secondary Shadow Color", path: "boxShadowColor.secondary", defaultToken: "Color04" },
  { label: "Tertiary Shadow Color", path: "boxShadowColor.tertiary", defaultToken: "Color06" },
  { label: "Quaternary Shadow Color", path: "boxShadowColor.quaternary", defaultToken: "Color14" },
  { label: "Quinary Shadow Color", path: "boxShadowColor.quinary", defaultToken: "Color09" },
  { label: "Senary Shadow Color", path: "boxShadowColor.senary", defaultToken: "Color15" },
];

// Step 7: Buttons & Actions -> Primary Button
export const BUTTON_PRIMARY_FIELDS = [
  { label: "Background Color", path: "button.primary.background", defaultToken: "Color01" },
  { label: "Text Color", path: "button.primary.color", defaultHex: "#FFFFFF" },
];

// Step 7: Buttons & Actions -> Secondary Button
export const BUTTON_SECONDARY_FIELDS = [
  { label: "Background Color", path: "button.secondary.background", defaultToken: "Color04" },
  { label: "Text Color", path: "button.secondary.color", defaultHex: "#FFFFFF" },
];

// Step 7: Buttons & Actions -> Tertiary Button
export const BUTTON_TERTIARY_FIELDS = [
  { label: "Background Color", path: "button.tertiary.background", defaultToken: "Color06" },
  { label: "Text Color", path: "button.tertiary.color", defaultHex: "#FFFFFF" },
];

// Step 7: Buttons & Actions -> Inverse Button
export const BUTTON_INVERSE_FIELDS = [
  { label: "Background Color", path: "button.inverse.background", defaultToken: "Color15" },
  { label: "Text Color", path: "button.inverse.color", defaultToken: "Color05" },
];
