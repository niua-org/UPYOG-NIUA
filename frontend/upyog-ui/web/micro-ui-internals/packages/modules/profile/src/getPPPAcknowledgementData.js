const getValue = (value) => {
  if (value && typeof value === "object") {
    return value.name || value.code || value.fileStoreId || "";
  }
  return value;
};

const hasValue = (value) => value !== undefined && value !== null && value !== "";

const getTranslatedValue = (value, t) => {
  const translationKey = value?.i18nKey || value?.code || value?.name || value;
  return hasValue(translationKey) ? t(translationKey) : "";
};

const getDocumentName = (file, fileStoreId) => file?.name || getValue(file) || fileStoreId || "";

const getProfileValues = (member, t) => {
  const profile = member?.profileDetails || {};
  const citizen = member?.citizenDetails || {};
  const documentValue = (file, type) => {
    const fileName = getDocumentName(file);
    const documentType = getTranslatedValue(type, t);
    return [documentType, fileName].filter(Boolean).join(" - ");
  };

  return [
    { title: t("CORE_COMMON_PROFILE_NAME"), value: profile.name || getMemberName(member) },
    { title: t("CORE_COMMON_PROFILE_GENDER"), value: getTranslatedValue(profile.gender || citizen.gender, t) },
    { title: t("CORE_COMMON_PROFILE_DOB"), value: profile.dob || citizen.dob },
    {
      title: t("CORE_COMMON_PROFILE_MOBILE_NUMBER"),
      value: profile.mobileNumber || member?.mobileNumber || citizen.mobileNumber,
    },
    {
      title: t("CORE_COMMON_PROFILE_ALT_MOBILE_NUMBER"),
      value: profile.altContactNumber || citizen.alternateMobileNumber,
    },
    { title: t("CORE_COMMON_PROFILE_EMAIL"), value: profile.emailId || member?.emailId || citizen.emailId },
    { title: t("CORE_COMMON_FATHER_NAME"), value: profile.fatherName || citizen.fatherName },
    { title: t("CORE_COMMON_MOTHER_NAME"), value: profile.motherName || citizen.motherName },
    { title: t("CORE_COMMON_SPOUSENAME"), value: profile.spouseName || citizen.spouseName },
    { title: t("CORE_COMMON_GUARDIAN_NAME"), value: profile.guardianName || citizen.guardian },
    {
      title: t("PROFILE_RELATIONSHIP_WITH_HOF"),
      value: getTranslatedValue(profile.relationshipWithHof || getMemberRelationship(member), t),
    },
    { title: t("PROFILE_SPECIALLY_ABLED"), value: getTranslatedValue(profile.speciallyAbled, t) },
    { title: t("PROFILE_CASTE"), value: getTranslatedValue(profile.caste, t) },
    {
      title: t("PROFILE_IDENTITY_PROOF"),
      value: documentValue(profile.identityProofFile, profile.identityProofType),
    },
    {
      title: t("PROFILE_BIRTH_PROOF"),
      value: documentValue(profile.birthProofFile, profile.birthProofType),
    },
    {
      title: t("PROFILE_CASTE_PROOF"),
      value: documentValue(profile.casteProofFile || profile.casteProof, profile.casteProofType),
    },
    {
      title: t("PROFILE_DISABILITY_PROOF"),
      value: documentValue(profile.disabilityProofFile || profile.disabilityProof, profile.disabilityProofType),
    },
  ].filter((item) => hasValue(item.value));
};

