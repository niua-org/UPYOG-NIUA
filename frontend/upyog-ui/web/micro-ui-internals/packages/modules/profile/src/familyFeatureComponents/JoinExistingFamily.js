import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Card, CardHeader, CardText, CardLabel, TextInput, SubmitBar, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import OtpVerify from "../verifications/OtpVerify";
import { AADHAAR_LENGTH, FAMILY_ID_LENGTH, PROFILE_ROUTES, getFamilyIdLength } from "../utils/constants";

/**
 * 
 * @description
 * Screen allowing a citizen to join an existing family group by searching via:
 * 1. Family ID (alphanumeric ID)
 * 2. Head of Family (HOF) Aadhaar Number (12 digits)
 * 
 * Flow:
 * - If searching via Aadhaar:
 *   1. Calls standard `Digit.UserService.checkAadhaarHof` service.
 *   2. Only if `res.isHof === true`, triggers `sendAadhaarOtp()`.
 *   3. If false, displays error toast and stops.
 * - If searching via Family ID: Triggers Family ID OTP directly.
 * 
 * @returns {React.ReactElement} Join Existing Family search & OTP verification UI.
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
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const [otpType, setOtpType] = useState(null);

  // Auto-dismiss toast
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(timer);
  }, [toast]);

  const showError = (label) => setToast({ error: true, label });

  const handleAadhaarChange = (e) => {
    const rawValue = e.target.value.replace(/\D/g, "").slice(0, 12);
    if (!/^\d{0,12}$/.test(rawValue)) return;
    setAadhaar(rawValue);
    setReferenceId(null); // aadhaar changed -> old OTP session is invalid
  };

  const handleFamilyIdChange = (e) => {
    const value = e.target.value.toUpperCase();
    if (/^[A-Z0-9-]*$/.test(value) && getFamilyIdLength(value) <= FAMILY_ID_LENGTH) {
      setFamilyId(value);
      setReferenceId(null);
    }
  };

  /**
   * Triggers Aadhaar OTP for HOF validation.
   */
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

  /**
   * Validates HOF status via UPYOG Digit.UserService.checkAadhaarHof service.
   * Calls sendAadhaarOtp ONLY if isHof is true.
   */
  const handleAadhaarSearch = async () => {
    setLoading(true);
    try {
      const res = await Digit.UserService.checkAadhaarHof({ aadhaarNumber: aadhaar, tenantId });
      if (res?.isHof) {
        await sendAadhaarOtp();
      } else {
        showError(res?.message || t("ERR_PROFILE_NOT_HOF"));
      }
    } catch (err) {
      console.log("error while checking HOF status", err);
      showError(
        err?.response?.data?.Errors?.[0]?.code ||
        t("ERR_PROFILE_CHECK_HOF_FAILED")
      );
    } finally {
      setLoading(false);
    }
  };

  /**
   * Triggers Family ID OTP for family validation.
   */
  const searchFamilyById = async (familyId, tenantId) => {
    setLoading(true);
    try {
      const res = await Digit.UserService.sendFamilyOtp({ familyId, tenantId });
      if (!res?.isSent) throw new Error("FAMILY_NOT_FOUND");
      setReferenceId(res.referenceId);
      setOtpType("FAMILY_ID");
    } catch (err) {
      console.log("error on join existing family page", err);
      showError(err?.response?.data?.Errors?.[0]?.message || t("PROFILE_FAMILY_NOT_FOUND"));
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => (aadhaar ? handleAadhaarSearch() : searchFamilyById(familyId, tenantId));

  /**
   * Handler executed when OTP is successfully verified.
   */
  const handleVerified = (res) => {
    const verifiedData =
      otpType === "FAMILY_ID"
        ? { consentToken: res.consentToken, familyId: res.family?.familyId, family: res.family }
        : res;

    // Persist verified family context so it survives tab navigations in createfamilyconfig
    const joinData = {
      verifiedData,
      familyId: verifiedData?.familyId || familyId,
      isJoinFlow: true,
    };
    sessionStorage.setItem("Digit.family.joinData", JSON.stringify(joinData));

    // Redirect to Create Family multi-step flow starting at profileDetails
    navigate(`${PROFILE_ROUTES.PROFILE_CREATE_FAMILY}/profileDetails?flow=join`);
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
          value={aadhaar.replace(/(\d{4})(?=\d)/g, "$1 ")}
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
            tenantId={tenantId}
          />
        )}
      </Card>

      {toast && <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />}
    </React.Fragment>
  );
};

export default JoinExistingFamily;