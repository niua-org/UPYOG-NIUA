/**
 * tlWizardUtils.js
 * Thin TL wrappers over shared formWizardUtils (citizen audience + TL skip set).
 */
import {
  buildWizardSteps as sharedBuildWizardSteps,
  createWizardGoNext,
  getWizardBasePath,
  isAccordionWizard,
  getNavigationPattern,
} from "@nudmcdgnpm/digit-ui-react-components";

const TL_SKIP_COMPONENTS = ["ReviewDetails", "TLCheckPage"];

/**
 * Flatten MDMS / local wizard entries into routable citizen steps.
 * Accordion configs collapse form body items into a single /apply route.
 *
 * @param {Array} initialConfig
 * @param {string} indexRoute
 * @returns {Array}
 */
export const buildWizardSteps = (initialConfig, indexRoute) =>
  sharedBuildWizardSteps(initialConfig, {
    indexRoute,
    audience: "citizen",
    skipComponents: TL_SKIP_COMPONENTS,
    docsComponent: "TradeLicense",
    formComponent: "TLCitizenNewApplication",
  });

export {
  isAccordionWizard,
  getNavigationPattern,
  getWizardBasePath,
  createWizardGoNext,
};
