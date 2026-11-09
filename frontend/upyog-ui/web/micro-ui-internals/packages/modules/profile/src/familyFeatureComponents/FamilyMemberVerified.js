import React, { useState, useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useLocation, useNavigate } from "react-router-dom";
import {
  Card,
  CardHeader,
  CardLabel,
  CardLabelError,
  SubmitBar,
  StatusTable,
  Row,
  Toast,
  Dropdown,
} from "@nudmcdgnpm/digit-ui-react-components";
import { addFamilyMember, getFamilyMembers } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * FamilyMemberVerified Component
 *
 * Review and confirmation screen displayed after a new family member has been
 * successfully authenticated via Aadhaar OTP in `AddFamilyMember`.
 *
 * Workflow:
 * 1. Reads demographic details and Aadhaar information from React Router state or sessionStorage.
 * 2. Displays a verification confirmation banner and a StatusTable showing verified demographics.
 * 3. Prompts the citizen to select the member's relationship to the Head of Family (MDMS-backed).
 * 4. On submission, stages the member into local family state via `addFamilyMember()` and
 *    redirects back to the table view in `FamilyMemberAdded` with `{ replace: true }`.
 */
const FamilyMemberVerified = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const stateId = Digit.ULBService.getStateId() || Digit.ULBService.getCurrentTenantId() || "pg";

  // MDMS Hook: Fetch active relationship master data from common-masters.FamilyRelationship
  const { data: FamilyRelationShips } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "common-masters",
    [{ name: "FamilyRelationship" }],
    {
      select: (data) => {
        const formattedData = data?.["common-masters"]?.["FamilyRelationship"];
        return formattedData?.filter((option) => option.active !== false) || [];
      },
    }
  );

  // Memoize formatted relationship dropdown options
  const familyRelationShips = useMemo(() => {
    if (Array.isArray(FamilyRelationShips) && FamilyRelationShips.length > 0) {
      return FamilyRelationShips.map((relation) => ({
        code: relation.code,
        name: relation.name || relation.code,
        i18nKey: `${relation.code}`,
      }));
    }
    return [];
  }, [FamilyRelationShips]);

  // Retrieve member details passed from the OTP verification step or fallback to sessionStorage
  const [verifiedMember] = useState(() => {
    if (location.state?.verifiedMember) return location.state.verifiedMember;
    try {
      const stored = sessionStorage.getItem("Digit.family.verifiedMember");
      return stored ? JSON.parse(stored) : null;
    } catch (e) {
      return null;
    }
  });

  // Retrieve last 4 digits of Aadhaar (or fallback to sessionStorage / verifiedMember)
  const [aadhaarLast4] = useState(() => {
    return (
      location.state?.aadhaarLast4 ||
      verifiedMember?.aadhaarLast4 ||
      sessionStorage.getItem("Digit.family.aadhaarLast4") ||
      ""
    );
  });

  /**
   * Route Guard:
   * Protect against direct URL hits or empty state reloads.
   * If verified member userUuid is missing, redirect to Add Family Member.
   */
  useEffect(() => {
    if (!verifiedMember?.userUuid) {
      navigate(PROFILE_ROUTES.ADD_FAMILY_MEMBER, { replace: true });
    }
  }, [verifiedMember?.userUuid, navigate]);

  // Relationship dropdown and validation error state
  const [selectedRelationship, setSelectedRelationship] = useState(null);
  const [relationshipError, setRelationshipError] = useState(null);

  // Toast notification state
  const [toast, setToast] = useState(null);

  // Toast notification helper
  const showToast = (type, message, duration = 5000) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), duration);
  };

  /**
   * Action Handler: Add to Family
   * 1. Validates that a relationship option has been selected.
   * 2. Constructs member object containing userUuid, demographic fields, and relationship.
   * 3. Prevents duplicate addition with toast feedback.
   * 4. Appends member to temporary session storage list via `addFamilyMember()`.
   * 5. Cleans up single-member verification cache from sessionStorage.
   * 6. Redirects to `FamilyMemberAdded` screen replacing history stack.
   */
  const handleAddFamilyMember = () => {
    // 1. Validate relationship selection
    if (!selectedRelationship) {
      setRelationshipError(t("ERR_SELECT_RELATIONSHIP"));
      return;
    }

    // 2. Build the staged member object
    const member = {
      userUuid: verifiedMember?.userUuid,
      name: verifiedMember?.demographics?.name,
      relationshipWithHead: (selectedRelationship?.code || "").toUpperCase(),
      isHead: false,
      aadhaarLast4: aadhaarLast4 || verifiedMember?.aadhaarLast4 || undefined,
      gender: verifiedMember?.gender || verifiedMember?.demographics?.gender,
    };

    // 3. Guard against missing userUuid
    if (!member.userUuid) {
      showToast("error", t("MISSING_USER_UUID"));
      return;
    }

    // 4. Guard against duplicate member addition
    const currentMembers = getFamilyMembers();
    if (currentMembers.some((m) => m.userUuid === member.userUuid)) {
      showToast("error", t("ERR_FAMILY_MEMBER_ALREADY_ADDED", "This member is already added to the family."));
      return;
    }

    // 5. Append to temporary frontend staging list in sessionStorage
    addFamilyMember(member);

    // 6. Clean up single-member verification cache from sessionStorage so state does not linger
    try {
      sessionStorage.removeItem("Digit.family.verifiedMember");
      sessionStorage.removeItem("Digit.family.aadhaarLast4");
      sessionStorage.removeItem("Digit.family.cleanAadhaar");
      sessionStorage.removeItem("Digit.family.maskedMobile");
    } catch (clearErr) {
      console.error("Error clearing verification session keys:", clearErr);
    }

    // 7. Navigate to family members table view (replace: true to keep history stack clean)
    navigate(PROFILE_ROUTES.PROFILE_CREATE_FAMILY, { replace: true });
  };

  // Prevent transient rendering of NA fields while redirecting
  if (!verifiedMember?.userUuid) return null;

  return (
    <React.Fragment>
      <Card>
        {/* Screen Header */}
        <CardHeader>{t("FAMILY_MEMBER_VERIFIED")}</CardHeader>

        {/* Verification Success Banner */}
        <div style={profileStyles.verifiedBanner}>
          <span>✓</span>
          <span>{verifiedMember?.message ? t(verifiedMember.message, verifiedMember.message) : t("AADHAAR_VERIFIED_SUCCESSFULLY", "Aadhaar verified successfully")}</span>
        </div>

        {/* Demographics Status Table */}
        <StatusTable>
          <Row
            label={t("CORE_COMMON_NAME")}
            text={verifiedMember?.name || verifiedMember?.demographics?.name || "NA"}
          />
          <Row
            label={t("CORE_COMMON_PROFILE_DOB")}
            text={verifiedMember?.dob || verifiedMember?.demographics?.dob || "NA"}
          />
          <Row
            label={t("CORE_COMMON_PROFILE_GENDER")}
            text={t(verifiedMember?.gender || verifiedMember?.demographics?.gender) || "NA"}
          />
          <Row
            label={t("CORE_COMMON_AADHAR_NUMBER")}
            text={verifiedMember?.maskedAadhaar || (aadhaarLast4 ? `XXXX-XXXX-${aadhaarLast4}` : "NA")}
          />
        </StatusTable>

        {/* Relationship Selection from MDMS */}
        <div style={{ marginTop: "24px", marginBottom: "20px" }}>
          <CardLabel>
            {t("CORE_COMMON_RELATIONSHIP")}
            <span className="check-page-link-button"> *</span>
          </CardLabel>
          <Dropdown
            t={t}
            option={familyRelationShips}
            selected={selectedRelationship}
            select={(val) => {
              setSelectedRelationship(val);
              if (relationshipError) setRelationshipError(null);
            }}
            optionKey="name"
            placeholder={t("SELECT_RELATIONSHIP")}
            optionCardStyles={{
              maxHeight: "220px",
              overflowY: "auto",
            }}
          />
          {relationshipError && <CardLabelError>{relationshipError}</CardLabelError>}
        </div>

        {/* Confirm and Add Button */}
        <SubmitBar
          label={t("ADD_TO_FAMILY")}
          onSubmit={handleAddFamilyMember}
        />
      </Card>

      {/* Toast Feedback */}
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

export default FamilyMemberVerified;
