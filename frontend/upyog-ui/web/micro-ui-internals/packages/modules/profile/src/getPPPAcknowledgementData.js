const hasValue = (value) => value !== undefined && value !== null && value !== "";

const getMemberName = (member) =>
  member?.name || member?.citizenDetails?.name || member?.profileDetails?.name || "";

const getMemberRelationship = (member) =>
  member?.relationshipWithHead ||
  member?.relationshipWithHof?.code ||
  member?.relationshipWithHof?.name ||
  "";

const getTranslatedValue = (value, t) => {
  const translationKey = value?.i18nKey || value?.code || value?.name || value;
  return hasValue(translationKey) ? t(translationKey) : "";
};

const getProfileValues = (member, t) => {
  const profile = member?.profileDetails || {};
  const citizen = member?.citizenDetails || {};
  const documentName = (file) => {
    if (file && typeof file !== "object") return file;
    return file?.name || file?.fileName || file?.fileStoreId || file?.filestoreId || "";
  };
  const documentValue = (file, type) =>
    [getTranslatedValue(type, t), documentName(file)].filter(Boolean).join(" - ");

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
    { title: t("PROFILE_IDENTITY_PROOF"), value: documentValue(profile.identityProofFile, profile.identityProofType) },
    { title: t("PROFILE_BIRTH_PROOF"), value: documentValue(profile.birthProofFile, profile.birthProofType) },
    {
      title: t("PROFILE_CASTE_PROOF"),
      value: documentValue(profile.casteProofFile || profile.casteProof, profile.casteProofType),
    },
    {
      title: t("PROFILE_DISABILITY_PROOF"),
      value: documentValue(profile.disabilityProofFile || profile.disabilityProof, profile.disabilityProofType),
    },
    ...(citizen.documents || member?.documents || []).map((document, index) => ({
      title: `${t("PROFILE_IDENTITY_PROOF")} ${index + 1}`,
      value: documentValue(document, document.documentType),
    })),
  ].filter((item) => hasValue(item.value));
};

/**
 * Generates PPP family acknowledgement data for the PDF.
 * @param {Object} acknowledgement - Family ID, application date, and family member details.
 * @param {Object} tenantInfo - Tenant details used in the acknowledgement header.
 * @param {Function} t - Translation function for localized labels and values.
 * @returns {Object} PDF acknowledgement data containing family and member detail tables.
 */
const getPPPAcknowledgementData = (acknowledgement, tenantInfo, t) => {
  const members = Array.isArray(acknowledgement?.members)
    ? acknowledgement.members
    : Array.isArray(acknowledgement?.family?.members)
      ? acknowledgement.family.members
      : [];
  const applicationDate = acknowledgement?.applicationDate || acknowledgement?.createdTime || Date.now();
  const fieldLabel = t("CORE_COMMON_FIELDS", { defaultValue: "Fields" });
  const valueLabel = t("CORE_COMMON_DETAILS", { defaultValue: "Details" });
  const nameLabel = t("CORE_COMMON_PROFILE_NAME");
  const relationshipLabel = t("PROFILE_RELATIONSHIP_WITH_HOF");
  const asTable = (title, headers, rows, keepTogether = false) => ({
    title,
    isPppAcknowledgement: true,
    isTable: true,
    headers,
    widths: ["*", "*"],
    hideSectionNumber: true,
    keepTogether,
    tableRows: rows,
    values: rows.map(([field, value]) => ({ title: field, value })),
  });
  const details = [
    asTable(t("PROFILE_FAMILY_DETAILS"), [fieldLabel, valueLabel], [
      [t("CORE_COMMON_FAMILY_ID"), acknowledgement?.familyId],
      [
        t("PROFILE_APPLICATION_DATE"),
        Digit.DateUtils.ConvertTimestampToDate(applicationDate, "dd/MM/yyyy"),
      ],
    ].filter((row) => hasValue(row[1]))),
  ];

  if (members.length) {
    members.forEach((member, index) => {
      const memberFields = getProfileValues(member, t).filter(
        ({ title }) => title !== nameLabel && title !== relationshipLabel
      );
      details.push(
        asTable(
          `${t("PROFILE_FAMILY_MEMBER")} ${index + 1}`,
          [fieldLabel, valueLabel],
          [
            [nameLabel, getMemberName(member) || t("CS_NA")],
            [relationshipLabel, getTranslatedValue(getMemberRelationship(member), t)],
            ...memberFields.map(({ title, value }) => [title, value]),
          ],
          true
        )
      );
    });
  } else {
    details.push(
      asTable(`${t("PROFILE_FAMILY_MEMBER")} 1`, [fieldLabel, valueLabel], [[t("CS_NA"), ""]], true)
    );
  }

  return {
    t,
    tenantId: tenantInfo?.code || acknowledgement?.tenantId,
    name: tenantInfo?.i18nKey ? t(tenantInfo.i18nKey) : tenantInfo?.code,
    email: tenantInfo?.emailId,
    phoneNumber: tenantInfo?.contactNumber,
    heading: t("PROFILE_PPP_ACKNOWLEDGEMENT_HEADING"),
    isTOCRequired: false,
    details,
  };
};

export default getPPPAcknowledgementData;
