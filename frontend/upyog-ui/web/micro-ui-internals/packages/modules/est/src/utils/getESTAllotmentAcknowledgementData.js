// src/utils/getESTAllotmentAcknowledgementData.js

/* -------------------- Utility helpers -------------------- */

const pick = (...vals) =>
  vals.find((v) => v !== undefined && v !== null && v !== "") || "";

const filterEmpty = (arr = []) =>
  arr.filter(
    (i) =>
      i &&
      i.value !== undefined &&
      i.value !== null &&
      i.value !== ""
  );

/* -------------------- Date formatter -------------------- */
/**
 * Returns: DD/MM/YYYY
 */
const formatDate = (d) => {
  if (!d) return "";
  const date = d instanceof Date ? d : new Date(d);
  if (isNaN(date.getTime())) return "";
  return date.toLocaleDateString("en-GB");
};

/* -------------------- Duration calculator -------------------- */
/**
 * Returns:
 * 1 year 6 months
 */
const calculatemonths = (start, end) => {
  if (!start || !end) return "";

  const startDate = new Date(start);
  const endDate = new Date(end);

  if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) return "";

  let years = endDate.getFullYear() - startDate.getFullYear();
  let months = endDate.getMonth() - startDate.getMonth();

  if (months < 0) {
    years--;
    months += 12;
  }

  let result = "";

  if (years > 0) {
    result += `${years} year${years > 1 ? "s" : ""}`;
  }

  if (months > 0) {
    result += `${result ? " " : ""}${months} month${months > 1 ? "s" : ""}`;
  }

  return result || "0 months";
};

/* -------------------- Duration formatter -------------------- */
/**
 * Final Output:
 * 18 months (1 year 6 months)
 */
const formatDurationWithMonths = (allotment = {}) => {
  const totalMonths = Number(allotment.duration || 0);
  if (!totalMonths) return "";

  const readable = calculatemonths(
    allotment.agreementStartDate,
    allotment.agreementEndDate
  );

  return readable
    ? `${totalMonths} months (${readable})`
    : `${totalMonths} months`;
};

/* -------------------- Main function -------------------- */

const getESTAllotmentAcknowledgementData = async (
  application = {},
  tenantInfo = {},
  t = (k) => k
) => {
  /* ---------------- Asset ---------------- */

  const asset =
    application?.Assets?.[0] ||
    application?.assetData?.Assetdata ||
    application?.Assetdata ||
    {};

  /* ---------------- Allotment ---------------- */

  const allotment =
    application?.Allotments?.[0] ||
    application?.AssignAssetsData?.AllotmentData ||
    {};

  /* ---------------- Details ---------------- */

  const details = [
    {
      title: t("EST_ASSET_DETAILS"),
      asSectionHeader: true,
      values: filterEmpty([
        {
          title: t("EST_ASSET_NUMBER"),
          value: pick(allotment.assetNo, asset.estateNo),
        },
        { title: t("EST_BUILDING_NAME"), value: asset.buildingName },
        { title: t("EST_BUILDING_NUMBER"), value: asset.buildingNo },
        { title: t("EST_LOCALITY"), value: asset.locality },
        { title: t("EST_TOTAL_AREA"), value: asset.totalFloorArea },
        { title: t("EST_FLOOR"), value: asset.floor },
        { title: t("EST_RATE"), value: pick(allotment.rate, asset.rate) },
        { title: t("EST_ASSET_TYPE"), value: asset.assetType },
      ]),
    },

    {
      title: t("EST_PERSONAL_DETAILS_OF_ALLOTTEE"),
      asSectionHeader: true,
      values: filterEmpty([
        { title: t("EST_ALLOTTEE_NAME"), value: allotment.alloteeName },
        { title: t("EST_PHONE_NUMBER"), value: allotment.mobileNo },
        {
          title: t("EST_ALTERNATE_PHONE_NUMBER"),
          value: allotment.alternateMobileNo,
        },
        { title: t("EST_EMAIL_ID"), value: allotment.emailId },
      ]),
    },

    {
      title: t("EST_ALLOTMENT_INVOICE_DETAILS"),
      asSectionHeader: true,
      values: filterEmpty([
        {
          title: t("EST_AGREEMENT_START_DATE"),
          value: formatDate(allotment.agreementStartDate),
        },
        {
          title: t("EST_AGREEMENT_END_DATE"),
          value: formatDate(allotment.agreementEndDate),
        },
        {
          title: t("EST_DURATION_IN_YEARS"),
          value: formatDurationWithMonths(allotment),
        },
        {
          title: t("EST_MONTHLY_RENT_IN_INR"),
          value: allotment.monthlyRent,
        },
        {
          title: t("EST_ADVANCE_PAYMENT_IN_INR"),
          value: allotment.advancePayment,
        },
        {
          title: t("EST_ADVANCE_PAYMENT_DATE"),
          value: formatDate(allotment.advancePaymentDate),
        },
        {
          title: t("EST_EOFFICE_FILE_NO"),
          value: allotment.eofficeFileNo,
        },
      ]),
    },
  ];

  /* ---------------- Final output ---------------- */

  return {
    heading: t("EST_ACKNOWLEDGEMENT"),
    applicationNumber: pick(allotment.assetNo, asset.estateNo),
    tenantId: tenantInfo?.code,
    name: tenantInfo?.name,
    email: tenantInfo?.emailId,
    phoneNumber: tenantInfo?.contactNumber,
    details,

    Assets: [asset],
    Allotments: [allotment],
    asset,
    allotment,
    fullApplication: application,
  };
};

export default getESTAllotmentAcknowledgementData;
