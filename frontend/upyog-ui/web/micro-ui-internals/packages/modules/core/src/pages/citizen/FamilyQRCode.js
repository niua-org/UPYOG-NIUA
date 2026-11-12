import React, { useEffect, useState } from "react";
import { Card, Row, CardHeader, StatusTable, Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";

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

const formatEpochToDate = (dateEpoch) => {
  const dateFromApi = parseDateSafe(dateEpoch);
  if (!dateFromApi) return "NA";
  const month = String(dateFromApi.getMonth() + 1).padStart(2, "0");
  const day = String(dateFromApi.getDate()).padStart(2, "0");
  const year = dateFromApi.getFullYear();
  return `${day}/${month}/${year}`;
};

const calculateAgeFromDob = (dob) => {
  const birthDate = parseDateSafe(dob);
  if (!birthDate) return "NA";
  const ageDiff = Date.now() - birthDate.getTime();
  const ageDate = new Date(ageDiff);
  return String(Math.abs(ageDate.getUTCFullYear() - 1970));
};

const maskAadhaar = (aadhaar) => {
  if (!aadhaar) return "NA";
  const str = String(aadhaar);
  return str.length >= 4 ? `XXXX-XXXX-${str.slice(-4)}` : str;
};

const FamilyQRCode = () => {
  const { t } = useTranslation();
  const params = new URLSearchParams(window.location.search);
  const tenantId = params.get("tenantId");
  const familyId = params.get("familyId");

  const { data: familyData, isLoading: familyLoading } = Digit.Hooks.useFamilyQrCode({
    tenantId,
    familyId,
  });

  const [memberProfiles, setMemberProfiles] = useState({});
  const family = familyData?.Families?.[0];

  useEffect(() => {
    if (!family?.members?.length) return;
    let isCancelled = false;

    const fetchMemberProfiles = async () => {
      const profilesMap = {};

      await Promise.all(
        family.members.map(async (member) => {
          const memberUuid = member.userUuid;
          const memberMobile = member.mobileNumber;
          const memberTenant = member.tenantId;
          if (!memberUuid) return;

          try {
            const res = await Digit.UserService.userSearch(
              memberTenant,
              {
                uuid: memberUuid,
                include: ["ADDRESS", "PROFILE", "DOCUMENT"],
                ...(memberMobile ? { mobileNumber: memberMobile } : {}),
              },
              {}
            );
            if (res?.user?.[0]) {
              profilesMap[memberUuid] = res.user[0];
            }
          } catch (e) {
            // Fallback to family/_search member data when user profile lookup is unavailable
          }
        })
      );

      if (!isCancelled) {
        setMemberProfiles(profilesMap);
      }
    };

    fetchMemberProfiles();
    return () => {
      isCancelled = true;
    };
  }, [family, tenantId]);

  const resolvedAddr = memberProfiles[family?.headUserUuid]?.addresses?.[0];

  const district = resolvedAddr?.city || "NA";
  const city = resolvedAddr?.city || "NA";
  const ward = resolvedAddr?.locality || "NA";
  const address = resolvedAddr?.address || "NA";

  const members = family?.members || [];
  let totalIncome = 0;
  let hasIncome = false;
  members.forEach((m) => {
    const inc = m.memberIncome ?? m.additionalDetails?.memberIncome;
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
          <CardHeader>{t("Family Identification Summary")}</CardHeader>
          {familyLoading ? (
            <Loader />
          ) : family ? (
            <React.Fragment>
              <StatusTable>
                <Row label={t("CORE_COMMON_FAMILY_ID")} text={family.familyId || "NA"} />
                <Row label={t("ISSUE_DATE")} text={formatEpochToDate(family.auditDetails?.createdTime)} />
                <Row label={t("HEAD_OF_FAMILY")} text={family.headName || "NA"} />
                <Row label={t("CORE_COMMON_STATUS")} text={family.status || "NA"} />
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
                      const profile = memberProfiles[member.userUuid];
                      const name = member.name || "NA";
                      const fatherName = profile?.fatherName || "NA";
                      const motherName = profile?.motherName || "NA";
                      const dob = formatEpochToDate(profile?.dob);
                      const age = calculateAgeFromDob(profile?.dob);
                      const mobile = member.mobileNumber || "NA";
                      const aadhaar = maskAadhaar(profile?.aadhaarNumber);
                      const income = member.memberIncome ?? "NA";
                      const occupation = member.occupation || "NA";
                      const caste = member.caste || "NA";
                      const isDivyang = member.isDivyang || "N";

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

