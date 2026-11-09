import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Card, CardHeader, CardText, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import AadhaarVerification from "../components/AadhaarVerification";

/**
 * AddFamilyMember Component
 *
 * Screen for initiating the addition of a new family member to a family group.
 *
 * Workflow:
 * 1. Citizen enters the candidate member's 12-digit Aadhaar number and accepts Aadhaar consent.
 * 2. An OTP is triggered to the Aadhaar-registered mobile number.
 * 3. Citizen submits the 6-digit OTP (with resend timer and edit capabilities).
 * 4. Once verified, member details are cached in sessionStorage for refresh resilience.
 * 5. Citizen is routed to `FamilyMemberVerified` to review demographic details and select relationship.
 */
const AddFamilyMember = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  // State to manage toast notifications (success / error messages)
  const [toast, setToast] = useState(null);

  /**
   * Helper function to display temporary toast messages.
   * @param {string} type - "success" | "error"
   * @param {string} message - Notification message to display
   * @param {number} duration - Auto-dismiss timeout in milliseconds (default: 5000ms)
   */
  const showToast = (type, message, duration = 5000) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), duration);
  };

  /**
   * Callback invoked by AadhaarVerification upon successful OTP authentication.
   *
   * @param {Object} verificationData
   * @param {Object} verificationData.verifiedMember - Sanitized demographic details returned from verification API
   * @param {string} verificationData.aadhaarLast4 - Last 4 digits of Aadhaar (full number is not persisted for PII compliance)
   * @param {string} verificationData.maskedMobile - Masked mobile number (e.g. +91 ******7935)
   */
  const handleVerified = ({ verifiedMember, aadhaarLast4, maskedMobile }) => {
    // Persist verified data to sessionStorage so state is preserved if user refreshes the review page
    try {
      sessionStorage.setItem("Digit.family.verifiedMember", JSON.stringify(verifiedMember));
      sessionStorage.setItem("Digit.family.aadhaarLast4", aadhaarLast4);
      sessionStorage.setItem("Digit.family.maskedMobile", maskedMobile);
    } catch (storageErr) {
      console.error("sessionStorage write error:", storageErr);
    }

    // Navigate to the verification review and relationship selection screen
    navigate("/upyog-ui/citizen/profile/family-member-verified", {
      state: {
        verifiedMember,
        aadhaarLast4,
        maskedMobile,
      },
    });
  };

  return (
    <React.Fragment>
      <Card>
        {/* Page Title & Subtext */}
        <CardHeader>{t("ADD_FAMILY_MEMBER_HEADER")}</CardHeader>
        <CardText>{t("ADD_FAMILY_MEMBER_SUBTEXT")}</CardText>

        {/* Reusable Aadhaar Input & OTP Verification Widget */}
        <AadhaarVerification
          onVerified={handleVerified}
          onError={(errorMsg) => showToast("error", errorMsg)}
        />
      </Card>

      {/* Floating Toast Notification */}
      {toast && (
        <div className="profile-toast-container">
          <Toast
            error={toast.type === "error"}
            label={toast.message}
            onClose={() => setToast(null)}
          />
        </div>
      )}
    </React.Fragment>
  );
};

export default AddFamilyMember;