const getIncomeValues = (income, t) => {
  if (!income || !Object.keys(income).length) return [];

  return [
    { title: t("PROFILE_OCCUPATION"), value: getTranslatedValue(income.occupation, t) },
    {
      title: t("PROFILE_ANNUAL_INCOME"),
      value: hasValue(income.annualIncome) ? `₹ ${income.annualIncome}` : "",
    },
    { title: t("PROFILE_TAX_ID_TYPE"), value: getTranslatedValue(income.taxIdType, t) },
    { title: t("PROFILE_TAX_ID_NUMBER"), value: income.taxIdNumber },
    {
      title: t("PROFILE_INCOME_PROOF"),
      value: getTranslatedValue(income.incomeProofType, t),
    },
    {
      title: t("PROFILE_INCOME_PROOF"),
      value: getDocumentName(income.incomeProofFile, income.uploadedIncomeFile),
    },
    {
      title: t("PROFILE_TAX_PROOF"),
      value: getDocumentName(income.taxProof, income.uploadedTaxProof),
    },
    {
      title: t("PROFILE_TAX_ID_VALID"),
      value: hasValue(income.taxIdValidated)
        ? t(income.taxIdValidated ? "CORE_COMMON_YES" : "CORE_COMMON_NO")
        : "",
    },
  ].filter((item) => hasValue(item.value));
};

const getVehicleValues = (vehicle, t) => {
  if (!vehicle || !Object.keys(vehicle).length) return [];

  const values = [
    {
      title: t("PROFILE_DO_YOU_HAVE_VEHICLE"),
      value: getTranslatedValue(
        vehicle.hasVehicle === "Yes" ? "COMMON_YES" : vehicle.hasVehicle === "No" ? "COMMON_NO" : vehicle.hasVehicle,
        t
      ),
    },
  ];

  if (vehicle.hasVehicle === "No") return values.filter((item) => hasValue(item.value));

  return values.concat([
    { title: t("PROFILE_VEHICLE_TYPE"), value: getTranslatedValue(vehicle.vehicleType, t) },
    { title: t("PROFILE_VEHICLE_CATEGORY"), value: getTranslatedValue(vehicle.vehicleCategory, t) },
    { title: t("PROFILE_VEHICLE_FUEL_TYPE"), value: getTranslatedValue(vehicle.fuelType, t) },
    { title: t("PROFILE_VEHICLE_NO"), value: vehicle.vehicleNo },
    { title: t("PROFILE_VEHICLE_PURCHASE_YEAR"), value: vehicle.purchaseYear },
    { title: t("PROFILE_VEHICLE_RC"), value: getDocumentName(vehicle.vehicleRc, vehicle.uploadedRCFile) },
    {
      title: t("PROFILE_VEHICLE_NO_VALID"),
      value: hasValue(vehicle.vehicleNoValidated)
        ? t(vehicle.vehicleNoValidated ? "CORE_COMMON_YES" : "CORE_COMMON_NO")
        : "",
    },
  ]).filter((item) => hasValue(item.value));
};

const getMemberName = (member) =>
  member?.name || member?.citizenDetails?.name || member?.profileDetails?.name || "";

const getMemberRelationship = (member) =>
  member?.relationshipWithHead ||
  member?.relationshipWithHof?.code ||
  member?.relationshipWithHof?.name ||
  "";

