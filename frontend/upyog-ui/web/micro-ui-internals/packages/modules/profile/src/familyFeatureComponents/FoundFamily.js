/**
 * 
 * @description
 * Standardized review and submission screen for joining an existing family.
 * 
 * Features:
 * - Displays verified Head of Family (HOF) information in a structured `StatusTable`.
 * - Categorizes and presents Applicant details into 4 visually distinct sections:
 *   1. Personal Details (including selected Relationship with HOF)
 *   2. Minimal Property Details (Locality Type, Property ID / Address, Property Type)
 *   3. Minimal Income Details (Occupation, Annual Income, Tax ID Type & Number)
 *   4. Minimal Vehicle Details (Vehicle Type, Vehicle Number, Purchase Year)
 * - Uses official UPYOG `StatusTable` & `Row` components for clear label-value alignment.
 * - Submits `Digit.UserService.joinFamily` request with the working backend payload.
 * - Navigates to `FAMILY_JOINED` on successful submission.
 * 
 * @returns {React.ReactElement} Formatted Found Family review & Join Family action UI.
 */

import React, { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import {
  Card,
  CardHeader,
  StatusTable,
  Row,
  SubmitBar,
  Loader,
  Toast,
} from "@nudmcdgnpm/digit-ui-react-components";
import { getFamilyHeadFields, getPersonalFields, useProfileUser, withValues } from "../utils";
import { PROFILE_ROUTES } from "../utils/constants";
import { profileStyles } from "../utils/profileStyles";


const FoundFamily = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const locationState = useLocation().state || {};

  // Retrieve cached join session state from sessionStorage as fallback
  const storedJoinData = useMemo(() => {
    try {
      return JSON.parse(sessionStorage.getItem("Digit.family.joinData") || "null");
    } catch {
      return null;
    }
  }, []);

  const verifiedData = locationState.verifiedData || storedJoinData?.verifiedData;
  const familyId = locationState.familyId || storedJoinData?.familyId || verifiedData?.familyId;

  const { user, loading } = useProfileUser();
  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState(null);

  // Retrieve applicant's filled multi-step form data from sessionStorage
  const userUuid = Digit.UserService.getUser()?.info?.uuid;
  const [allParams] = Digit.Hooks.useSessionStorage("Family_Details", {});
  const applicantParams = allParams?.[userUuid] || {};

  const profileDetails = applicantParams.profileDetails || {};
  const propertyDetails = applicantParams.property || {};
  const incomeDetails = applicantParams.income || {};
  const vehicleDetails = applicantParams.vehicles || {};

  const showError = (label) => setToast({ error: true, label });

  // Direct relationship code extraction from CreateProfile step
  const relationship = profileDetails.relationshipWithHof;
  const relationshipCode = relationship?.code || relationship;

  // Head of family display summary fields
  const familyHeadFields = useMemo(() => getFamilyHeadFields(verifiedData, t), [verifiedData, t]);

  // Applicant personal fields including selected relationship with HOF
  const applicantFields = useMemo(() => {
    const baseFields = user ? getPersonalFields(user) : [];
    if (relationship) {
      baseFields.push([
        "PROFILE_RELATIONSHIP_WITH_HEAD",
        t(relationship?.code || relationship?.name || relationship),
      ]);
    }
    return baseFields;
  }, [user, relationship, t]);

  // Minimal Property details summary
  const propertyFields = useMemo(() => {
    if (!propertyDetails || Object.keys(propertyDetails).length === 0) return [];
    const isUrban = propertyDetails.localityType === "URBAN";

    const pairs = [
      ["PROFILE_LOCALITY_TYPE", propertyDetails.localityType ? t(`${propertyDetails.localityType}`) : null],
      isUrban
        ? ["PROFILE_PROPERTY_ID", propertyDetails.primaryPropertyId || propertyDetails.propertyId]
        : ["PROFILE_GRAM_PANCHAYAT", propertyDetails.gramPanchayat?.name || propertyDetails.gramPanchayat],
      isUrban
        ? ["PROFILE_PROPERTY_LOCATION", propertyDetails.propertyAddress]
        : ["PROFILE_VILLAGE", propertyDetails.village?.name || propertyDetails.village],
      !isUrban
        ? ["PROFILE_PROPERTY_TYPE", propertyDetails.propertyType?.code || propertyDetails.propertyType?.name || propertyDetails.propertyType]
        : null,
      !isUrban
        ? ["COMMON_ADDRESS", propertyDetails.propertyAddress]
        : null,
    ].filter(Boolean);

    return withValues(pairs);
  }, [propertyDetails, t]);

  // Minimal Income details summary
  const incomeFields = useMemo(() => {
    if (!incomeDetails || Object.keys(incomeDetails).length === 0) return [];
    const pairs = [
      [
        "PROFILE_OCCUPATION",
        incomeDetails.occupation
          ? t(incomeDetails.occupation?.i18nKey || incomeDetails.occupation?.code || incomeDetails.occupation?.name || incomeDetails.occupation)
          : null,
      ],
      ["PROFILE_ANNUAL_INCOME", incomeDetails.annualIncome ? `₹ ${incomeDetails.annualIncome}` : null],
      ["PROFILE_TAX_ID_TYPE", incomeDetails.taxIdType?.code || incomeDetails.taxIdType?.name || incomeDetails.taxIdType],
      ["PROFILE_TAX_ID_NUMBER", incomeDetails.taxIdNumber],
    ].filter(Boolean);

    return withValues(pairs);
  }, [incomeDetails, t]);

  // Minimal Vehicle details summary
  const vehicleFields = useMemo(() => {
    if (!vehicleDetails || Object.keys(vehicleDetails).length === 0) return [];
    const pairs = [
      [
        "PROFILE_VEHICLE_TYPE",
        vehicleDetails.vehicleType
          ? t(vehicleDetails.vehicleType?.i18nKey || vehicleDetails.vehicleType?.code || vehicleDetails.vehicleType?.name || vehicleDetails.vehicleType)
          : null,
      ],
      ["PROFILE_VEHICLE_NO", vehicleDetails.vehicleNo],
      ["PROFILE_VEHICLE_PURCHASE_YEAR", vehicleDetails.purchaseYear],
    ].filter(Boolean);

    return withValues(pairs);
  }, [vehicleDetails, t]);

  if (!verifiedData) {
    return (
      <Card>
        <CardHeader>{t("PROFILE_FOUND_FAMILY")}</CardHeader>
        <div style={profileStyles.message}>{t("PROFILE_FAMILY_NOT_FOUND")}</div>
      </Card>
    );
  }

  /**
   * Dispatches the Join Family API call.
   */
  const handleJoin = async () => {
    setSubmitting(true);
    try {
      const res = await Digit.UserService.joinFamily({
        familyId: verifiedData?.familyId || familyId,
        tenantId,
        applicantUserUuid: userUuid,
        relationshipWithHead: relationshipCode,
        txnId: verifiedData?.txnId || verifiedData?.consentToken,
      });

      if (res?.JoinRequest?.status !== "JOINED") throw new Error("JOIN_FAILED");

      // Clean up join session state on successful submission
      sessionStorage.removeItem("Digit.family.joinData");

      navigate(PROFILE_ROUTES.FAMILY_JOINED, {
        replace: true,
        state: { joinData: res.JoinRequest },
      });
    } catch (err) {
      console.log("error on join family page", err);
      showError(err?.response?.data?.Errors?.[0]?.message || t("PROFILE_JOIN_FAMILY_FAILED"));
      setSubmitting(false);
    }
  };

  return (
    <React.Fragment>
      {/* 1. Found Family / Head of Family Card */}
      <Card>
        <CardHeader style={{ fontSize: "24px", marginBottom: "16px" }}>
          {t("PROFILE_FOUND_FAMILY")}
        </CardHeader>
        <div style={profileStyles.foundFamilyDetailSection.sectionBoxStyle}>
          <StatusTable>
            {familyHeadFields.map(([key, val], idx) => (
              <Row key={idx} label={t(key)} text={val || "NA"} />
            ))}
          </StatusTable>
        </div>
      </Card>

      {/* 2. Applicant Review Details Card */}
      <Card>
        <CardHeader style={{ fontSize: "24px", marginBottom: "20px" }}>
          {t("PROFILE_APPLICANT_DETAILS")}
        </CardHeader>

        {/* Section A: Personal Details */}
        <div style={profileStyles.foundFamilyDetailSection.sectionBoxStyle}>
          <div style={profileStyles.foundFamilyDetailSection.sectionTitleStyle}>
            <span>{t("CORE_COMMON_PROFILE_PERSONAL_DETAILS")}</span>
          </div>
          {loading ? (
            <Loader />
          ) : (
            <StatusTable>
              {applicantFields.map(([key, val], idx) => (
                <Row key={idx} label={t(key)} text={val || "NA"} />
              ))}
            </StatusTable>
          )}
        </div>

        {/* Section B: Property Details */}
        {propertyFields.length > 0 && (
          <div style={profileStyles.foundFamilyDetailSection.sectionBoxStyle}>
            <div style={profileStyles.foundFamilyDetailSection.sectionTitleStyle}>
              <span>{t("ES_TITILE_PROPERTY_DETAILS")}</span>
            </div>
            <StatusTable>
              {propertyFields.map(([key, val], idx) => (
                <Row key={idx} label={t(key)} text={val || "NA"} />
              ))}
            </StatusTable>
          </div>
        )}

        {/* Section C: Income Details */}
        {incomeFields.length > 0 && (
          <div style={profileStyles.foundFamilyDetailSection.sectionBoxStyle}>
            <div style={profileStyles.foundFamilyDetailSection.sectionTitleStyle}>
              <span>{t("ES_TITLE_INCOME_DETAILS")}</span>
            </div>
            <StatusTable>
              {incomeFields.map(([key, val], idx) => (
                <Row key={idx} label={t(key)} text={val || "NA"} />
              ))}
            </StatusTable>
          </div>
        )}

        {/* Section D: Vehicle Details */}
        {vehicleFields.length > 0 && (
          <div style={profileStyles.foundFamilyDetailSection.sectionBoxStyle}>
            <div style={profileStyles.foundFamilyDetailSection.sectionTitleStyle}>
              <span>{t("ES_TITLE_VEHICLE_DETAILS")}</span>
            </div>
            <StatusTable>
              {vehicleFields.map(([key, val], idx) => (
                <Row key={idx} label={t(key)} text={val || "NA"} />
              ))}
            </StatusTable>
          </div>
        )}

        <SubmitBar
          label={t("PROFILE_JOIN_FAMILY")}
          onSubmit={handleJoin}
          disabled={submitting}
        />
      </Card>

      {toast && <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />}
    </React.Fragment>
  );
};

export default FoundFamily;