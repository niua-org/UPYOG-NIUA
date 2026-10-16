/**
 * estWizardUtils.js
 * Thin EST wrappers over shared formWizardUtils (employee audience).
 */
import {
  buildWizardSteps as sharedBuildWizardSteps,
  createWizardGoNext,
  getWizardBasePath,
  isAccordionWizard,
  getNavigationPattern,
} from "@nudmcdgnpm/digit-ui-react-components";

const EST_SKIP_COMPONENTS = ["ReviewDetails"];

/**
 * Flatten MDMS wizard entries into routable employee steps.
 *
 * @param {Array} initialConfig
 * @param {string} indexRoute
 * @returns {Array}
 */
export const buildWizardSteps = (initialConfig, indexRoute) =>
  sharedBuildWizardSteps(initialConfig, {
    indexRoute,
    audience: "employee",
    skipComponents: EST_SKIP_COMPONENTS,
  });

export {
  isAccordionWizard,
  getNavigationPattern,
  getWizardBasePath,
  createWizardGoNext,
};
