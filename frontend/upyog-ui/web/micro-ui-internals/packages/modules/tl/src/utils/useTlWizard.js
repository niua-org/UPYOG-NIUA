/**
 * useTlWizard.js
 * Thin TL wrapper over shared useFormWizard — citizen create session.
 */
import { useCallback } from "react";
import { useFormWizard } from "@nudmcdgnpm/digit-ui-react-components";

const TL_SKIP_COMPONENTS = ["ReviewDetails", "TLCheckPage"];

/**
 * @param {object} options — same as useFormWizard, plus TL defaults
 */
const useTlWizard = ({
  mdmsData,
  isLoading,
  indexRoute,
  sessionKey,
  sessionStepKey,
  terminalSegments,
  multiStepNavigation = true,
  invalidateQueryKey,
  transformSession,
}) => {
  const {
    onUpdateSuccess: clearSessionOnUpdate,
    ...wizard
  } = useFormWizard({
    mdmsData,
    isLoading,
    indexRoute,
    sessionKey,
    sessionStepKey,
    terminalSegments,
    multiStepNavigation,
    invalidateQueryKey,
    transformSession,
    userType: "citizen",
    skipComponents: TL_SKIP_COMPONENTS,
    docsComponent: "TradeLicense",
    formComponent: "TLCitizenNewApplication",
  });

  const onUpdateSuccess = useCallback(() => {
    sessionStorage.removeItem("CurrentFinancialYear");
    clearSessionOnUpdate();
  }, [clearSessionOnUpdate]);

  return {
    ...wizard,
    onUpdateSuccess,
  };
};

export default useTlWizard;
