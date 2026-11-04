import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Card, CardHeader, CardText, CardLabel, TextInput, SubmitBar, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import OtpVerify from "../verifications/OtpVerify";
import { AADHAAR_LENGTH, FAMILY_ID_LENGTH, PROFILE_ROUTES, getFamilyIdLength } from "../utils/constants";

/**
 * JoinExistingFamily allows users to join an existing family using either
 * their Family ID or the Head of Family's Aadhaar number. Aadhaar-based
 * verification is completed through OTP before navigating to the found family details.
 */

const JoinExistingFamily = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const userUuid = Digit.UserService.getUser()?.info?.uuid;
  const [familyId, setFamilyId] = useState("");
  const [aadhaar, setAadhaar] = useState("");
  const [referenceId, setReferenceId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [otpType, setOtpType] = useState(null);

  // auto-dismiss toast
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(timer);
  }, [toast]);

  const showError = (label) => setToast({ error: true, label });

  const handleAadhaarChange = (e) => {
    const value = e.target.value;
    if (!/^\d{0,12}$/.test(value)) return;
    setAadhaar(value);
    setReferenceId(null); // aadhaar changed -> old OTP session is invalid
  };

  const handleFamilyIdChange = (e) => {
    const value = e.target.value.toUpperCase();
    if (/^[A-Z0-9-]*$/.test(value) && getFamilyIdLength(value) <= FAMILY_ID_LENGTH) {
      setFamilyId(value);
      setReferenceId(null);
    }
  };

  const sendAadhaarOtp = async () => {
    setLoading(true);
    try {
      const res = await Digit.UserService.sendAadhaarOtp({ aadhaarNumber: aadhaar });
      if (res?.status !== "OTP_SENT" || !res?.referenceId) throw new Error("OTP_NOT_SENT");
      setReferenceId(res.referenceId);
      setOtpType("AADHAAR");
    } catch (err) {
      console.log("error on join existing family page", err);
      showError(t("AADHAAR_OTP_SEND_FAILED"));
    } finally {
      setLoading(false);
    }
  };

  const searchFamilyById = async (familyId,tenantId) => {
    setLoading(true);
    try {
      const res = await Digit.UserService.sendFamilyOtp({familyId, tenantId});
      if (!res?.isSent) throw new Error("FAMILY_NOT_FOUND");
      setReferenceId(res.referenceId);
      setOtpType("FAMILY_ID");
    } catch (err) {
      console.log("error on join existing family page", err);
      showError(t("PROFILE_FAMILY_NOT_FOUND"));
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => (aadhaar ? sendAadhaarOtp() : searchFamilyById(familyId, tenantId));

  const handleVerified = (res) => {
    const verifiedData =
      otpType === "FAMILY_ID"
        ? { consentToken: res.consentToken, familyId: res.family?.familyId, family: res.family }
        : res;
    navigate(PROFILE_ROUTES.FOUND_FAMILY, { state: { verifiedData, familyId } });
  };

  const canSearch = aadhaar.length === AADHAAR_LENGTH || getFamilyIdLength(familyId) === FAMILY_ID_LENGTH;

  return (
    <React.Fragment>
      <Card>
        <CardHeader>{t("PROFILE_JOIN_A_FAMILY")}</CardHeader>
        <CardText>{t("PROFILE_JOIN_FAMILY_SUBHEADER")}</CardText>

        <CardLabel>{t("CORE_COMMON_FAMILY_ID")}</CardLabel>
        <TextInput
          name="familyId"
          value={familyId}
          onChange={handleFamilyIdChange}
          disable={!!aadhaar}
          placeholder={t("PROFILE_ENTER_FAMILY_ID")}
        />

        <CardText>{t("OR")}</CardText>

        <CardLabel>{t("CORE_COMMON_AADHAR_NUMBER_HOF")}</CardLabel>
        <TextInput
          type="tel"
          name="aadhaar"
          value={aadhaar}
          onChange={handleAadhaarChange}
          disable={!!familyId}
          placeholder={t("PROFILE_ENTER_AADHAAR")}
        />

        <SubmitBar label={t("COMMON_SEARCH")} onSubmit={handleSearch} disabled={!canSearch || loading} />

        {referenceId && (
          <OtpVerify
            type={otpType}
            referenceId={referenceId}
            userUuid={userUuid}
            familyId={familyId}
            onVerified={handleVerified}
            onError={showError}
          />
        )}
      </Card>

      {toast && <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />}
    </React.Fragment>
  );
};

export default JoinExistingFamily;