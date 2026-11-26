import React, { useState, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import {
  Card,
  CardHeader,
  CardText,
  CardLabel,
  CardLabelError,
  TextInput,
  OTPInput,
  SubmitBar,
  Loader,
  EditIcon,
  Modal,
  CloseSvg,
  Toast,
} from "@nudmcdgnpm/digit-ui-react-components";
import { buildVerifiedMemberInfo } from "../utils/payloads";
import { getFamilyMembers } from "../utils";
import { OTP_LENGTH } from "../utils/constants";

/**
 * AadhaarVerificationModal
 *
 * Unified Aadhaar verification component supporting both:
 * 1. "profile" mode: Citizen self-verification (updates user session & profile status)
 * 2. "family" mode: Family member addition (verifies candidate & caches to sessionStorage)
 *
 * Can be rendered either as an interactive popup (<Modal>) or as a standalone page (<Card>).
 *
 * @param {boolean}  [isOpen=true]          - Controls modal visibility (only relevant when isModal=true).
 * @param {boolean}  [isModal=true]         - If true renders inside a <Modal> popup; false renders as a <Card> page.
 * @param {"profile"|"family"} [mode="profile"] - Verification flow:
 *                                             "profile" → self-verification, updates localStorage session.
 *                                             "family"  → member addition, caches minimal fields to sessionStorage.
 * @param {Function} [onClose]              - Called when the modal is dismissed (X button or cancel).
 * @param {Function} [onSuccess]            - Called after successful verification.
 *                                             profile mode: receives (user) object.
 *                                             family mode:  receives ({ verifiedMember, aadhaarLast4, maskedMobile }).
 * @param {string}   [title]                - Override the modal/card heading.
 * @param {string}   [subtext]              - Optional descriptive text shown below the heading.
 * @param {boolean}  [showConsentBox=true]  - Whether to show the OTP consent callout banner.
 * @returns {React.ReactElement|null}       - Renders null when isModal=true and isOpen=false.
 */



const AadhaarVerificationModal = ({
  isOpen = true,
  isModal = true,
  mode = "profile",
  onClose,
  onSuccess,
  title,
  subtext,
  showConsentBox = true,
}) => {
  const { t } = useTranslation();
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId() || stateId;

  // Form input and session states
  const [aadhaarNumber, setAadhaarNumber] = useState("");
  const [cleanAadhaar, setCleanAadhaar] = useState("");
  const [isOtpSent, setIsOtpSent] = useState(false);
  const [referenceId, setReferenceId] = useState(null);
  const [txnId, setTxnId] = useState(null);
  const [maskedMobile, setMaskedMobile] = useState("+91 ******XXXX");
  const [linkedMobile, setLinkedMobile] = useState("");
  const [otp, setOtp] = useState("");
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  // Countdown timer state
  const [timeLeft, setTimeLeft] = useState(30);
  const [timerActive, setTimerActive] = useState(false);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);

  const navigationTimerRef = useRef(null);

  // Auto-dismiss toast
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(timer);
  }, [toast]);

  // Clean timer on unmount
  useEffect(() => {
    return () => {
      if (navigationTimerRef.current) {
        clearTimeout(navigationTimerRef.current);
      }
    };
  }, []);

  // Countdown timer effect
  useEffect(() => {
    let interval = null;
    if (timerActive && timeLeft > 0) {
      interval = setInterval(() => setTimeLeft((prev) => prev - 1), 1000);
    } else if (timeLeft === 0) {
      setTimerActive(false);
    }
    return () => clearInterval(interval);
  }, [timerActive, timeLeft]);

  const resetForm = () => {
    setAadhaarNumber("");
    setCleanAadhaar("");
    setIsOtpSent(false);
    setReferenceId(null);
    setTxnId(null);
    setMaskedMobile("+91 ******XXXX");
    setLinkedMobile("");
    setOtp("");
    setError(null);
    setTimerActive(false);
    setTimeLeft(30);
  };

  const showError = (errorMsg) => {
    setError(errorMsg);
    // Validation errors should only appear inline inside the modal, never outside as a floating toast
    if (!isModal) {
      setToast({ error: true, label: errorMsg });
    }
  };

  const handleClose = () => {
    resetForm();
    // Purge temporary session storage keys if modal is cancelled/closed without completing the flow
    // to prevent abandoned verification data from persisting and pre-filling subsequent member forms.
    if (mode === "family") {
      sessionStorage.removeItem("Digit.family.verifiedMember");
      sessionStorage.removeItem("Digit.family.aadhaarLast4");
      sessionStorage.removeItem("Digit.family.maskedMobile");
    }
    onClose?.();
  };


  /**
   * Format input into 4-digit groups (XXXX XXXX XXXX)
   */
  const handleAadhaarChange = (e) => {
    const rawVal = e.target.value.replace(/\D/g, "").slice(0, 12);
    setCleanAadhaar(rawVal);
    const formatted = rawVal.replace(/(\d{4})(?=\d)/g, "$1 ").trim();
    setAadhaarNumber(formatted);
    if (error) setError(null);
  };

  /**
   * Step 1: Send OTP to Aadhaar-linked mobile
   */
  const handleSendOtp = async () => {
    if (cleanAadhaar.length !== 12) {
      const err = t("CORE_COMMON_PROFILE_AADHAR_NUMBER_INVALID", "Please enter a valid 12-digit Aadhaar number");
      setError(err);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const requestData = { aadhaarNumber: cleanAadhaar };
      const res = await Digit.UserService.sendAadhaarOtp(requestData);

      if (res?.status === "OTP_SENT" || res?.referenceId || res?.txnId) {
        setTxnId(res?.txnId || null);
        setReferenceId(res?.referenceId || res?.txnId || null);

        if (res?.mobileNumber) {
          setMaskedMobile(`+91 ******${String(res.mobileNumber).slice(-4)}`);
          setLinkedMobile(/^\d{10}$/.test(String(res.mobileNumber)) ? String(res.mobileNumber) : "");
        }
        setTimeLeft(30);
        setTimerActive(true);
        setIsOtpSent(true);
      } else {
        throw new Error("OTP_NOT_SENT");
      }
    } catch (err) {
      const errorMsg =
        err?.response?.data?.message ||
        err?.response?.data?.Errors?.[0]?.message ||
        err?.response?.data?.status ||
        err?.message ||
        t("AADHAAR_OTP_SEND_FAILED", "Failed to send Aadhaar OTP. Please try again.");
      showError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Step 1b: Resend OTP
   */
  const handleResendOtp = async () => {
    if (timerActive && timeLeft > 0) return;
    setOtp("");
    setError(null);
    await handleSendOtp();
  };

  /**
   * Step 1c: Unlock Aadhaar input to allow editing number
   */
  const handleEditAadhaar = () => {
    setIsOtpSent(false);
    setOtp("");
    setReferenceId(null);
    setTxnId(null);
    setError(null);
    setTimerActive(false);
    setTimeLeft(30);
  };

  /**
   * Step 2: Verify OTP
   */
  const handleVerifyOtp = async () => {
    if (otp.length !== OTP_LENGTH) {
      const err = t("CS_INVALID_OTP", "Please enter a valid 6-digit OTP");
      setError(err);
      return;
    }

    setVerifying(true);
    setError(null);

    try {
      if (mode === "profile") {
        // Mode 1: Citizen self-profile verification
        const res = await Digit.UserService.verifyProfile({
          referenceId,
          otp,
          aadhaarNumber: cleanAadhaar,
          tenantId,
        });

        if (res?.user?.isIdentityVerified !== true) {
          throw new Error("PROFILE_VERIFICATION_FAILED");
        }

        const verifiedFields = {
          name: res.user.name,
          isIdentityVerified: res.user.isIdentityVerified,
          ...(res.user.gender && { gender: res.user.gender }),
          ...(res.user.dob && { dob: res.user.dob }),
        };

        const sessionUser = Digit.UserService.getUser();
        if (sessionUser) {
          const updatedUser = {
            ...sessionUser,
            info: {
              ...sessionUser.info,
              ...verifiedFields,
            },
          };

          Digit.UserService.setUser(updatedUser);
          Digit.SessionStorage.set("citizen.userRequestObject", updatedUser);
          const minimalInfo = {
            isIdentityVerified: verifiedFields.isIdentityVerified,
            name: verifiedFields.name,
          };
          localStorage.setItem("user-info", JSON.stringify({ ...JSON.parse(localStorage.getItem("user-info") || "{}"), ...minimalInfo }));
          localStorage.setItem("Citizen.user-info", JSON.stringify({ ...JSON.parse(localStorage.getItem("Citizen.user-info") || "{}"), ...minimalInfo }));

        }

        setToast({
          error: false,
          label: t("PROFILE_VERIFICATION_SUCCESS", "Aadhaar verified successfully!"),
        });

        navigationTimerRef.current = setTimeout(() => {
          onSuccess?.(res?.user);
          if (isModal) handleClose();
        }, 1200);

      } else {
        // Mode 2: Adding a family member
        const userInfo = Digit.UserService.getUser()?.info || {};
        const requestData = {
          referenceId: referenceId || txnId,
          otp,
          isResync: false,
          userUuid: userInfo?.uuid,
          tenantId,
        };

        const response = await Digit.UserService.verifyAadhaarOtp(requestData);
        const aadhaarLast4 = cleanAadhaar ? cleanAadhaar.slice(-4) : "";
        const verifiedMember = buildVerifiedMemberInfo({ response, aadhaarLast4 });
        if (!verifiedMember.mobileNumber && linkedMobile) {
          verifiedMember.mobileNumber = linkedMobile;
        }

        // Duplicate member validation
        const loggedInUuid = userInfo?.uuid;
        const memberUuid = verifiedMember?.userUuid;
        if (memberUuid && (memberUuid === loggedInUuid || getFamilyMembers().some((m) => m.userUuid === memberUuid))) {
          const duplicateMsg = t("ERR_AADHAAR_ALREADY_ADDED", "This Aadhaar number is already added to the family.");
          showError(duplicateMsg);
          setOtp("");
          setVerifying(false);
          return;
        }

        try {
          // For PII privacy compliance, never persist unmasked Aadhaar-linked mobile in browser sessionStorage.
          // The unmasked mobile is passed in-memory via onSuccess -> router navigation state instead.
          const memberToStore = {
            ...verifiedMember,
            mobileNumber: undefined,
            demographics: verifiedMember?.demographics
              ? { ...verifiedMember.demographics, mobileNumber: undefined }
              : undefined,
          };
          sessionStorage.setItem("Digit.family.verifiedMember", JSON.stringify(memberToStore));
          sessionStorage.setItem("Digit.family.aadhaarLast4", aadhaarLast4);
          sessionStorage.setItem("Digit.family.maskedMobile", maskedMobile);
        } catch (storageErr) {
          console.error("sessionStorage write error:", storageErr);
        }

        setToast({
          error: false,
          label: t("PROFILE_VERIFICATION_SUCCESS", "Aadhaar verified successfully!"),
        });

        navigationTimerRef.current = setTimeout(() => {
          onSuccess?.({ verifiedMember, aadhaarLast4, maskedMobile });
          if (isModal) {
            resetForm();
            onClose?.();
          }
        }, 1200);
      }
    } catch (err) {
      console.error("Aadhaar verification error:", err);
      const errorMsg =
        err?.response?.data?.Errors?.[0]?.message ||
        err?.response?.data?.message ||
        err?.message ||
        t("PROFILE_VERIFICATION_FAILED", "Verification failed. Please check the OTP and try again.");
      showError(errorMsg);
      setOtp("");
      setVerifying(false);
    }
  };

  if (isModal && !isOpen) {
    return null;
  }

  const modalHeading =
    title ||
    (mode === "family"
      ? t("ADD_FAMILY_MEMBER_HEADER", "Add Family Member")
      : t("PROFILE_AADHAAR_VERIFICATION", "Aadhaar Verification"));

  const modalSubtext =
    subtext ||
    (mode === "family"
      ? t("ADD_FAMILY_MEMBER_SUBTEXT", "Enter the Aadhaar number of the family member to verify and add.")
      : null);

  const formBody = (
    <div style={{ padding: isModal ? "8px 0" : 0 }}>
      {modalSubtext && <CardText style={{ marginBottom: "16px", color: "#505A5F" }}>{modalSubtext}</CardText>}

      {/* Aadhaar Input */}
      <CardLabel style={{ fontWeight: 600, fontSize: "15px", marginBottom: "8px" }}>
        {t("CORE_COMMON_ENTER_AADHAAR", "Aadhaar Number")}
        <span className="check-page-link-button"> *</span>
      </CardLabel>

      <TextInput
        type="text"
        name="aadhaarNumber"
        value={aadhaarNumber}
        onChange={handleAadhaarChange}
        placeholder="1234 5678 9012"
        inputMode="numeric"
        maxLength={14}
        disable={isOtpSent || loading}
        style={{ marginBottom: "14px" }}
      />

      {/* Consent Callout Banner */}
      {showConsentBox && (
        <div className="aadhaar-verification-consent-box">
          <span className="aadhaar-verification-consent-icon">💡</span>
          <span className="aadhaar-verification-consent-text">
            {t("AADHAAR_OTP_INFO_CALLOUT", "An OTP will be sent to the mobile number registered with this Aadhaar number.")}
          </span>
        </div>
      )}

      {/* Send OTP Action */}
      {!isOtpSent && (
        <SubmitBar
          label={loading ? t("COMMON_SENDING", "Sending OTP...") : t("CS_SEND_OTP", "Send OTP")}
          onSubmit={handleSendOtp}
          disabled={cleanAadhaar.length !== 12 || loading}
        />
      )}

      {/* Inline OTP Section */}
      {isOtpSent && (
        <div style={{ marginTop: "20px" }}>
          <CardLabel style={{ fontWeight: 600, fontSize: "15px", marginBottom: "8px" }}>
            {t("CORE_COMMON_ENTER_OTP", "Enter OTP")}
            <span className="check-page-link-button"> *</span>
          </CardLabel>

          <OTPInput
            length={OTP_LENGTH}
            onChange={(val) => {
              setOtp(val);
              if (error) setError(null);
            }}
            value={otp}
          />

          {/* 30s Countdown Timer / Resend OTP */}
          <div style={{ marginTop: "8px", marginBottom: "16px" }}>
            {timeLeft > 0 ? (
              <p style={{ fontSize: "13px", color: "#505A5F", margin: "4px 0" }}>
                {`${t("CS_RESEND_ANOTHER_OTP", "Resend OTP in")} ${timeLeft} ${t("CS_RESEND_SECONDS", "seconds")}`}
              </p>
            ) : (
              <p
                className="card-text-button cp"
                style={{ fontSize: "13px", color: "#A82227", cursor: "pointer", fontWeight: 600, margin: "4px 0" }}
                onClick={handleResendOtp}
              >
                {t("CS_RESEND_OTP", "Resend OTP")}
              </p>
            )}
          </div>

          {/* OTP Action Row: Verify OTP and Edit Aadhaar side by side */}
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "12px",
              marginTop: "16px",
              width: "100%",
            }}
          >
            <div style={{ flex: 1, minWidth: 0 }}>
              <SubmitBar
                style={{ width: "100%", margin: 0, height: "40px" }}
                label={verifying ? t("COMMON_VERIFYING", "Verifying...") : t("CS_VERIFY_OTP", "Verify OTP")}
                onSubmit={handleVerifyOtp}
                disabled={otp.length !== OTP_LENGTH || verifying}
              />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <button
                type="button"
                onClick={handleEditAadhaar}
                style={{
                  width: "100%",
                  display: "inline-flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: "6px",
                  background: "#FFFFFF",
                  border: "1px solid #A82227",
                  color: "#A82227",
                  borderRadius: "4px",
                  padding: "0 10px",
                  fontSize: "14px",
                  fontWeight: "600",
                  cursor: "pointer",
                  height: "40px",
                  minHeight: "40px",
                  boxSizing: "border-box",
                  whiteSpace: "nowrap",
                  transition: "all 0.2s ease",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = "#FDF2F2";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = "#FFFFFF";
                }}
              >
                <EditIcon style={{ width: 15, height: 15, flexShrink: 0 }} />
                <span>{t("CORE_COMMON_EDIT_AADHAAR_NUMBER", "Edit Aadhaar Number")}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Inline Field Error */}
      {error && <CardLabelError style={{ marginTop: "12px" }}>{error}</CardLabelError>}

      {/* Loading indicator */}
      {(loading || verifying) && <Loader />}
    </div>
  );

  if (isModal) {
    return (
      <React.Fragment>
        <Modal
          headerBarMain={
            <h2 style={{ margin: 0, fontSize: "20px", fontWeight: 600, color: "#0B0C0C" }}>
              {modalHeading}
            </h2>
          }
          headerBarEnd={
            <div
              onClick={handleClose}
              style={{ cursor: "pointer", padding: "4px", display: "flex", alignItems: "center" }}
              title={t("CS_COMMON_CLOSE", "Close")}
            >
              <CloseSvg />
            </div>
          }
          hideSubmit={true}
          popupStyles={{ maxWidth: "520px", width: "92%", borderRadius: "12px", padding: "20px 24px" }}
        >
          {formBody}
        </Modal>

        {/* Only show non-error (e.g. success) toast if needed, never error toast outside modal */}
        {toast && !toast.error && <Toast error={false} label={toast.label} onClose={() => setToast(null)} />}
      </React.Fragment>
    );
  }

  return (
    <React.Fragment>
      <Card>
        <CardHeader>{modalHeading}</CardHeader>
        {formBody}
      </Card>

      {toast && <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />}
    </React.Fragment>
  );
};

export default AadhaarVerificationModal;
