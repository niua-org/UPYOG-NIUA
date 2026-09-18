import { useEffect, useState } from "react";
import { getScreenType, SCREEN_TYPES } from "../utils/utils";

/**
 * Resolve the first responsive category without requiring an effect.
 *
 * During server-side rendering `window` is unavailable, so desktop is used as
 * a safe deterministic fallback. In the browser, the initial state is derived
 * immediately from the current viewport to avoid rendering the wrong
 * onboarding background before the resize listener is registered.
 */
const getInitialScreenType = () => {
  if (typeof window === "undefined") {
    return SCREEN_TYPES.DESKTOP;
  }

  return getScreenType(window.innerWidth);
};

/**
 * Track only the named responsive category used by MDMS background configs.
 *
 * The hook returns `mobile`, `tablet`, `laptop`, or `desktop`, rather than the
 * raw viewport width. Components therefore re-render only when a breakpoint is
 * crossed, not for every pixel emitted while the browser is being resized.
 */
export const useScreenSize = () => {
  // Pass the initializer function itself so React evaluates it only while
  // creating the state, instead of recalculating it on every render.
  const [screenType, setScreenType] = useState(
    getInitialScreenType
  );

  useEffect(() => {
    const handleResize = () => {
      // Keep breakpoint rules centralized in getScreenType so the hook and
      // responsive style helpers cannot drift to different width boundaries.
      const nextScreenType = getScreenType(
        window.innerWidth
      );

      // Preserve the current state reference when the viewport remains within
      // the same breakpoint, preventing unnecessary consumer re-renders.
      setScreenType((currentScreenType) =>
        currentScreenType === nextScreenType
          ? currentScreenType
          : nextScreenType
      );
    };

    // Reconcile the SSR/default value with the real browser width immediately
    // after mount, including cases where the viewport changed before the effect.
    handleResize();

    // Listen for viewport changes only while a component uses this hook.
    window.addEventListener("resize", handleResize);

    return () => {
      // Remove the exact handler reference on unmount to avoid leaked listeners
      // and state updates after the consuming layout has been removed.
      window.removeEventListener(
        "resize",
        handleResize
      );
    };
  }, []);

  // Consumers use this category to choose the matching responsive MDMS entry.
  return screenType;
};
