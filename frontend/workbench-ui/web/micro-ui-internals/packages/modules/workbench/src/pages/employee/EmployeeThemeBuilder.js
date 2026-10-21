import React, { useState, useMemo, useEffect, useCallback } from "react";
import { useTranslation } from "react-i18next";
import {
    SectionHeader,
    Card,
    CardTitle,
    FieldsRow,
    ColorField,
    PaletteSelectField,
    NumberField,
    TextField,
    SelectField,
    ShadowField,
    SubmitButton
} from "../../components/ThemeCustomizeComponents";
import PaletteFieldGroup from "../../components/PaletteFieldGroup";
import {
    PRIMARY_COLOR_FIELDS,
    TEXT_LINK_COLOR_FIELDS,
    BORDER_FEEDBACK_COLOR_FIELDS,
    BORDER_COLOR_TOKENS,
    BOX_SHADOW_COLOR_TOKENS,
    BUTTON_PRIMARY_FIELDS,
    BUTTON_SECONDARY_FIELDS,
    BUTTON_TERTIARY_FIELDS,
    BUTTON_INVERSE_FIELDS
} from "../../configs/colorFieldsConfig";
import { ThemeConfig } from "../../configs/ThemeConfig";
import {
    Modal,
    Toast,
    RadioButtons,
    ThemePreviewIcon,
    ThemeBrandIcon,
    ThemeCommonIcon,
    ThemeGradientsIcon,
    ThemeTypographyIcon,
    ThemeBorderIcon,
    ThemeRadiusIcon,
    ThemeShadowsIcon,
    ThemeTextIcon
} from "@upyog/workbench-ui-react-components";

// ─── Default 15-Color Palette Definitions (Color01 to Color15) ─────────────────────────────────────
const DEFAULT_PALETTE = {
    Color01: "#a82227",
    Color02: "#F18F5E",
    Color03: "#C8602B",
    Color04: "#22394D",
    Color05: "#0B0C0C",
    Color06: "#505A5F",
    Color07: "#1D70B8",
    Color08: "#003078",
    Color09: "#D6D5D4",
    Color10: "#464646",
    Color11: "#a82227",
    Color12: "#D4351C",
    Color13: "#00703C",
    Color14: "#9E9E9E",
    Color15: "#FAFAFA",
};

const PALETTE_DESCRIPTIONS = {
    Color01: "Color01",
    Color02: "Color02",
    Color03: "Color03",
    Color04: "Color04",
    Color05: "Color05",
    Color06: "Color06",
    Color07: "Color07",
    Color08: "Color08",
    Color09: "Color09",
    Color10: "Color10",
    Color11: "Color11",
    Color12: "Color12",
    Color13: "Color13",
    Color14: "Color14",
    Color15: "Color15",
};

/**
 * LiveThemePreviewPanel Component
 * Renders a real-time simulated Employee Portal interface displaying live buttons,
 * typography, badges, cards, inputs, and gradients reflecting current theme Extend state.
 */
