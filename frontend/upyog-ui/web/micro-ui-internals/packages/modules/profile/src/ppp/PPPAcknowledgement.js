import React from "react";
import { useTranslation } from "react-i18next";
import { Banner, Card, CardText, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { PROFILE_ROUTES } from "../utils/constants";
import getPPPAcknowledgementData from "../getPPPAcknowledgementData";

const PPPAcknowledgement = ({ mode = "create", familyId, acknowledgement, error, isPending = false, onGoToProfile }) => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const isCreate = mode === "create";
  const isSuccess = Boolean(familyId) && !isPending;

  const handleDownload = async () => {
    const tenantId =
      acknowledgement?.tenantId ||
      Digit.ULBService.getCitizenCurrentTenant(true) ||
      Digit.ULBService.getCurrentTenantId();
    const tenantInfo = storeData?.tenants?.find((tenant) => tenant.code === tenantId);
    const data = getPPPAcknowledgementData(
      { ...(acknowledgement || {}), familyId },
      tenantInfo,
      t
    );
    Digit.Utils.pdf.generate(data);
  };

  const goToProfile = () => {
    if (typeof onGoToProfile === "function") {
      onGoToProfile();
      return;
    }
    navigate(PROFILE_ROUTES.DETAILS);
  };

  const message = isPending
    ? t("PROFILE_PPP_ACKNOWLEDGEMENT_PENDING")
    : isSuccess
      ? t(isCreate ? "PROFILE_FAMILY_CREATED_SUCCESSFULLY" : "PROFILE_FAMILY_JOINED_SUCCESSFULLY")
      : t(isCreate ? "PROFILE_FAMILY_CREATE_FAILED" : "PROFILE_JOIN_FAMILY_FAILED");

  return (
    <React.Fragment>
      <Card>
        <Banner
          successful={isSuccess}
          message={message}
          info={isSuccess ? t(isCreate ? "CORE_COMMON_FAMILY_ID" : "CORE_JOINED_FAMILY_ID") : ""}
          applicationNumber={isSuccess ? familyId : undefined}
          infoStyles={isSuccess ? { fontSize: "24px", marginBottom: "12px" } : undefined}
          applicationNumberStyles={isSuccess ? { fontSize: "24px", marginTop: "12px" } : undefined}
        />

        {!isSuccess && !isPending && (
          <CardText>
            {error?.response?.data?.Errors?.[0]?.message ||
              error?.message ||
              t("PROFILE_PPP_ACKNOWLEDGEMENT_RETRY")}
          </CardText>
        )}

        <div
          className="profile-ppp-acknowledgement-actions"
          style={{ display: "flex", flexWrap: "wrap", gap: "16px", marginTop: "24px" }}
        >
          <SubmitBar
            label={t("PROFILE_GO_BACK_TO_PROFILE")}
            onSubmit={goToProfile}
            style={{ width: "220px", maxWidth: "100%" }}
          />
          {isSuccess && (
            <SubmitBar
              label={t("PPP_DOWNLOAD_ACK_FORM")}
              onSubmit={handleDownload}
              style={{ maxWidth: "100%" }}
            />
          )}
        </div>
      </Card>
    </React.Fragment>
  );
};

export default PPPAcknowledgement;
