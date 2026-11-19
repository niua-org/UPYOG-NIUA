import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import PPPAcknowledgement from "../ppp/PPPAcknowledgement";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * Response Page User will get when he success fully Join the family
 * He will get the family Id which he joined and a success message
 * He can go back to profile page by clicking on the button
 */
const FamilyJoinedResponse = () => {
  const navigate = Digit.Hooks.useCustomNavigate();
  const joinData = useLocation().state?.joinData || Digit.SessionStorage.get("PPP_FAMILY_JOIN_ACKNOWLEDGEMENT");

  // direct URL / refresh -> no state, send the user back
  useEffect(() => {
    if (!joinData) navigate(PROFILE_ROUTES.DETAILS, { replace: true });
  }, [joinData, navigate]);

  useEffect(() => {
    window.history.pushState(null, "", window.location.href);
    const handlePopState = () => navigate(PROFILE_ROUTES.DETAILS, { replace: true });
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [navigate]);

  if (!joinData) return null;

  return (
    <PPPAcknowledgement
      mode="join"
      familyId={joinData.familyId}
      acknowledgement={joinData}
      onGoToProfile={() => {
        Digit.SessionStorage.del("PPP_FAMILY_JOIN_ACKNOWLEDGEMENT");
        navigate(PROFILE_ROUTES.DETAILS);
      }}
    />
  );
};

export default FamilyJoinedResponse;