const getPropertyValues = (property, t) => {
  if (!property) return [];
  if (property.noProperty) {
    return [{ title: t("PROFILE_PROPERTY_DETAILS"), value: t("PROFILE_NO_PROPERTY") }];
  }

  const portfolio = property.propertiesPortfolio || [];
  if (portfolio.length) {
    return portfolio.flatMap((item, index) => {
      const propertyNumber = portfolio.length > 1 ? ` ${index + 1}` : "";
      const isUrban = item.localityType === "URBAN";
      const values = [
        { title: `${t("PROFILE_LOCALITY_TYPE")}${propertyNumber}`, value: t(isUrban ? "URBAN" : "RURAL") },
      ];

      if (isUrban) {
        values.push(
          { title: `${t("PROFILE_PROPERTY_ID")}${propertyNumber}`, value: item.propertyId || item.primaryPropertyId },
          { title: `${t("PROFILE_PROPERTY_LOCATION")}${propertyNumber}`, value: item.propertyAddress },
          { title: `${t("PROFILE_PROPERTY_TAX_AMOUNT")}${propertyNumber}`, value: hasValue(item.propertyTaxAmount) ? `₹ ${item.propertyTaxAmount}` : "" },
          { title: `${t("PROFILE_OWNER_NAME")}${propertyNumber}`, value: item.ownerName },
          { title: `${t("PROFILE_STATUS")}${propertyNumber}`, value: item.status },
          {
            title: `${t("PROFILE_PRIMARY_ADDRESS")}${propertyNumber}`,
            value: hasValue(item.isPrimaryAddress)
              ? t(item.isPrimaryAddress ? "CORE_COMMON_YES" : "CORE_COMMON_NO")
              : "",
          }
        );
      } else {
        values.push(
          { title: `${t("PROFILE_GRAM_PANCHAYAT")}${propertyNumber}`, value: getValue(item.gramPanchayat) },
          { title: `${t("PROFILE_VILLAGE")}${propertyNumber}`, value: getValue(item.village) },
          { title: `${t("PROFILE_WARD_NUMBER")}${propertyNumber}`, value: getValue(item.ward) },
          { title: `${t("PROFILE_SURVEY_NUMBER")}${propertyNumber}`, value: item.surveyNumber },
          { title: `${t("COMMON_ADDRESS")}${propertyNumber}`, value: item.propertyAddress },
          { title: `${t("PROFILE_PROPERTY_TYPE")}${propertyNumber}`, value: getValue(item.propertyType) },
          { title: `${t("PROFILE_PROPERTY_USAGE")}${propertyNumber}`, value: getValue(item.propertyUsage) },
          { title: `${t("PROFILE_OWNERSHIP_TYPE")}${propertyNumber}`, value: getValue(item.ownershipType) },
          { title: `${t("PROFILE_JOINT_OWNER_NAME")}${propertyNumber}`, value: item.ownerName },
          { title: `${t("PROFILE_JOINT_OWNER_MOBILE")}${propertyNumber}`, value: item.ownerMobile },
          { title: `${t("PROFILE_ADDRESS_PROOF")}${propertyNumber}`, value: getValue(item.documentType) },
          {
            title: `${t("PROFILE_DOCUMENT_UPLOAD")}${propertyNumber}`,
            value: getValue(item.propertyDocument),
          }
        );
      }

      return values.filter((value) => hasValue(value.value));
    });
  }

  const isUrban = property.localityType === "URBAN";
  const values = [
    { title: t("PROFILE_LOCALITY_TYPE"), value: t(isUrban ? "URBAN" : "RURAL") },
  ];

  if (isUrban) {
    values.push(
      { title: t("PROFILE_PROPERTY_ID"), value: property.primaryPropertyId || property.propertyId },
      { title: t("PROFILE_PROPERTY_LOCATION"), value: property.propertyAddress },
      { title: t("PROFILE_PROPERTY_TAX_AMOUNT"), value: hasValue(property.propertyTaxAmount) ? `₹ ${property.propertyTaxAmount}` : "" },
      { title: t("PROFILE_OWNER_NAME"), value: property.ownerName },
      { title: t("PROFILE_STATUS"), value: property.status },
      {
        title: t("PROFILE_PRIMARY_ADDRESS"),
        value: hasValue(property.isPrimaryAddress)
          ? t(property.isPrimaryAddress ? "CORE_COMMON_YES" : "CORE_COMMON_NO")
          : "",
      }
    );
  } else {
    values.push(
      { title: t("PROFILE_GRAM_PANCHAYAT"), value: getValue(property.gramPanchayat) },
      { title: t("PROFILE_VILLAGE"), value: getValue(property.village) },
      { title: t("PROFILE_WARD_NUMBER"), value: getValue(property.ward) },
      { title: t("PROFILE_SURVEY_NUMBER"), value: property.surveyNumber },
      { title: t("COMMON_ADDRESS"), value: property.propertyAddress },
      { title: t("PROFILE_PROPERTY_TYPE"), value: getValue(property.propertyType) },
      { title: t("PROFILE_PROPERTY_USAGE"), value: getValue(property.propertyUsage) },
      { title: t("PROFILE_OWNERSHIP_TYPE"), value: getValue(property.ownershipType) },
      { title: t("PROFILE_JOINT_OWNER_NAME"), value: property.ownerName },
      { title: t("PROFILE_JOINT_OWNER_MOBILE"), value: property.ownerMobile },
      { title: t("PROFILE_ADDRESS_PROOF"), value: getValue(property.documentType) },
      { title: t("PROFILE_DOCUMENT_UPLOAD"), value: getValue(property.propertyDocument) }
    );
  }

  return values.filter((item) => hasValue(item.value));
};

