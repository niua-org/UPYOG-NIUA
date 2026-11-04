import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Card, Banner, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * Response Page User will get when he success fully Join the family
 * He will get the family Id which he joined and a success message
 * He can go back to profile page by clicking on the button
 */
const FamilyJoinedResponse = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const joinData = useLocation().state?.joinData;

  // direct URL / refresh -> no state, send the user back
  useEffect(() => {
    if (!joinData) navigate(PROFILE_ROUTES.DETAILS, { replace: true });
  }, [joinData, navigate]);

  if (!joinData) return null;

  return (
    <Card>
      <Banner
        successful
        message={t("PROFILE_FAMILY_JOINED_SUCCESSFULLY")}
        info={t("CORE_COMMON_FAMILY_ID")}
        complaintNumber={joinData.familyId}
      />
      <SubmitBar label={t("PROFILE_GO_BACK_TO_PROFILE")} onSubmit={() => navigate(PROFILE_ROUTES.DETAILS)} />
    </Card>
  );
};

export default FamilyJoinedResponse;