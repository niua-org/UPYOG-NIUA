import { useEffect, useState } from "react";
import { getScreenType, SCREEN_TYPES } from "../utils/utils";

const getInitialScreenType = () => {
  if (typeof window === "undefined") {
    return SCREEN_TYPES.DESKTOP;
  }

  return getScreenType(window.innerWidth);
};

export const useScreenSize = () => {
  const [screenType, setScreenType] = useState(
    getInitialScreenType
  );

  useEffect(() => {
    const handleResize = () => {
      const nextScreenType = getScreenType(
        window.innerWidth
      );

      setScreenType((currentScreenType) =>
        currentScreenType === nextScreenType
          ? currentScreenType
          : nextScreenType
      );
    };

    handleResize();

    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener(
        "resize",
        handleResize
      );
    };
  }, []);

  return screenType;
};