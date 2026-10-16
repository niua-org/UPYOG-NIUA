/**
 * useEstWizard.js
 * Thin EST wrapper over shared useFormWizard — employee registration / allotment flows.
 */
import { useFormWizard } from "@nudmcdgnpm/digit-ui-react-components";

const EST_SKIP_COMPONENTS = ["ReviewDetails"];

/**
 * Shared MDMS wizard router state for EST flows.
 */
const useEstWizard = ({
  mdmsData,
  isLoading,
  indexRoute,
  sessionKey,
  terminalSegments,
  multiStepNavigation = true,
  invalidateQueryKey,
  checkFlow,
  buildSuccessAckState,
}) =>
  useFormWizard({
    mdmsData,
    isLoading,
    indexRoute,
    sessionKey,
    terminalSegments,
    multiStepNavigation,
    invalidateQueryKey,
    checkFlow,
    buildSuccessAckState,
    audience: "employee",
    skipComponents: EST_SKIP_COMPONENTS,
  });

export default useEstWizard;
