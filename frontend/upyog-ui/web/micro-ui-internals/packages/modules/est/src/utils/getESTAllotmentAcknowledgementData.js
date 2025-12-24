
import {
  pick,
  filterEmpty,
  formatDate,
  formatDurationWithMonths,
} from "./index";

const getESTAllotmentAcknowledgementData = async (
  application = {},
  tenantInfo = {},
  t = (k) => k
) => {
  /* ---------------- Safe destructuring ---------------- */

  const {
    Assets = [],
    Allotments = [],
    assetData,
    Assetdata,
    AssignAssetsData,
  } = application;

  /* ---------------- Asset ---------------- */

  const asset =
    Assets[0] ||
    assetData?.Assetdata ||
    Assetdata ||
    {};

  /* ---------------- Allotment ---------------- */

  const allotment =
    Allotments[0] ||
    AssignAssetsData?.AllotmentData ||
    {};

  /* ---------------- Section builders ---------------- */

  const assetDetails = filterEmpty([
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
  ]);

  const allotteeDetails = filterEmpty([
    { title: t("EST_ALLOTTEE_NAME"), value: allotment.alloteeName },
    { title: t("EST_PHONE_NUMBER"), value: allotment.mobileNo },
    {
      title: t("EST_ALTERNATE_PHONE_NUMBER"),
      value: allotment.alternateMobileNo,
    },
    { title: t("EST_EMAIL_ID"), value: allotment.emailId },
  ]);

  const invoiceDetails = filterEmpty([
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
  ]);

  /* ---------------- Final Details ---------------- */

  const details = [
    {
      title: t("EST_ASSET_DETAILS"),
      asSectionHeader: true,
      values: assetDetails,
    },
    {
      title: t("EST_PERSONAL_DETAILS_OF_ALLOTTEE"),
      asSectionHeader: true,
      values: allotteeDetails,
    },
    {
      title: t("EST_ALLOTMENT_INVOICE_DETAILS"),
      asSectionHeader: true,
      values: invoiceDetails,
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

    // Raw data (useful for debugging / future use)
    Assets: [asset],
    Allotments: [allotment],
    asset,
    allotment,
    fullApplication: application,
  };
};

export default getESTAllotmentAcknowledgementData;
