import React from "react";
import { Card, Row, CardHeader, StatusTable, Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";

/**
 * Safely parses various date formats (epoch, ISO string, DD-MM-YYYY, DD/MM/YYYY) into a Date object.
 * @param {string|number|Date} val - Raw date value.
 * @returns {Date|null} Parsed Date object or null if invalid.
 */
const parseDateSafe = (val) => {
  if (!val) return null;
  if (typeof val === "string") {
    const ddmmyyyy = val.match(/^(\d{2})[-/](\d{2})[-/](\d{4})$/);
    if (ddmmyyyy) {
      const parsed = new Date(`${ddmmyyyy[3]}-${ddmmyyyy[2]}-${ddmmyyyy[1]}`);
      if (!isNaN(parsed.getTime())) return parsed;
    }
  }
  const dateObj = new Date(val);
  return isNaN(dateObj.getTime()) ? null : dateObj;
};

/**
 * Formats a date epoch or string into DD/MM/YYYY format.
 * @param {string|number|Date} dateEpoch - Date epoch or date string.
 * @returns {string} Formatted date string in DD/MM/YYYY format, or "NA" if invalid.
 */
const formatEpochToDate = (dateEpoch) => {
  const dateFromApi = parseDateSafe(dateEpoch);
  if (!dateFromApi) return "NA";
  const month = String(dateFromApi.getMonth() + 1).padStart(2, "0");
  const day = String(dateFromApi.getDate()).padStart(2, "0");
  const year = dateFromApi.getFullYear();
  return `${day}/${month}/${year}`;
};

/**
 * Calculates current age in completed years from date of birth.
 * Returns "NA" for invalid, unparseable, or future dates.
 * @param {string|number|Date} dob - Date of birth value.
 * @returns {string} Age as string, or "NA" if invalid or in future.
 */
const calculateAgeFromDob = (dob) => {
  const birthDate = parseDateSafe(dob);
  if (!birthDate) return "NA";
  const ageDiff = Date.now() - birthDate.getTime();
  if (ageDiff < 0) return "NA";
  const ageDate = new Date(ageDiff);
  return String(ageDate.getUTCFullYear() - 1970);
};

/**
 * Masks an Aadhaar number to display only the last 4 digits (e.g., XXXX-XXXX-1234).
 * @param {string|number} aadhaar - Raw Aadhaar number.
 * @returns {string} Masked Aadhaar string or "NA" if missing.
 */
const maskAadhaar = (aadhaar) => {
  if (!aadhaar) return "NA";
  const str = String(aadhaar);
  return str.length >= 4 ? `XXXX-XXXX-${str.slice(-4)}` : str;
};

/**
 * Masks a mobile number to display only the last 4 digits (e.g., XXXXXX1234).
 * @param {string|number} mobile - Raw mobile number.
 * @returns {string} Masked mobile number string or "NA" if missing.
 */
const maskMobile = (mobile) => {
  if (!mobile) return "NA";
  const str = String(mobile);
  return str.length >= 4 ? `XXXXXX${str.slice(-4)}` : str;
};

/**
 * Public QR Code verification component for Family Certificate.
 * Reads query parameters `tenantId` and `familyId` from the URL, fetches family details,
 * and renders a summary table along with active member details.
 * Expected Query Params:
 *   - tenantId: ULB / state tenant identifier (e.g., "pg" or "pg.citya")
 *   - familyId: Unique family identifier (e.g., "FAM-2026-000064")
 * @returns {JSX.Element} Rendered family verification view.
 */
const FamilyQRCode = () => {
  const { t } = useTranslation();
  const params = new URLSearchParams(window.location.search);
  const tenantId = params.get("tenantId");
  const familyId = params.get("familyId");

  const { data: familyData, isLoading: familyLoading } = Digit.Hooks.useFamilyQrCode({
    tenantId,
    familyId,
  });

  const family = familyData?.Families?.[0];

  const headMember = family?.members?.find(
    (m) => m.userUuid === family?.headUserUuid || m.relationshipWithHead === "HEAD"
  );
  const resolvedAddr = headMember?.citizenDetails?.addresses?.[0] || {};

  const district = resolvedAddr?.district || family?.additionalDetails?.district || "NA";
  const city = resolvedAddr?.city || family?.additionalDetails?.city || "NA";
  const ward =
    resolvedAddr?.locality ||
    resolvedAddr?.ward ||
    family?.additionalDetails?.ward ||
    family?.additionalDetails?.locality ||
    "NA";
  const address =
    resolvedAddr?.address ||
    resolvedAddr?.addressLine1 ||
    family?.additionalDetails?.address ||
    (family?.familyAddressPropertyId ? `Property ID: ${family.familyAddressPropertyId}` : "NA");

  const members = (family?.members || []).filter((m) => m.active !== false && m.status !== "EXITED");
  let totalIncome = 0;
  let hasIncome = false;
  members.forEach((m) => {
    const inc = m.income?.annualIncome ?? m.memberIncome ?? m.additionalDetails?.memberIncome;
    if (inc != null && !isNaN(Number(inc))) {
      totalIncome += Number(inc);
      hasIncome = true;
    }
  });
  const familyIncome = hasIncome ? String(totalIncome) : "NA";

  return (
    <React.Fragment>
      <div className="core-assets-qrcode-fullwidth">
        <Card>
          <CardHeader>{t("CORE_FAMILY_IDENTIFICATION_SUMMARY", "Family Identification Summary")}</CardHeader>
          {familyLoading ? (
            <Loader />
          ) : family ? (
            <React.Fragment>
              <StatusTable>
                <Row label={t("CORE_COMMON_FAMILY_ID")} text={family.familyId || "NA"} />
                <Row label={t("CORE_COMMON_STATUS")} text={family.status || "NA"} />
                <Row label={t("ISSUE_DATE")} text={formatEpochToDate(family.auditDetails?.createdTime)} />
                <Row label={t("HEAD_OF_FAMILY")} text={family.headName || "NA"} />
                <Row label={t("CORE_COMMON_DISTRICT")} text={district} />
                <Row label={t("CORE_COMMON_BLOCK_CITY")} text={city} />
                <Row label={t("CORE_COMMON_VILLAGE_WARD")} text={ward} />
                <Row label={t("CORE_COMMON_ADDRESS")} text={address} />
                <Row label={t("FAMILY_INCOME")} text={familyIncome} />
              </StatusTable>

              <div className="core-family-qrcode-members-section">
                <h3 className="core-family-qrcode-members-title">
                  {t("CORE_COMMON_FAMILY_MEMBER")}
                </h3>
                <table className="core-family-qrcode-table">
                  <thead>
                    <tr>
                      {[
                        "CORE_COMMON_NAME",
                        "CORE_COMMON_FATHER_NAME",
                        "CORE_COMMON_MOTHER_NAME",
                        "CORE_COMMON_DOB",
                        "RELATION_WITH_HOF",
                        "CORE_COMMON_AGE",
                        "CORE_COMMON_MOBILE_NUMBER",
                        "CORE_COMMON_AADHAAR_NUMBER",
                        "CORE_COMMON_MEMBER_INCOME",
                        "CORE_COMMON_OCCUPATION",
                        "CORE_COMMON_CASTE",
                        "CORE_COMMON_IS_DIVYANG",
                      ].map((header) => (
                        <th key={header}>
                          {t(header)}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {members.map((member, index) => {
                      const citizen = member.citizenDetails;
                      const name = member.name || citizen?.name || "NA";
                      const fatherName = citizen?.fatherName || member.fatherName || "NA";
                      const motherName = citizen?.motherName || member.motherName || "NA";
                      const dob = formatEpochToDate(citizen?.dob || member.dob);
                      const age = calculateAgeFromDob(citizen?.dob || member.dob);
                      const mobile = maskMobile(member.mobileNumber || citizen?.mobileNumber);
                      const aadhaar = maskAadhaar(citizen?.aadhaarNumber || member.aadhaarNumber);
                      const income = member.income?.annualIncome ?? member.memberIncome ?? "NA";
                      const occupation = member.income?.occupation ?? member.occupation ?? "NA";
                      const caste = citizen?.caste || member.caste || "NA";
                      const isDivyang =
                        citizen?.isSpeciallyAbled !== undefined
                          ? citizen.isSpeciallyAbled
                            ? "YES"
                            : "NO"
                          : member.isDivyang || "NO";

                      return (
                        <tr key={member.id || index}>
                          <td>{name}</td>
                          <td>{fatherName}</td>
                          <td>{motherName}</td>
                          <td className="core-family-qrcode-nowrap">{dob}</td>
                          <td>{member.relationshipWithHead || "NA"}</td>
                          <td>{age}</td>
                          <td>{mobile}</td>
                          <td className="core-family-qrcode-nowrap">{aadhaar}</td>
                          <td>{income}</td>
                          <td>{occupation}</td>
                          <td>{caste}</td>
                          <td>{isDivyang}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </React.Fragment>
          ) : (
            <p>{t("NO_FAMILY_DETAILS_FOUND")}</p>
          )}
        </Card>
      </div>
    </React.Fragment>
  );
};

export default FamilyQRCode;

