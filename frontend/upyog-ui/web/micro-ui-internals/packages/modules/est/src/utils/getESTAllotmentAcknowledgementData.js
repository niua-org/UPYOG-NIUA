const capitalize = (text = "") =>
  text.substr(0, 1).toUpperCase() + text.substr(1);

const ulbCamel = (ulb = "") =>
  ulb.toLowerCase().split(" ").map(capitalize).join(" ");

const getESTAllotmentAcknowledgementData = async (
  application = {},
  tenantInfo = {},
  t
) => {
  // ---- NORMALISE INPUT ----
  const allotment =
    application?.Allotments?.[0] || // case-1: apiData.Allotments[0]
    application?.AssignAssetsData || // case-2: { AssignAssetsData: {...} }
    application; // case-3: direct object

  // ---- DOCUMENTS (if any) ----
  const filesArray = allotment?.documents?.map((v) => v?.fileStoreId) || [];
  let res = [];
  try {
    if (filesArray.length > 0) {
      res = await Digit.UploadServices.Filefetch(
        filesArray,
        Digit.ULBService.getStateId()
      );
    }
  } catch (e) {
    console.log("File fetch ignored in ack pdf", e);
  }

  // helper: keep only non-empty values
  const filterEmptyValues = (values) =>
    values.filter(
      (item) =>
        item?.value !== undefined &&
        item?.value !== null &&
        item?.value !== ""
    );

  // ---- TENANT DISPLAY NAME ----
  let ulbGradeKey = "";
  try {
    ulbGradeKey =
      tenantInfo?.city?.ulbGrade &&
      `ULBGRADE_${tenantInfo.city.ulbGrade
        .toUpperCase()
        .replace(" ", "_")
        .replace(".", "_")}`;
  } catch (e) {
    ulbGradeKey = "";
  }

  const ulbName =
    (tenantInfo?.i18nKey ? t(tenantInfo.i18nKey) : "") +
    (ulbGradeKey ? " " + ulbCamel(t(ulbGradeKey)) : "");

  return {
    t: t,
    tenantId: tenantInfo?.code || "NA",
    name: ulbName || "NA",
    email: tenantInfo?.emailId || "NA",
    phoneNumber: tenantInfo?.contactNumber || "NA",

    // payload me applicationNo nahi hai isliye assetNo ko bhi try karo
    applicationNumber:
      allotment?.applicationNo ||
      allotment?.assetNumber ||
      allotment?.assetNo ||
      "NA",

    isTOCRequired: false,
    heading: t("EST_ACKNOWLEDGEMENT"),

    details: [
      // ---------- 1. ASSET DETAILS ----------
      {
        title: t("EST_ASSET_DETAILS"),
        asSectionHeader: true,
        values: filterEmptyValues([
          {
            title: t("EST_ASSET_NUMBER"),
            value: allotment?.assetNo || allotment?.assetNumber,
          },
          {
            title: t("EST_ASSET_REFERENCE_NUMBER"),
            value: allotment?.assetRefNo || allotment?.assetReferenceNumber,
          },
          {
            title: t("EST_BUILDING_NAME"),
            value: allotment?.buildingName,
          },
          {
            title: t("EST_LOCALITY"),
            value: allotment?.locality?.name || allotment?.locality,
          },
          {
            title: t("EST_TOTAL_AREA"),
            value: allotment?.totalFloorArea || allotment?.totalAreaSqFt,
          },
          {
            title: t("EST_FLOOR"),
            value: allotment?.floorNo || allotment?.floor,
          },
          {
            title: t("EST_RATE"),
            // payload me rentRate aa raha hai
            value: allotment?.rate || allotment?.rentRate,
          },
        ]),
      },

      // ---------- 2. PERSONAL DETAILS OF ALLOTTEE ----------
      {
        title: t("EST_PERSONAL_DETAILS_OF_ALLOTTEE"),
        asSectionHeader: true,
        values: filterEmptyValues([
          {
            title: t("EST_PROPERTY_TYPE"),
            value: allotment?.propertyType
              ? t(
                  `EST_PROPERTY_TYPE_${String(allotment.propertyType)
                    .toUpperCase()
                    .replace(" ", "_")}`
                )
              : "",
          },
          {
            title: t("EST_ALLOTTEE_NAME"),
            value: allotment?.allotteeName,
          },
          {
            title: t("EST_PHONE_NUMBER"),
            // payload: mobileNo
            value: allotment?.phoneNumber || allotment?.mobileNo,
          },
          {
            title: t("EST_ALTERNATE_PHONE_NUMBER"),
            // payload: alternateMobileNo
            value:
              allotment?.altPhoneNumber ||
              allotment?.alternatePhoneNumber ||
              allotment?.alternateMobileNo,
          },
          {
            title: t("EST_EMAIL_ID"),
            value: allotment?.emailId,
          },
        ]),
      },

      // ---------- 3. ALLOTMENT / INVOICE DETAILS ----------
      {
        title: t("EST_ALLOTMENT_INVOICE_DETAILS"),
        asSectionHeader: true,
        values: filterEmptyValues([
          {
            title: t("EST_AGREEMENT_START_DATE"),
            value: allotment?.agreementStartDate,
          },
          {
            title: t("EST_AGREEMENT_END_DATE"),
            value: allotment?.agreementEndDate,
          },
          {
            title: t("EST_DURATION_IN_YEARS"),
            // payload: duration
            value: allotment?.durationInYears || allotment?.duration,
          },
          {
            title: t("EST_RATE_PER_SQFT"),
            // payload: rentRate
            value: allotment?.ratePerSqft || allotment?.rentRate,
          },
          {
            title: t("EST_MONTHLY_RENT_IN_INR"),
            value: allotment?.monthlyRent,
          },
          {
            title: t("EST_ADVANCE_PAYMENT_IN_INR"),
            value: allotment?.advancePayment,
          },
          {
            title: t("EST_ADVANCE_PAYMENT_DATE"),
            value: allotment?.advancePaymentDate,
          },
        ]),
      },

      // ---------- 4. DOCUMENT UPLOAD ----------
      {
        title: t("EST_DOCUMENT_UPLOAD"),
        asSectionHeader: true,
        values: filterEmptyValues([
          {
            title: t("EST_EOFFICE_FILE_NO"),
            // payload: eofficeFileNo (all small)
            value: allotment?.eOfficeFileNo || allotment?.eofficeFileNo,
          },
          {
            title: t("EST_CITIZEN_REQUEST_LETTER"),
            value:
              allotment?.citizenRequestLetterNo ||
              allotment?.citizenRequestLetterFileId,
          },
          {
            title: t("EST_ALLOTMENT_LETTER"),
            value: allotment?.allotmentLetterFileId,
          },
          {
            title: t("EST_SIGNED_DEED"),
            value: allotment?.signedDeedFileId,
          },
        ]),
      },
    ],
  };
};

export default getESTAllotmentAcknowledgementData;
