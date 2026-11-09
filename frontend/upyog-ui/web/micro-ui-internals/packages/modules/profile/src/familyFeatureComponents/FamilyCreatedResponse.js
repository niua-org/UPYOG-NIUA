import React, { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Card, Banner, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * FamilyCreatedResponse Component
 *
 * Success acknowledgment page displayed upon the successful creation of a Family ID.
 *
 * Key Behaviors:
 * 1. Reads the newly generated `familyId` passed via React Router navigation state (`location.state.familyId`).
 * 2. Route Guard: If accessed directly or refreshed without state, immediately redirects to Profile Details.
 * 3. History Guard: Intercepts browser/hardware Back button (`popstate`) to prevent users from navigating
 *    back into completed form steps, cleanly redirecting them to Profile Details instead.
 * 4. Displays a success `Banner` containing the generated Family ID and a primary button to return to Profile.
 */
const FamilyCreatedResponse = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  // Extract generated family ID from navigation state
  const familyId = location.state?.familyId;

  /**
   * Route Guard:
   * Protect against direct URL hits or empty state reloads.
   * If `familyId` is missing, redirect the user back to Profile Details.
   */
  useEffect(() => {
    if (!familyId) {
      navigate(PROFILE_ROUTES.DETAILS, { replace: true });
    }
  }, [familyId, navigate]);

  /**
   * Browser Back Button Interceptor:
   * Pushes a dummy state into history and listens for the `popstate` event.
   * When the user clicks the browser Back button, they are redirected to Profile Details
   * rather than stepping backward into already-submitted form steps.
   */
  useEffect(() => {
    window.history.pushState(null, "", window.location.href);

    const handlePopState = () => {
      navigate(PROFILE_ROUTES.DETAILS, { replace: true });
    };

    window.addEventListener("popstate", handlePopState);

    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, [navigate]);

  // If no family ID is available, render nothing while redirect takes effect
  if (!familyId) return null;

  return (
    <Card>
      {/* Success Banner displaying the generated Family ID */}
      <Banner
        successful={true}
        message={t("PROFILE_FAMILY_CREATED_SUCCESSFULLY")}
        info={t("CORE_COMMON_FAMILY_ID")}
        applicationNumber={familyId}
      />

      {/* Action button to return to the Citizen Profile Details dashboard */}
      <SubmitBar
        label={t("PROFILE_GO_BACK_TO_PROFILE")}
        onSubmit={() => navigate(PROFILE_ROUTES.DETAILS)}
      />
    </Card>
  );
};

export default FamilyCreatedResponse;