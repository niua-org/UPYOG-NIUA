import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Banner, Card, CardText, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { PROFILE_ROUTES } from "../utils/constants";
import getPPPAcknowledgementData from "../getPPPAcknowledgementData";

/**
 * PPP Acknowledgement Component
 *
 * Handles acknowledgement display for success and failure cases.
 * @param {Object} props - Acknowledgement component properties.
 * @param {"create"|"join"} [props.mode="create"] - Family operation flow.
 * @param {string} props.familyId - Identifier for the created or joined family.
 * @param {Object} [props.acknowledgement] - Acknowledgement and applicant data.
 * @param {Error|Object} [props.error] - Error returned by the family operation.
 * @param {boolean} [props.isPending=false] - Whether the operation is still pending.
 * @param {Function} [props.onGoToProfile] - Optional profile navigation callback.
 * @returns {React.ReactElement} The acknowledgement banner and available actions.
 */
const PPPAcknowledgement = ({ mode = "create", familyId, acknowledgement, error, isPending = false, onGoToProfile }) => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const isCreate = mode === "create";
  const isSuccess = Boolean(familyId) && !isPending;
  const [downloadError, setDownloadError] = useState("");
  const [isDownloading, setIsDownloading] = useState(false);

  useEffect(() => {
    if (error) {
      console.error("Family acknowledgement operation failed:", error);
    }
  }, [error]);

  const handleDownload = async () => {
    setDownloadError("");
    setIsDownloading(true);
    try {
      const tenantId =
        acknowledgement?.tenantId ||
        Digit.ULBService.getCitizenCurrentTenant(true) ||
        Digit.ULBService.getCurrentTenantId();
      const familyResponse = await Digit.UserService.familySearch(tenantId, { familyId });
      const family = familyResponse?.Families?.find((item) => item.familyId === familyId);
      if (!family) {
        throw new Error(t("PROFILE_FAMILY_NOT_FOUND"));
      }

      const tenantInfo = storeData?.tenants?.find((tenant) => tenant.code === tenantId);
      const acknowledgementData = getPPPAcknowledgementData(
        {
          ...family,
          familyId,
          tenantId,
          applicationDate: acknowledgement?.applicationDate || acknowledgement?.createdTime,
          applicantUserUuid: acknowledgement?.applicantUserUuid,
        },
        tenantInfo,
        t
      );
      await Digit.Utils.pdf.generate(acknowledgementData);
    } catch (downloadErrorCause) {
      console.error("Unable to download family acknowledgement:", downloadErrorCause);
      setDownloadError(t("PROFILE_PPP_ACKNOWLEDGEMENT_DOWNLOAD_RETRY"));
    } finally {
      setIsDownloading(false);
    }
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
            {t("PROFILE_PPP_ACKNOWLEDGEMENT_REQUEST_RETRY")}
          </CardText>
        )}
        {downloadError && <CardText>{downloadError}</CardText>}

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
              disabled={isDownloading}
              style={{ maxWidth: "100%" }}
            />
          )}
        </div>
      </Card>
    </React.Fragment>
  );
};

export default PPPAcknowledgement;
