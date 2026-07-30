import { useMemo } from "react";
import { Link, Outlet } from "react-router-dom";
import { useScreenSize } from "../../hooks/useScreenSize";
import { createBackgroundCardStyle, createBackgroundStyle } from "../../utils/utils";
import { OnboardingProvider, useOnboarding } from "./OnboardingContext";
import OnboardingFeatures from "./OnboardingFeatures";

/**
 * Configuration-driven visual shell. The active form step is rendered through
 * Outlet so the same branding/layout can wrap login, registration, and OTP.
 */
const OnboardingShell = ({ homePath }) => {
  const { content, error, logo, refetch } = useOnboarding();
  const screenType = useScreenSize();
  const {
    background: { root: backgroundConfig, sections: { left: leftBackgroundConfig, right: rightBackgroundConfig } = {} } = {},
    brand: brandConfig = {},
    features: featuresConfig = {},
  } = content;
  // Resolve all responsive styles in one memoized calculation. Keeping these
  // objects stable prevents needless DOM style updates while avoiding a
  // separate memo hook and dependency list for every layout region.
  const { background, leftBackground, leftCardBackground, rightBackground, rightCardBackground } = useMemo(
    () => ({
      background: createBackgroundStyle(backgroundConfig, screenType),
      leftBackground: createBackgroundStyle(leftBackgroundConfig, screenType),
      leftCardBackground: createBackgroundCardStyle(leftBackgroundConfig?.card, screenType),
      rightBackground: createBackgroundStyle(rightBackgroundConfig, screenType),
      rightCardBackground: createBackgroundCardStyle(rightBackgroundConfig?.card, screenType),
    }),
    [backgroundConfig, leftBackgroundConfig, rightBackgroundConfig, screenType]
  );

  const { showShadow: showRightCardShadow, ...rightCardStyle } = rightCardBackground ?? {};

  // Configuration failure affects the complete onboarding experience, not an
  // individual form step. Replace the whole shell so the fallback is centered
  // across the viewport rather than constrained to the right-hand form panel.
  if (error) {
    return (
      <section className="upyog-ui">
        <main className="onboarding onboarding--fallback">
          <div className="onboarding__fallback" role="alert">
            <h1>Unable to load onboarding</h1>
            <p>We could not load the onboarding configuration. Please try again.</p>
            <button type="button" className="button secondary" onClick={refetch}>
              Retry
            </button>
          </div>
        </main>
      </section>
    );
  }

  return (
    <section className="upyog-ui">
      <section className="onboarding" style={background}>
        <aside className="onboarding__content" style={leftBackground}>
          <div className="onboarding__content-wrapper" style={leftCardBackground}>
            <Link className="onboarding__logo" to={homePath} title={logo?.alt}>
              <img src={logo?.src} alt={logo?.alt} />
            </Link>

            {brandConfig?.title?.isVisible && (
              <h1 className="onboarding__title">
                {brandConfig.title.default} <span>{brandConfig.title.highlight}</span>
              </h1>
            )}

            {brandConfig?.subtitle?.isVisible && (
              <p className="onboarding__subtitle">
                {brandConfig.subtitle.primary} <span>{brandConfig.subtitle.secondary}</span>
              </p>
            )}

            <OnboardingFeatures config={featuresConfig} />
          </div>
        </aside>

        <aside className="onboarding__form" style={rightBackground}>
          <div className={`onboarding__form-wrapper${showRightCardShadow ? " shadow" : ""}`} style={rightCardStyle}>
            <Outlet />
          </div>
        </aside>
      </section>
    </section>
  );
};

/**
 * Route layout entry point. Parent routes choose the MDMS module/master and
 * home link, keeping the shared onboarding package user-type agnostic.
 */
const OnboardingLayout = ({ stateCode, moduleName, masterName, homePath, storageKey }) => (
  <OnboardingProvider stateCode={stateCode} moduleName={moduleName} masterName={masterName} storageKey={storageKey}>
    <OnboardingShell homePath={homePath} />
  </OnboardingProvider>
);

export default OnboardingLayout;