const LiveThemePreviewPanel = React.memo(({ themeExtend, palette }) => {
    const { t } = useTranslation();

    const primaryMain = themeExtend?.colors?.primary?.main || palette.Color01 || "#a82227";
    const primaryLight = themeExtend?.colors?.primary?.light || palette.Color02 || "#F18F5E";
    const secondaryColor = themeExtend?.colors?.secondary || palette.Color04 || "#22394D";
    const textPrimary = themeExtend?.colors?.text?.primary || palette.Color05 || "#0B0C0C";
    const textSecondary = themeExtend?.colors?.text?.secondary || palette.Color06 || "#505A5F";
    const linkNormal = themeExtend?.colors?.link?.normal || palette.Color07 || "#1D70B8";
    const borderColor = themeExtend?.borderColor?.primary || themeExtend?.colors?.border || palette.Color09 || "#D6D5D4";
    const inputBorderColor = themeExtend?.colors?.inputBorder || themeExtend?.colors?.["input-border"] || palette.Color10 || "#464646";
    const focusColor = themeExtend?.colors?.focus || palette.Color11 || "#a82227";
    const errorColor = themeExtend?.colors?.error || palette.Color12 || "#D4351C";
    const successColor = themeExtend?.colors?.success || palette.Color13 || "#00703C";

    const primaryGradient = themeExtend?.gradient?.primary || `linear-gradient(to right, ${primaryMain}, ${primaryLight})`;
    const secondaryGradient = themeExtend?.gradient?.secondary || `linear-gradient(to right, ${secondaryColor}, ${textSecondary})`;

    const fontFamilySans = themeExtend?.fontFamily?.sans?.[0] || "Roboto, sans-serif";
    const fontWeightRegular = themeExtend?.fontWeight?.regular || "400";
    const fontWeightMedium = themeExtend?.fontWeight?.medium || "500";
    const fontWeightBold = themeExtend?.fontWeight?.bold || "700";

    const borderRadiusMd = themeExtend?.borderRadius?.md || "8px";
    const borderWidthSm = themeExtend?.borderWidth?.sm || "1px";
    const boxShadowMd = themeExtend?.boxShadow?.md || "0 4px 6px -1px rgba(0, 0, 0, 0.1)";
    const boxShadowLg = themeExtend?.boxShadow?.lg || "0 10px 15px -3px rgba(0, 0, 0, 0.1)";

    const btnPrimaryBg = themeExtend?.button?.primary?.background || primaryMain;
    const btnPrimaryColor = themeExtend?.button?.primary?.color || "#FFFFFF";
    const btnSecondaryBg = themeExtend?.button?.secondary?.background || secondaryColor;
    const btnSecondaryColor = themeExtend?.button?.secondary?.color || "#FFFFFF";
    const btnTertiaryBg = themeExtend?.button?.tertiary?.background || textSecondary;
    const btnTertiaryColor = themeExtend?.button?.tertiary?.color || "#FFFFFF";
    const btnInverseBg = themeExtend?.button?.inverse?.background || "#FFFFFF";
    const btnInverseColor = themeExtend?.button?.inverse?.color || "#000000";

    return (
        <div className="live-theme-preview-wrapper" style={{ fontFamily: fontFamilySans }}>
            {/* Simulated Header */}
            <div className="preview-nav-header" style={{ background: secondaryColor }}>
                <div className="brand-section">
                    <div className="brand-icon-box" style={{ background: primaryMain }}>
                        U
                    </div>
                    <span className="brand-name" style={{ fontWeight: fontWeightBold }}>UPYOG Workbench</span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <span className="portal-tag" style={{ background: "rgba(255,255,255,0.15)", padding: "3px 10px", borderRadius: "12px" }}>
                        Live UI
                    </span>
                    <span style={{ width: "8px", height: "8px", borderRadius: "50%", background: successColor }} />
                </div>
            </div>

            {/* Simulated Hero Banner */}
            <div className="preview-hero-banner" style={{ background: primaryGradient }}>
                <h3 className="hero-title" style={{ fontWeight: fontWeightBold }}>
                    Theme Live Demonstration
                </h3>
                <p className="hero-subtitle" style={{ fontWeight: fontWeightRegular }}>
                    Real-time visual reflection of palette, buttons, typography, borders, and shadows.
                </p>
            </div>

            <div className="preview-content-body">
                {/* Buttons Preview Card */}
                <div className="preview-card-block" style={{ borderRadius: borderRadiusMd, border: `${borderWidthSm} solid ${borderColor}`, boxShadow: boxShadowMd }}>
                    <h4 className="card-block-title" style={{ fontWeight: fontWeightBold, color: textPrimary }}>
                        Button Actions
                    </h4>
                    <div className="buttons-flex-row">
                        <button className="preview-action-btn" style={{ background: btnPrimaryBg, color: btnPrimaryColor, borderRadius: borderRadiusMd, fontWeight: fontWeightMedium }}>
                            Primary
                        </button>
                        <button className="preview-action-btn" style={{ background: btnSecondaryBg, color: btnSecondaryColor, borderRadius: borderRadiusMd, fontWeight: fontWeightMedium }}>
                            Secondary
                        </button>
                        <button className="preview-action-btn" style={{ background: btnTertiaryBg, color: btnTertiaryColor, borderRadius: borderRadiusMd, fontWeight: fontWeightMedium }}>
                            Tertiary
                        </button>
                        <button className="preview-action-btn" style={{ background: btnInverseBg, color: btnInverseColor, border: `1px solid ${borderColor}`, borderRadius: borderRadiusMd, fontWeight: fontWeightMedium }}>
                            Inverse
                        </button>
                    </div>
                </div>

                {/* Form Controls & Badges */}
                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                    <div className="preview-card-block" style={{ borderRadius: borderRadiusMd, border: `${borderWidthSm} solid ${borderColor}`, boxShadow: boxShadowMd }}>
                        <h4 style={{ margin: "0 0 8px 0", fontSize: "12.5px", fontWeight: fontWeightBold, color: textPrimary }}>
                            Inputs & Focus Glow
                        </h4>
                        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                            <input
                                type="text"
                                value="Standard Field"
                                readOnly
                                style={{ padding: "7px 10px", borderRadius: "6px", border: `1px solid ${inputBorderColor}`, fontSize: "12px", color: textPrimary, width: "100%", boxSizing: "border-box" }}
                            />
                            <input
                                type="text"
                                value="Focused Field Glow"
                                readOnly
                                style={{ padding: "7px 10px", borderRadius: "6px", border: `1.5px solid ${focusColor}`, boxShadow: `0 0 0 3px ${focusColor}22`, fontSize: "12px", color: textPrimary, width: "100%", boxSizing: "border-box" }}
                            />
                        </div>
                    </div>

                    <div className="preview-card-block" style={{ borderRadius: borderRadiusMd, border: `${borderWidthSm} solid ${borderColor}`, boxShadow: boxShadowMd }}>
                        <h4 style={{ margin: "0 0 8px 0", fontSize: "12.5px", fontWeight: fontWeightBold, color: textPrimary }}>
                            Status Feedback Badges
                        </h4>
                        <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", alignItems: "center" }}>
                            <span style={{ background: `${successColor}15`, color: successColor, border: `1px solid ${successColor}40`, padding: "3px 8px", borderRadius: "10px", fontSize: "11px", fontWeight: fontWeightBold }}>
                                Success
                            </span>
                            <span style={{ background: `${errorColor}15`, color: errorColor, border: `1px solid ${errorColor}40`, padding: "3px 8px", borderRadius: "10px", fontSize: "11px", fontWeight: fontWeightBold }}>
                                Error
                            </span>
                            <span style={{ background: `${focusColor}15`, color: focusColor, border: `1px solid ${focusColor}40`, padding: "3px 8px", borderRadius: "10px", fontSize: "11px", fontWeight: fontWeightBold }}>
                                Active
                            </span>
                        </div>
                    </div>
                </div>

                {/* Typography & Link */}
                <div className="preview-card-block" style={{ borderRadius: borderRadiusMd, border: `${borderWidthSm} solid ${borderColor}`, boxShadow: boxShadowLg }}>
                    <h4 style={{ margin: "0 0 4px 0", fontSize: "14px", fontWeight: fontWeightBold, color: textPrimary }}>
                        Typography & Hyperlinks
                    </h4>
                    <p style={{ margin: "0 0 8px 0", fontSize: "12px", color: textSecondary, fontWeight: fontWeightRegular }}>
                        Body text sample. <a href="#preview" onClick={(e) => e.preventDefault()} style={{ color: linkNormal, fontWeight: fontWeightMedium, textDecoration: "underline" }}>Interactive Link</a>.
                    </p>
                    <div style={{ background: secondaryGradient, height: "6px", borderRadius: "3px" }} />
                </div>
            </div>
        </div>
    );
});

