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

export const createBackgroundStyle = (backgroundConfig, screenType) => {
  if (!backgroundConfig) return {};

  const { showColor, showBackgroundColor, showBackgroundImage, backgroundColor, responsive, attachment, showShadow = false } = backgroundConfig;

  const backgroundStyle = {
    showShadow,
  };

  const shouldShowBackgroundColor = showBackgroundColor ?? showColor ?? Boolean(backgroundColor);

  if (shouldShowBackgroundColor && backgroundColor) {
    backgroundStyle.backgroundColor = backgroundColor;
  }

  if (!showBackgroundImage) {
    return backgroundStyle;
  }

  const responsiveBackground = getResponsiveConfig(responsive, screenType);

  if (!responsiveBackground?.image) {
    return backgroundStyle;
  }

  return {
    ...backgroundStyle,
    backgroundImage: `url("${responsiveBackground.image}")`,
    backgroundPosition: responsiveBackground.position ?? "center center",
    backgroundSize: responsiveBackground.size ?? "cover",
    backgroundAttachment: responsiveBackground.attachment ?? attachment ?? "scroll",
    backgroundRepeat: responsiveBackground.repeat ?? "no-repeat",
  };
};

/** Apply card backgrounds; only an explicit false overrides the CSS shadow. */
export const createBackgroundCardStyle = (cardConfig, screenType) => {
  if (!cardConfig) return {};

  return createBackgroundStyle(cardConfig, screenType);
};