const getPPPAcknowledgementData = (acknowledgement, tenantInfo, t) => {
  const familyId = acknowledgement?.familyId;
  const members = acknowledgement?.members || acknowledgement?.family?.members || [];
  const applicationDate = acknowledgement?.applicationDate || acknowledgement?.createdTime || Date.now();
  const dateValue = Digit.DateUtils.ConvertTimestampToDate(applicationDate, "dd/MM/yyyy");

  const memberValues = members.flatMap((member, index) => {
    const memberNumber = members.length > 1 ? ` ${index + 1}` : "";
    const relationship = getMemberRelationship(member);
    return [
      { title: `${t("CORE_COMMON_NAME")}${memberNumber}`, value: getMemberName(member) },
      {
        title: `${t("PROFILE_RELATIONSHIP_WITH_HOF")}${memberNumber}`,
        value: relationship ? t(relationship) : "",
      },
      {
        title: `${t("PROFILE_MEMBER_STATUS")}${memberNumber}`,
        value: member?.isHead ? t("PROFILE_HEAD_OF_FAMILY") : t("PROFILE_FAMILY_MEMBER"),
      },
    ].filter((item) => item.value);
  });

  const details = [
    {
      title: t("PROFILE_FAMILY_DETAILS"),
      values: [
        { title: t("CORE_COMMON_FAMILY_ID"), value: familyId },
        { title: t("PROFILE_APPLICATION_DATE"), value: dateValue },
      ].filter((item) => item.value),
    },
    {
      title: t("PROFILE_FAMILY_MEMBERS"),
      values: memberValues.length
        ? memberValues
        : [{ title: t("PROFILE_FAMILY_MEMBERS"), value: t("CS_NA") }],
    },
  ];

  members.forEach((member) => {
    const memberName = getMemberName(member);
    const memberSuffix = memberName ? ` - ${memberName}` : "";
    const profileValues = getProfileValues(member, t);
    const incomeValues = getIncomeValues(member?.income, t);
    const vehicleValues = getVehicleValues(member?.vehicles, t);
    const propertyValues = getPropertyValues(member?.property, t);

    if (profileValues.length) {
      details.push({
        title: `${t(member?.isHead ? "PROFILE_APPLICANT_DETAILS" : "PROFILE_FAMILY_MEMBER")}${memberSuffix}`,
        values: profileValues,
      });
    }

    if (incomeValues.length) {
      details.push({
        title: `${t("ES_TITLE_INCOME_DETAILS")}${memberSuffix}`,
        values: incomeValues,
      });
    }

    if (vehicleValues.length) {
      details.push({
        title: `${t("ES_TITLE_VEHICLE_DETAILS")}${memberSuffix}`,
        values: vehicleValues,
      });
    }

    if (propertyValues.length) {
      details.push({
        title: `${t("PROFILE_PROPERTY_DETAILS")}${memberSuffix}`,
        values: propertyValues,
      });
    }
  });

  if (!members.length && acknowledgement?.property) {
    details.push({
      title: t("PROFILE_PROPERTY_DETAILS"),
      values: getPropertyValues(acknowledgement.property, t),
    });
  }

  return {
    t,
    tenantId: tenantInfo?.code || acknowledgement?.tenantId,
    name: tenantInfo?.i18nKey ? t(tenantInfo.i18nKey) : tenantInfo?.code,
    email: tenantInfo?.emailId,
    phoneNumber: tenantInfo?.contactNumber,
    heading: t("PROFILE_PPP_ACKNOWLEDGEMENT_HEADING"),
    applicationNumber: familyId,
    isTOCRequired: false,
    details,
  };
};

export default getPPPAcknowledgementData;