LiveThemePreviewPanel.displayName = "LiveThemePreviewPanel";

/**
 * Helper to update nested object properties immutably without JSON.parse/stringify overhead.
 */
const setNestedValue = (obj, keys, value) => {
    if (!keys || keys.length === 0) return value;
    const [head, ...tail] = keys;
    return {
        ...obj,
        [head]: tail.length === 0 ? value : setNestedValue(obj?.[head] || {}, tail, value),
    };
};

/**
 * EmployeeThemeBuilder Component
 * Provides a clean, modern UI allowing administrators and designers to configure
 * the full ThemeConfig.
 * 
 * First Step: Define base Color01 to Color15 color palette slots using color pickers.
 * Subsequent Steps: Select colors exclusively via predefined palette dropdowns.
 */
const EmployeeThemeBuilder = () => {
    const { t } = useTranslation();

    /**
     * Localization helper: returns translation if key exists in locale bundle,
     * otherwise returns human-readable fallback string.
     */
    const getI18nText = useCallback((key, fallback) => {
        const translated = t(key);
        return translated && translated !== key ? translated : fallback;
    }, [t]);

    // Load default base structure from ThemeConfig
    const initialConfig = useMemo(() => {
        return ThemeConfig?.[0]?.config?.theme?.extend || {};
    }, []);

    // Palette State (Color01 to Color15)
    const [palette, setPalette] = useState(DEFAULT_PALETTE);

    // Full Theme Extended State
    const [themeExtend, setThemeExtend] = useState(initialConfig);

    // Active UI States
    const [activeTab, setActiveTab] = useState("palette");
    const [showJsonModal, setShowJsonModal] = useState(false);
    const [showSaveModal, setShowSaveModal] = useState(false);
    const [themeName, setThemeName] = useState("Employee Theme");
    const [isDefaultTheme, setIsDefaultTheme] = useState(true);
    const [copiedJson, setCopiedJson] = useState(false);
    const [toast, setToast] = useState(null);

    // Auto-dismiss toast notification after 5 seconds
    useEffect(() => {
        if (toast) {
            const timer = setTimeout(() => {
                setToast(null);
            }, 5000);
            return () => clearTimeout(timer);
        }
    }, [toast]);

    /**
     * Updates a single color in the Color01-Color15 palette and dynamically propagates it
     * to matching theme tokens if configured.
     */
    const handlePaletteChange = useCallback((key, newValue) => {
        setPalette((prevPalette) => ({ ...prevPalette, [key]: newValue }));

        // Dynamic Binding: Update corresponding themeExtend color keys automatically
        setThemeExtend((prev) => {
            const nextColors = { ...prev.colors };
            if (key === "Color01") nextColors.primary = { ...(nextColors.primary || {}), main: newValue };
            if (key === "Color02") nextColors.primary = { ...(nextColors.primary || {}), light: newValue };
            if (key === "Color03") nextColors.primary = { ...(nextColors.primary || {}), dark: newValue };
            if (key === "Color04") nextColors.secondary = newValue;
            if (key === "Color05") nextColors.text = { ...(nextColors.text || {}), primary: newValue };
            if (key === "Color06") nextColors.text = { ...(nextColors.text || {}), secondary: newValue };
            if (key === "Color07") nextColors.link = { ...(nextColors.link || {}), normal: newValue };
            if (key === "Color08") nextColors.link = { ...(nextColors.link || {}), hover: newValue };
            if (key === "Color09") nextColors.border = newValue;
            if (key === "Color10") {
                nextColors["input-border"] = newValue;
                nextColors.inputBorder = newValue;
            }
            if (key === "Color11") nextColors.focus = newValue;
            if (key === "Color12") nextColors.error = newValue;
            if (key === "Color13") nextColors.success = newValue;

            const nextBorderColor = { ...(prev.borderColor || {}) };
            if (key === "Color01") nextBorderColor.primary = newValue;
            if (key === "Color04") nextBorderColor.secondary = newValue;
            if (key === "Color06") nextBorderColor.tertiary = newValue;
            if (key === "Color14") nextBorderColor.quaternary = newValue;
            if (key === "Color09") nextBorderColor.quinary = newValue;
            if (key === "Color15") nextBorderColor.senary = newValue;

            const nextBoxShadowColor = { ...(prev.boxShadowColor || {}) };
            if (key === "Color01") nextBoxShadowColor.primary = newValue;
            if (key === "Color04") nextBoxShadowColor.secondary = newValue;
            if (key === "Color06") nextBoxShadowColor.tertiary = newValue;
            if (key === "Color14") nextBoxShadowColor.quaternary = newValue;
            if (key === "Color09") nextBoxShadowColor.quinary = newValue;
            if (key === "Color15") nextBoxShadowColor.senary = newValue;

            const nextButton = { ...(prev.button || {}) };
            if (key === "Color01") nextButton.primary = { ...(nextButton.primary || {}), background: newValue };
            if (key === "Color04") nextButton.secondary = { ...(nextButton.secondary || {}), background: newValue };
            if (key === "Color06") nextButton.tertiary = { ...(nextButton.tertiary || {}), background: newValue };
            if (key === "Color15") nextButton.inverse = { ...(nextButton.inverse || {}), background: newValue };

            return {
                ...prev,
                colors: nextColors,
                borderColor: nextBorderColor,
                boxShadowColor: nextBoxShadowColor,
                button: nextButton,
            };
        });
    }, []);

    /**
     * Nested field value update helper using dot notation paths (lightning fast, shallow copy).
     */
    const handleThemeFieldChange = useCallback((path, value) => {
        setThemeExtend((prev) => setNestedValue(prev, path.split("."), value));
    }, []);

    /**
     * Parses linear-gradient string to extract direction, color1, and color2 or returns fallbacks.
     */
    const parseGradientDetails = useCallback((gradientStr, fallbackKey1 = "Color01", fallbackKey2 = "Color02") => {
        const defaultColor1 = palette[fallbackKey1] || DEFAULT_PALETTE[fallbackKey1];
        const defaultColor2 = palette[fallbackKey2] || DEFAULT_PALETTE[fallbackKey2];

        if (!gradientStr) {
            return { direction: "to right", color1: defaultColor1, color2: defaultColor2 };
        }

        let direction = "to right";
        if (gradientStr.includes("to left")) direction = "to left";
        else if (gradientStr.includes("to top")) direction = "to top";
        else if (gradientStr.includes("to bottom")) direction = "to bottom";
        else if (gradientStr.includes("to right")) direction = "to right";

        const hexes = gradientStr.match(/#(?:[0-9a-fA-F]{3}){1,2}\b/g);
        const color1 = hexes && hexes[0] ? hexes[0] : defaultColor1;
        const color2 = hexes && hexes[1] ? hexes[1] : defaultColor2;

        return { direction, color1, color2 };
    }, [palette]);

    const handleGradientChange = useCallback((gKey, direction, color1, color2) => {
        const newGradientValue = `linear-gradient(${direction}, ${color1}, ${color2})`;
        handleThemeFieldChange(`gradient.${gKey}`, newGradientValue);
    }, [handleThemeFieldChange]);

    // Compile final JSON structure matching ThemeConfig object format
    const compiledFullConfig = useMemo(() => {
        return {
            tenantId: Digit.ULBService.getCurrentTenantId() || "pg",
            themeType: "EMPLOYEE",
            themeName: themeName || "Employee Theme",
            isActive: true,
            isDefault: isDefaultTheme ? "yes" : "no",
            status: isDefaultTheme ? "DEFAULT" : "CUSTOM",
            workflowId: null,
            Done: true,
            done: true,
            config: {
                theme: {
                    screens: { dt: "780px" },
                    extend: themeExtend,
                },
            },
        };
    }, [themeExtend, themeName, isDefaultTheme]);

    const handleCopyJson = useCallback(() => {
        navigator.clipboard?.writeText(JSON.stringify(compiledFullConfig, null, 2)).then(() => {
            setCopiedJson(true);
            setTimeout(() => setCopiedJson(false), 2000);
        });
    }, [compiledFullConfig]);

    // Opens popup modal to enter Theme Name & Default Theme setting
    const handleSaveTheme = useCallback(() => {
        setShowSaveModal(true);
    }, []);

    // Confirms and triggers API call after clicking Save in modal popup
    const handleConfirmSaveTheme = useCallback(async () => {
        if (!themeName || !themeName.trim()) {
            setToast({
                key: "error",
                label: getI18nText("WBH_THEME_NAME_REQUIRED", "Please enter a theme name."),
            });
            return;
        }

        const payload = {
            themeConfig: {
                ...compiledFullConfig,
                isDefault: isDefaultTheme ? "yes" : "no",
                Done: true,
                done: true,
            },
            isDefault: isDefaultTheme ? "yes" : "no",
            Done: true,
            done: true,
        };

        console.log("Submitting Theme Config Payload:", JSON.stringify(payload, null, 2));

        try {
            const response = await Digit.CustomService.getResponse({
                url: "/mdms-v2/v1/theme-config/_create",
                method: "POST",
                userService: true,
                body: payload,
            });

            setShowSaveModal(false);
            setToast({
                key: "success",
                label: getI18nText("WBH_EMPLOYEE_THEME_SAVE_SUCCESS", "Employee Theme Configuration saved successfully!"),
            });
        } catch (err) {
            console.error("Save Theme API Error:", err);
            const errorMessage =
                err?.response?.data?.Errors?.[0]?.message ||
                err?.response?.data?.Errors?.[0]?.code ||
                err?.message ||
                getI18nText("WBH_EMPLOYEE_THEME_SAVE_ERROR", "Failed to save theme configuration.");

            setToast({
                key: "error",
                label: errorMessage,
            });
        }
    }, [themeName, isDefaultTheme, compiledFullConfig, getI18nText]);

    return (
        <div className="employee-theme-builder-wrapper">
            {/* Modern Header Banner */}
            <div className="theme-builder-header-banner">
                <div className="header-text-container">
                    <h2 className="header-title">
                        {t("WBH_EMPLOYEE_THEME_BUILDER") || "Employee Theme Builder"}
                    </h2>
                    <p className="header-description">
                        {t("WBH_THEME_BUILDER_DESC") || "Define your 15-color palette tokens (Color01 to Color15) and assign them across component styles seamlessly."}
                    </p>
                </div>

                <div className="header-actions-container">
                    <button
                        onClick={() => setShowJsonModal(true)}
                        className="preview-json-btn"
                    >
                        <ThemePreviewIcon fill="#FFFFFF" width="16" height="16" />
                        {t("WBH_PREVIEW_JSON") || "Preview JSON"}
                    </button>
                    <SubmitButton
                        label={t("WBH_SAVE_THEME") || "SAVE THEME CONFIG"}
                        onClick={handleSaveTheme}
                    />
                </div>
            </div>

            {/* Sleek Modern Pill Tabs */}
            <div className="theme-builder-tabs-container">
                {[
                    { id: "palette", label: getI18nText("WBH_TAB_PALETTE", "1. Palette (Color01 - Color15)") },
                    { id: "colors", label: getI18nText("WBH_TAB_COLORS", "2. Theme Colors") },
                    { id: "gradients", label: getI18nText("WBH_TAB_GRADIENTS", "3. Gradients") },
                    { id: "typography", label: getI18nText("WBH_TAB_TYPOGRAPHY", "4. Typography") },
                    { id: "layout", label: getI18nText("WBH_TAB_LAYOUT", "5. Spacing, Border & Radius") },
                    { id: "shadows", label: getI18nText("WBH_TAB_SHADOWS", "6. Shadows") },
                    { id: "buttons", label: getI18nText("WBH_TAB_BUTTONS", "7. Buttons & Actions") },
                ].map((tab) => {
                    const isActive = activeTab === tab.id;
                    return (
                        <button
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)}
                            className={`theme-tab-btn ${isActive ? "active" : ""}`}
                        >
                            {tab.label}
                        </button>
                    );
                })}
            </div>

            {/* ── STEP 1: Color01 to Color15 Palette Configuration ───────────────────────────────── */}
            {activeTab === "palette" && (
                <div>
                    <SectionHeader number={1} title={t("Color01 to Color15 Predefined Color Palette")} />
                    <Card className="theme-card-margin">
                        <CardTitle
                            icon={<ThemeBrandIcon fill="#000000" width="20" height="20" />}
                            title={t("Base Color Swatches (Color01 – Color15)")}
                            description={t("Define your 15 generic color slots using color pickers. Theme components will select from these predefined slots.")}
                        />
                        <div className="palette-grid-container">
                            {Object.keys(DEFAULT_PALETTE).map((cKey) => {
                                const colorVal = palette[cKey];
                                return (
                                    <div
                                        key={cKey}
                                        style={{
                                            border: "1.5px solid #E2E8F0",
                                            borderRadius: "10px",
                                            padding: "14px",
                                            backgroundColor: "#FFFFFF",
                                            boxShadow: "0 2px 6px rgba(0,0,0,0.02)",
                                            transition: "transform 0.2s ease, box-shadow 0.2s ease",
                                        }}
                                    >
                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "10px" }}>
                                            <span
                                                style={{
                                                    fontWeight: "800",
                                                    fontSize: "12px",
                                                    color: "#a82227",
                                                    backgroundColor: "#FFF5F5",
                                                    padding: "2px 8px",
                                                    borderRadius: "12px",
                                                    border: "1px solid #FFE3E3",
                                                }}
                                            >
                                                {cKey}
                                            </span>
                                        </div>
                                        <ColorField
                                            label=""
                                            value={colorVal}
                                            onChange={(v) => handlePaletteChange(cKey, v)}
                                        />
                                    </div>
                                );
                            })}
                        </div>
                    </Card>
                </div>
            )}

            {/* ── STEP 2: Theme Colors Mapping (Predefined Palette Dropdowns) ──────────── */}
            {activeTab === "colors" && (
                <div>
                    <SectionHeader number={2} title={t("Theme Color Assignment")} />
                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeBrandIcon fill="#000000" width="20" height="20" />} title={t("Primary Colors")} description={t("Select predefined palette colors for main brand attributes")} />
                        <PaletteFieldGroup
                            fields={PRIMARY_COLOR_FIELDS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeTextIcon fill="#000000" width="20" height="20" />} title={t("Text & Links")} description={t("Select predefined palette colors for text and hyperlinks")} />
                        <PaletteFieldGroup
                            fields={TEXT_LINK_COLOR_FIELDS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeBorderIcon fill="#000000" width="20" height="20" />} title={t("Borders & Feedback")} description={t("Select predefined palette colors for inputs, focus, and feedback states")} />
                        <PaletteFieldGroup
                            fields={BORDER_FEEDBACK_COLOR_FIELDS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>
                </div>
            )}

            {/* ── STEP 3: Gradients Configuration ────────────────────────────────────── */}
            {activeTab === "gradients" && (
                <div>
                    <SectionHeader number={3} title={t("Gradients Configuration")} />
                    {[
                        { key: "primary", title: t("Primary Gradient"), fallback1: "Color01", fallback2: "Color02" },
                        { key: "secondary", title: t("Secondary Gradient"), fallback1: "Color04", fallback2: "Color06" },
                        { key: "tertiary", title: t("Tertiary Gradient"), fallback1: "Color03", fallback2: "Color02" },
                        { key: "quaternary", title: t("Quaternary Gradient"), fallback1: "Color06", fallback2: "Color14" },
                        { key: "quinary", title: t("Quinary Gradient"), fallback1: "Color14", fallback2: "Color15" },
                        { key: "senary", title: t("Senary Gradient"), fallback1: "Color15", fallback2: "Color09" },
                    ].map((gItem) => {
                        const currentGradientStr = themeExtend?.gradient?.[gItem.key] || `linear-gradient(to right, ${palette[gItem.fallback1] || DEFAULT_PALETTE[gItem.fallback1]}, ${palette[gItem.fallback2] || DEFAULT_PALETTE[gItem.fallback2]})`;
                        const { direction, color1, color2 } = parseGradientDetails(currentGradientStr, gItem.fallback1, gItem.fallback2);

                        return (
                            <Card key={gItem.key} className="theme-card-margin">
                                <CardTitle
                                    icon={<ThemeGradientsIcon fill="#000000" width="20" height="20" />}
                                    title={gItem.title}
                                    description={t("Configure gradient direction and select two palette colors.")}
                                />
                                {/* Gradient Live Swatch Banner */}
                                <div
                                    style={{
                                        height: "44px",
                                        borderRadius: "10px",
                                        background: currentGradientStr,
                                        boxShadow: "0 2px 10px rgba(0,0,0,0.06)",
                                        border: "1px solid #E2E8F0",
                                        marginBottom: "16px",
                                        display: "flex",
                                        alignItems: "center",
                                        justifyContent: "flex-start",
                                        padding: "0 24px",
                                        color: "#FFFFFF",
                                        fontWeight: "700",
                                        fontSize: "13px",
                                        textShadow: "0 1px 3px rgba(0,0,0,0.7)",
                                        letterSpacing: "0.5px"
                                    }}
                                >
                                    {currentGradientStr}
                                </div>
                                <FieldsRow>
                                    <SelectField
                                        label={t("Gradient Direction")}
                                        value={direction}
                                        options={[
                                            { label: t("To Right (→)"), value: "to right" },
                                            { label: t("To Left (←)"), value: "to left" },
                                            { label: t("To Top (↑)"), value: "to top" },
                                            { label: t("To Bottom (↓)"), value: "to bottom" }
                                        ]}
                                        onChange={(newDir) => handleGradientChange(gItem.key, newDir, color1, color2)}
                                    />
                                    <PaletteSelectField
                                        label={t("Start Color (Color 1)")}
                                        value={color1}
                                        palette={palette}
                                        paletteDescriptions={PALETTE_DESCRIPTIONS}
                                        onChange={(newC1) => handleGradientChange(gItem.key, direction, newC1, color2)}
                                    />
                                    <PaletteSelectField
                                        label={t("End Color (Color 2)")}
                                        value={color2}
                                        palette={palette}
                                        paletteDescriptions={PALETTE_DESCRIPTIONS}
                                        onChange={(newC2) => handleGradientChange(gItem.key, direction, color1, newC2)}
                                    />
                                </FieldsRow>
                            </Card>
                        );
                    })}
                </div>
            )}

            {/* ── STEP 4: Typography ─────────────────────────────────────────────────── */}
            {activeTab === "typography" && (
                <div>
                    <SectionHeader number={4} title={t("Typography Settings")} />
                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeTypographyIcon fill="#000000" width="20" height="20" />} title={t("Font Family")} description={t("Primary and secondary font faces")} />
                        <FieldsRow>
                            <SelectField
                                label={t("Sans Font Family")}
                                value={themeExtend?.fontFamily?.sans?.[0] || "Roboto"}
                                options={["Roboto", "Inter", "Open Sans", "Poppins", "Nunito", "Montserrat"]}
                                onChange={(v) => handleThemeFieldChange("fontFamily.sans", [v, "sans-serif"])}
                            />
                            <SelectField
                                label={t("Condensed Font Family")}
                                value={themeExtend?.fontFamily?.rc?.[0] || "Roboto Condensed"}
                                options={["Roboto Condensed", "Arial Narrow", "Oswald"]}
                                onChange={(v) => handleThemeFieldChange("fontFamily.rc", [v, "sans-serif"])}
                            />
                        </FieldsRow>
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeTypographyIcon fill="#000000" width="20" height="20" />} title={t("Font Weights")} description={t("Easily increase or decrease numeric font weight values for regular, medium, and bold text")} />
                        <FieldsRow>
                            <NumberField
                                label={t("Regular Weight")}
                                value={themeExtend?.fontWeight?.regular || "400"}
                                onChange={(v) => handleThemeFieldChange("fontWeight.regular", String(v))}
                            />
                            <NumberField
                                label={t("Medium Weight")}
                                value={themeExtend?.fontWeight?.medium || "500"}
                                onChange={(v) => handleThemeFieldChange("fontWeight.medium", String(v))}
                            />
                            <NumberField
                                label={t("Bold Weight")}
                                value={themeExtend?.fontWeight?.bold || "700"}
                                onChange={(v) => handleThemeFieldChange("fontWeight.bold", String(v))}
                            />
                        </FieldsRow>
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeTypographyIcon fill="#000000" width="20" height="20" />} title={t("Font Sizes (px)")} description={t("Heading and body scale tokens")} />
                        <FieldsRow>
                            {Object.entries(themeExtend?.fontSize || {}).map(([sKey, sVal]) => (
                                <NumberField
                                    key={sKey}
                                    label={t(`Size ${sKey.toUpperCase()}`)}
                                    value={sVal}
                                    unit="px"
                                    onChange={(v) => handleThemeFieldChange(`fontSize.${sKey}`, v)}
                                />
                            ))}
                        </FieldsRow>
                    </Card>
                </div>
            )}

            {/* ── STEP 5: Spacing, Border & Radius ─────────────────────────────────── */}
            {activeTab === "layout" && (
                <div>
                    <SectionHeader number={5} title={t("Spacing, Border & Radius")} />
                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeBorderIcon fill="#000000" width="20" height="20" />} title={t("Spacing Tokens")} description={t("Margins and padding utilities")} />
                        <FieldsRow>
                            {Object.entries(themeExtend?.spacing || {}).map(([spKey, spVal]) => {
                                if (spKey.includes("/")) return null;
                                return (
                                    <NumberField
                                        key={spKey}
                                        label={t(`Spacing ${spKey.toUpperCase()}`)}
                                        value={spVal}
                                        unit="px"
                                        onChange={(v) => handleThemeFieldChange(`spacing.${spKey}`, v)}
                                    />
                                );
                            })}
                        </FieldsRow>
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeRadiusIcon fill="#000000" width="20" height="20" />} title={t("Border Radius Tokens (px)")} description={t("Corner rounding options")} />
                        <FieldsRow>
                            {Object.entries(themeExtend?.borderRadius || {}).map(([rKey, rVal]) => (
                                <NumberField
                                    key={rKey}
                                    label={t(`Radius ${rKey.toUpperCase()}`)}
                                    value={rVal}
                                    unit="px"
                                    onChange={(v) => handleThemeFieldChange(`borderRadius.${rKey}`, v)}
                                />
                            ))}
                        </FieldsRow>
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeBorderIcon fill="#000000" width="20" height="20" />} title={t("Border Width Tokens (px)")} description={t("Stroke thickness scale")} />
                        <FieldsRow>
                            {Object.entries(themeExtend?.borderWidth || {}).map(([wKey, wVal]) => (
                                <NumberField
                                    key={wKey}
                                    label={t(`Width ${wKey.toUpperCase()}`)}
                                    value={wVal}
                                    unit="px"
                                    onChange={(v) => handleThemeFieldChange(`borderWidth.${wKey}`, v)}
                                />
                            ))}
                        </FieldsRow>
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeBorderIcon fill="#000000" width="20" height="20" />} title={t("Border Colors")} description={t("Select predefined palette colors for border tokens")} />
                        <PaletteFieldGroup
                            fields={BORDER_COLOR_TOKENS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>
                </div>
            )}

            {/* ── STEP 6: Shadows ────────────────────────────────────────────────────── */}
            {activeTab === "shadows" && (
                <div>
                    <SectionHeader number={6} title={t("Box Shadows")} />
                    {Object.entries(themeExtend?.boxShadow || {}).map(([shKey, shVal]) => (
                        <Card key={shKey} className="theme-card-margin">
                            <CardTitle icon={<ThemeShadowsIcon fill="#000000" width="20" height="20" />} title={t(`Shadow ${shKey.toUpperCase()}`)} description={t("Elevation box-shadow declaration")} />
                            <ShadowField
                                label={t(`Shadow ${shKey.toUpperCase()}`)}
                                value={shVal}
                                palette={palette}
                                paletteDescriptions={PALETTE_DESCRIPTIONS}
                                onChange={(v) => handleThemeFieldChange(`boxShadow.${shKey}`, v)}
                            />
                            {/* Live Interactive In-Card Shadow Preview Box */}
                            <div
                                style={{
                                    marginTop: "16px",
                                    padding: "18px 20px",
                                    backgroundColor: "#FFFFFF",
                                    borderRadius: themeExtend?.borderRadius?.md || "10px",
                                    boxShadow: shVal,
                                    border: "1px solid #F1F5F9",
                                    display: "flex",
                                    alignItems: "center",
                                    justify: "center",
                                    minHeight: "44px"
                                }}
                            >
                                <span style={{ fontWeight: "700", fontSize: "13.5px", color: themeExtend?.colors?.text?.primary || "#1E293B" }}>
                                    Live Elevation Preview
                                </span>
                            </div>
                        </Card>
                    ))}
                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeShadowsIcon fill="#000000" width="20" height="20" />} title={t("Box Shadow Colors")} description={t("Select predefined palette colors for box shadow color tokens")} />
                        <PaletteFieldGroup
                            fields={BOX_SHADOW_COLOR_TOKENS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeShadowsIcon fill="#000000" width="20" height="20" />} title={t("Box Shadow Opacity")} description={t("Opacity scale values for shadow levels")} />
                        <FieldsRow>
                            {Object.entries(themeExtend?.boxShadowOpacity || {
                                sm: "0.05",
                                md: "0.1",
                                lg: "0.15",
                                xl: "0.2",
                                "2xl": "0.25",
                            }).map(([opKey, opVal]) => (
                                <NumberField
                                    key={opKey}
                                    label={t(`Opacity ${opKey.toUpperCase()}`)}
                                    value={opVal}
                                    step="0.01"
                                    onChange={(v) => handleThemeFieldChange(`boxShadowOpacity.${opKey}`, String(v))}
                                />
                            ))}
                        </FieldsRow>
                    </Card>
                </div>
            )}

            {/* ── STEP 7: Buttons (Predefined Palette Dropdowns) ────────────────────── */}
            {activeTab === "buttons" && (
                <div>
                    <SectionHeader number={7} title={t("Button Styles")} />
                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeTextIcon fill="#000000" width="20" height="20" />} title={t("Primary Button")} description={t("Select predefined palette colors for primary action button")} />
                        <PaletteFieldGroup
                            fields={BUTTON_PRIMARY_FIELDS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeTextIcon fill="#000000" width="20" height="20" />} title={t("Secondary Button")} description={t("Select predefined palette colors for secondary action button")} />
                        <PaletteFieldGroup
                            fields={BUTTON_SECONDARY_FIELDS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeTextIcon fill="#000000" width="20" height="20" />} title={t("Tertiary Button")} description={t("Select predefined palette colors for tertiary action button")} />
                        <PaletteFieldGroup
                            fields={BUTTON_TERTIARY_FIELDS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>

                    <Card className="theme-card-margin">
                        <CardTitle icon={<ThemeTextIcon fill="#000000" width="20" height="20" />} title={t("Inverse Button")} description={t("Select predefined palette colors for inverse action button")} />
                        <PaletteFieldGroup
                            fields={BUTTON_INVERSE_FIELDS}
                            themeExtend={themeExtend}
                            palette={palette}
                            paletteDescriptions={PALETTE_DESCRIPTIONS}
                            onChange={handleThemeFieldChange}
                            t={t}
                        />
                    </Card>
                </div>
            )}

            {/* ── Save Theme Confirmation Modal ────────────────────────────────────── */}
            {showSaveModal && (
                <Modal
                    headerBarMain={<h3 className="modal-header-title">{getI18nText("WBH_SAVE_THEME_TITLE", "Save Theme Configuration")}</h3>}
                    actionCancelLabel={getI18nText("WBH_CANCEL", "Cancel")}
                    actionCancelOnSubmit={() => setShowSaveModal(false)}
                    actionSaveLabel={getI18nText("WBH_SAVE_THEME_SUBMIT", "Save Theme")}
                    actionSaveOnSubmit={handleConfirmSaveTheme}
                    popupStyles={{ width: "500px", minHeight: "unset", height: "auto", borderRadius: "12px" }}
                    popupModuleMianStyles={{ minHeight: "unset", padding: "18px 20px 16px 20px" }}
                >
                    <div className="save-theme-modal-container">
                        {/* Theme Name Input */}
                        <div className="modal-form-group">
                            <label className="modal-field-label">
                                {getI18nText("WBH_THEME_NAME", "Theme Name")} <span className="required-asterisk">*</span>
                            </label>
                            <input
                                type="text"
                                value={themeName}
                                onChange={(e) => setThemeName(e.target.value)}
                                placeholder={getI18nText("WBH_THEME_NAME_PLACEHOLDER", "Enter theme name (e.g., Default Employee Theme)")}
                                className="modal-text-input"
                            />
                        </div>

                        {/* Set as Default Theme Radio Buttons */}
                        <div className="modal-form-group">
                            <label className="modal-field-label">
                                {getI18nText("WBH_SET_AS_DEFAULT_THEME", "Set as Default Theme?")}
                            </label>
                            <RadioButtons
                                options={[
                                    { code: "YES", label: getI18nText("WBH_YES", "Yes"), value: true },
                                    { code: "NO", label: getI18nText("WBH_NO", "No"), value: false }
                                ]}
                                optionsKey="label"
                                selectedOption={isDefaultTheme ? { code: "YES", label: getI18nText("WBH_YES", "Yes"), value: true } : { code: "NO", label: getI18nText("WBH_NO", "No"), value: false }}
                                onSelect={(val) => setIsDefaultTheme(val.value)}
                                className="modal-radio-container"
                                innerStyles={{ transform: "scale(0.7)", transformOrigin: "left center", display: "inline-flex", alignItems: "center" }}
                                inputStyle={{ fontSize: "18px", fontWeight: "600", color: "#1E293B" }}
                            />
                        </div>
                    </div>
                </Modal>
            )}

            {/* ── JSON Preview Modal ─────────────────────────────────────────────────── */}
            {showJsonModal && (
                <Modal
                    headerBarMain={<h3 className="modal-header-title">{t("WBH_THEME_CONFIG_JSON") || "Compiled Employee Theme JSON"}</h3>}
                    actionCancelLabel={t("CLOSE") || "Close"}
                    actionCancelOnSubmit={() => setShowJsonModal(false)}
                    actionSaveLabel={copiedJson ? t("COPIED!") || "Copied!" : t("COPY_JSON") || "Copy JSON"}
                    actionSaveOnSubmit={handleCopyJson}
                >
                    <div className="json-preview-container">
                        <pre className="json-code-block">
                            {JSON.stringify(compiledFullConfig, null, 2)}
                        </pre>
                    </div>
                </Modal>
            )}

            {/* Toast Notification */}
            {toast && (
                <Toast
                    error={toast.key === "error"}
                    label={toast.label}
                    onClose={() => setToast(null)}
                />
            )}
        </div>
    );
};

export default EmployeeThemeBuilder;