import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import PPPAcknowledgement from "../ppp/PPPAcknowledgement";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * FamilyJoinedResponse Component
 * 
 * Clean acknowledgment screen displayed after successfully joining a family.
 * - Reads data solely from React Router navigation state.
 * - On page refresh or direct URL hit, redirects user directly to citizen home (/upyog-ui/citizen).
 * - Allows downloading acknowledgment form or returning to Profile Details.
 */
const FamilyJoinedResponse = () => {
  const navigate = Digit.Hooks.useCustomNavigate();
  const location = useLocation();

  const joinData = location.state?.joinData;
  const familyId = location.state?.familyId || joinData?.familyId;

  // Refresh or Direct URL hit -> redirect directly to citizen home page
  useEffect(() => {
    if (!familyId) {
      navigate("/upyog-ui/citizen", { replace: true });
    }
  }, [familyId, navigate]);

  // Browser Back Button -> clean redirect to Profile Details
  useEffect(() => {
    window.history.pushState(null, "", window.location.href);
    const handlePopState = () => navigate(PROFILE_ROUTES.DETAILS, { replace: true });
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [navigate]);

  if (!familyId) return null;

  return (
    <PPPAcknowledgement
      mode="join"
      familyId={familyId}
      acknowledgement={joinData}
      onGoToProfile={() => navigate(PROFILE_ROUTES.DETAILS)}
    />
  );
};

export default FamilyJoinedResponse;