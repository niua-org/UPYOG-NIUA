import React, { useMemo, useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { Card, CardHeader, CardLabel, Dropdown, SubmitBar, Loader,Toast } from "@nudmcdgnpm/digit-ui-react-components";
import { FieldGrid, Section, getFamilyHeadFields, getPersonalFields } from "../utils";
import { PROFILE_ROUTES } from "../utils/constants";

// Function to get user Details
const useProfileUser = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const uuid = Digit.UserService.getUser()?.info?.uuid;
  const tenantId = Digit.ULBService.getCurrentTenantId();

  useEffect(() => {
    let isActive = true; // avoids state update after unmount
    const fetchUser = async () => {
      if (!uuid) {
        setLoading(false);
        return;
      }
      try {
        const response = await Digit.UserService.userSearch(tenantId, { uuid: [uuid], include: ["ADDRESS", "PROFILE", "DOCUMENT"] }, {});
        if (isActive) setUser(response?.user?.[0] || null);
      } catch (e) {
        if (isActive) setError(true);
      } finally {
        if (isActive) setLoading(false);
      }
    };
    fetchUser();
    return () => {
      isActive = false;
    };
  }, [uuid, tenantId]);

  return { user, loading, error };
};

/**
 * Shows the verified head-of-family details and lets the applicant
 * Pick a relationship and join the family.
 */
const FoundFamily = () => {
  const { t } = useTranslation();
  const stateId = Digit.ULBService.getStateId();
  const navigate = Digit.Hooks.useCustomNavigate();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const { verifiedData, familyId } = useLocation().state || {};
  const { user, loading, error } = useProfileUser();
  const [submitting, setSubmitting] = useState(false);
  const [relationship, setRelationship] = useState(null);
  const userUuid = Digit.UserService.getUser()?.info?.uuid;
  const [toast, setToast] = useState(null);
  const showError = (label) => setToast({ error: true, label });

  // Mdms Hook Call to get the Family Relationship Master Data
  const { data: FamilyRelationShips } = Digit.Hooks.useEnabledMDMS(stateId, "common-masters", [{ name: "FamilyRelationship" }], {
    select: (data) => {
      const formattedData = data?.["common-masters"]?.["FamilyRelationship"];
      const activeData = formattedData?.filter((option) => option.active);
      return activeData;
    },
  });
  let familyRelationShips = [];
  FamilyRelationShips &&
    FamilyRelationShips.map((relation) => {
      familyRelationShips.push({ i18nKey: `${relation.code}`, code: `${relation.code}`, name: `${relation.name}` });
    });


  // memorized fields to avoid unnecessary re-renders, will show the data of the family head and the applicant
  const familyHeadFields = useMemo(() => getFamilyHeadFields(verifiedData, t), [verifiedData, t]);
  const applicantFields = useMemo(() => (user ? getPersonalFields(user) : []), [user]);
  if (!verifiedData) return null;

  // Thsi function will make a payload and call the join family API to join the family
  const handleJoin = async () => {
    setSubmitting(true);
    try {
      const res = await Digit.UserService.joinFamily({
        familyId: verifiedData.familyId,
        tenantId,
        applicantUserUuid: userUuid,
        relationshipWithHead: relationship.code,
        hofConsentOtp: verifiedData.consentToken,
      });
      // if we get a response with status not equal to JOINED then we will show an error toast
      if (res?.JoinRequest?.status !== "JOINED") throw new Error("JOIN_FAILED");
      navigate(PROFILE_ROUTES.FAMILY_JOINED, { replace: true, state: { joinData: res.JoinRequest } });
    } catch (err) {
      console.log("error on join family page", err);
      showError(err?.response?.data?.Errors?.[0]?.message || t("PROFILE_JOIN_FAMILY_FAILED"));
      setSubmitting(false);
    }
  };

  return (
    <React.Fragment>
      <Card>
        <CardHeader>{t("PROFILE_FOUND_FAMILY")}</CardHeader>
        <FieldGrid fields={familyHeadFields} t={t} />
      </Card>

      <Card>
        <CardLabel>{t("PROFILE_RELATIONSHIP_WITH_HEAD")} *</CardLabel>
        <Dropdown
          option={familyRelationShips}
          selected={relationship}
          select={setRelationship}
          optionKey="i18nKey"
          t={t}
          placeholder={t("COMMON_SELECT")}
        />

        <Section title={t("PROFILE_JOINING_AS")}>{loading ? <Loader /> : <FieldGrid fields={applicantFields} t={t} />}</Section>

        <SubmitBar label={t("PROFILE_JOIN_FAMILY")} onSubmit={handleJoin} disabled={!relationship || submitting} />
      </Card>

      {toast && <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />}
    </React.Fragment>
  );
};

export default FoundFamily;
