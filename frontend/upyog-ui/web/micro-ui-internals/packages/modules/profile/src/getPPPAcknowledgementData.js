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

const formatDateOfBirth = (value) => {
  if (!hasValue(value)) return "";

  const dateValue = String(value);
  const dateOnly = dateValue.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (dateOnly) return `${dateOnly[3]}/${dateOnly[2]}/${dateOnly[1]}`;

  const timestamp = Number(value);
  if (Number.isFinite(timestamp)) {
    return Digit.DateUtils.ConvertTimestampToDate(timestamp, "dd/MM/yyyy");
  }

  return dateValue;
};

const getProfileValues = (member, t) => {
  const profile = member?.profileDetails || {};
  const citizen = member?.citizenDetails || {};

  return [
    { title: t("CORE_COMMON_PROFILE_GENDER"), value: getTranslatedValue(profile.gender || citizen.gender, t) },
    {
      title: t("CORE_COMMON_PROFILE_DOB"),
      value: formatDateOfBirth(profile.dob || citizen.dob || member?.dob),
    },
    {
      title: t("CORE_COMMON_PROFILE_MOBILE_NUMBER"),
      value: profile.mobileNumber || member?.mobileNumber || citizen.mobileNumber,
    },
    { title: t("CORE_COMMON_PROFILE_EMAIL"), value: profile.emailId || member?.emailId || citizen.emailId },
    { title: t("CORE_COMMON_FATHER_NAME"), value: profile.fatherName || citizen.fatherName },
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
      details.push(
        asTable(
          `${t("PROFILE_FAMILY_MEMBER")} ${index + 1}`,
          [fieldLabel, valueLabel],
          [
            [nameLabel, getMemberName(member) || t("CS_NA")],
            [relationshipLabel, getTranslatedValue(getMemberRelationship(member), t)],
            ...getProfileValues(member, t).map(({ title, value }) => [title, value]),
          ],
          true
        )
      );
    });
  } else {
    details.push(
      asTable(
        `${t("PROFILE_FAMILY_MEMBER")} 1`,
        [fieldLabel, valueLabel],
        [[t("CS_NA"), ""]],
        true
      )
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