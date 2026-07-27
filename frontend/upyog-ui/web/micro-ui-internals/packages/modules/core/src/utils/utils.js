export const SCREEN_TYPES = {
  MOBILE: "mobile",
  TABLET: "tablet",
  LAPTOP: "laptop",
  DESKTOP: "desktop",
};

export const getScreenType = (width) => {
  if (width < 768) return SCREEN_TYPES.MOBILE;
  if (width < 1024) return SCREEN_TYPES.TABLET;
  if (width < 1440) return SCREEN_TYPES.LAPTOP;

  return SCREEN_TYPES.DESKTOP;
};

/**
 * Resolves responsive config using mobile-first inheritance.
 *
 * Example:
 * - If only tablet is available, tablet is used for tablet, laptop and desktop.
 * - If tablet is missing but laptop is available, laptop is used for tablet.
 * - Exact screen configuration always gets first priority.
 */
export const getResponsiveConfig = (responsive, screenType) => {
  if (!responsive) return null;

  const fallbackOrder = {
    mobile: ["mobile", "tablet", "laptop", "desktop"],
    tablet: ["tablet", "laptop", "desktop", "mobile"],
    laptop: ["laptop", "tablet", "desktop", "mobile"],
    desktop: ["desktop", "laptop", "tablet", "mobile"],
  };

  const order =
    fallbackOrder[screenType] || fallbackOrder.desktop;

  const matchedScreen = order.find(
    (type) => responsive[type] != null
  );

  return matchedScreen ? responsive[matchedScreen] : null;
};

export const createBackgroundStyle = (
  backgroundConfig,
  screenType
) => {
  if (!backgroundConfig) return {};

  const {
    showColor,
    showBackgroundColor,
    showBackgroundImage,
    backgroundColor,
    responsive,
  } = backgroundConfig;
  // Section configs use showBackgroundColor while older root/card configs use
  // showColor. Color and image are independent layers when both are enabled.
  const shouldShowColor = showBackgroundColor ?? showColor ?? Boolean(backgroundColor);
  const shouldShowImage = showBackgroundImage ?? true;
  const style = shouldShowColor && backgroundColor ? { backgroundColor } : {};

  const responsiveBackground = getResponsiveConfig(
    responsive,
    screenType
  );

  if (!shouldShowImage || !responsiveBackground?.image) return style;

  return {
    ...style,
    backgroundImage: `url("${responsiveBackground.image}")`,
    backgroundPosition: responsiveBackground.position || "center center",
    backgroundSize: responsiveBackground.size || "cover",
    backgroundAttachment: responsiveBackground.attachment || backgroundConfig.attachment || "scroll",
    backgroundRepeat: "no-repeat",
  };
};

/** Apply card backgrounds; only an explicit false overrides the CSS shadow. */
export const createBackgroundCardStyle = (cardConfig, screenType) => {
  if (!cardConfig) return {};

  const style = createBackgroundStyle(cardConfig, screenType);
  return cardConfig.showShadow === false ? { ...style, boxShadow: "none" } : style;
};
