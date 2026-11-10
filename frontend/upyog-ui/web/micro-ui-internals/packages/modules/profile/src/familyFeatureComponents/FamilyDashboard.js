import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, Loader, Toast } from "@nudmcdgnpm/digit-ui-react-components";

import { PROFILE_ROUTES } from "../utils/constants";
import { profileStyles } from "../utils/profileStyles";

const FamilyDashboard = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const styles = profileStyles.myFamilyDashboard;
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    const fetchFamilyDashboard = async () => {
      try {
        setLoading(true);
        const uuid = Digit.UserService.getUser()?.info?.uuid;

        if (!uuid) {
          throw new Error("USER_NOT_FOUND");
        }
        const userResponse =
          await Digit.UserService.userSearch(
            tenantId,
            { uuid, include: ["ADDRESS", "PROFILE", "DOCUMENT"] },
            {}
          );

        const user = userResponse?.user?.[0];
        const familyId = user?.familyId ||  user?.additionalDetails?.familyId;

        if (!familyId) {
          throw new Error("FAMILY_ID_NOT_FOUND");
        }
        const response = await fetch(`/family/v1/dashboard/_search?tenantId=${tenantId}&familyId=${familyId}`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              RequestInfo: {
                apiId: "Rainmaker",
                ver: "1.0",
                ts: Date.now(),
                action: "",
                did: "1",
                key: "",
                msgId: `${Date.now()}|en_IN`,
                requesterId: "",
                authToken:
                  Digit.UserService.getUser()?.access_token,
              },
            }),
          }
        );

        const data = await response.json();
        console.log("Family Dashboard Response:", data);
        if (!response.ok) {
          throw new Error("FAMILY_DASHBOARD_FAILED");
        }
        setDashboard(data?.dashboard);
      } catch (error) {
        console.error( "Family Dashboard Error:", error);
        setToast({
          error: true,
          label: error.message === "FAMILY_ID_NOT_FOUND" ? "Family ID not found" : "Unable to fetch family details",
        });
      } finally {
        setLoading(false);
      }
    };

    fetchFamilyDashboard();
  }, [tenantId]);

  const handleAddNewMember = () => {
    navigate(PROFILE_ROUTES.ADD_FAMILY_MEMBER);
  };

  const handleGoToDashboard = () => {
    navigate(PROFILE_ROUTES.DETAILS);
  };

  const getInitials = (name) => {
    if (!name) return "";

    const words = name.trim().split(/\s+/);

    if (words.length === 1) {
      return words[0].substring(0, 2).toUpperCase();
    }

    return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase();
  };

  if (loading) {
    return (
      <Card style={styles.pageCard}> <Loader /></Card>
    );
  }

  if (!dashboard) {
    return (
      <Card style={styles.pageCard}>
        <div style={styles.pageTitle}>{t("PROFILE_MY_FAMILY", "My Family")}</div>
        <div style={styles.familySummary}> Unable to load family details. </div>
        {toast && (
          <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />
        )}
      </Card>
    );
  }
  const members = dashboard.activeMembers || [];
  const headOfFamily = members.find(
    (member) => member.isHead === true
  );

  return (
    <React.Fragment>
      <Card style={styles.pageCard}>
        <div style={styles.headerRow}>
          <div style={styles.pageTitle}> {t("PROFILE_MY_FAMILY", "My Family")} </div>
          <div style={styles.familyIdBadge}>{dashboard.familyId} </div>
        </div>

        <div style={styles.familySummary}>
          <div style={styles.summaryLine}>
            <span style={styles.summaryLabel}>{t("PROFILE_HEAD_OF_FAMILY", "Head of Family")}:</span>
            <span style={styles.summaryValue}>{headOfFamily?.name ||dashboard.headName}
            </span>
          </div>

          <div style={styles.summaryLine}>
            <span style={styles.summaryLabel}>
              {t( "PROFILE_TOTAL_REGISTERED_MEMBERS", "Total Registered Members")}:
            </span>
            <span style={styles.summaryValue}> {dashboard.totalMembersCount} </span>
          </div>
        </div>
        <div style={styles.membersHeader}>
          <div style={styles.membersTitle}>
            {t("PROFILE_REGISTERED_MEMBERS", "REGISTERED MEMBERS")}
          </div>
          <button type="button" onClick={handleAddNewMember} style={styles.addMemberLink}>
            <span style={styles.addMemberPlus}> +</span>
            <span> {t("PROFILE_ADD_NEW_MEMBER") || "Add New Member"}</span>
          </button>
        </div>
        <div style={styles.membersGrid}>
          {members.map((member) => {
            const name =
              member.name ||
              member.citizenDetails?.name ||
              "";
            const relationship =
              member.relationshipWithHead || member.citizenDetails?.relationshipWithHead ||"";
            const isYou = member.userUuid === Digit.UserService.getUser()?.info?.uuid;
            return (
              <div key={member.id}   style={styles.memberCard}>
                <div style={styles.avatar}>{getInitials(name)} </div>
                <div style={styles.memberName}>{name}</div>
                <div style={styles.memberRelationship}>
                  <span style={styles.bullet}>• </span>
                  <span> {relationship} {isYou && (
                      <span style={styles.youText}>{" "}
                        ({t("CORE_COMMON_YOU") || "You"})
                      </span>
                    )}
                  </span>
                </div>
                {member.isVerified && (
                  <div style={styles.verifiedBadge}>
                    {t("PROFILE_VERIFIED") || "VERIFIED"}
                  </div>
                )}
              </div>
            );
          })}
        </div>
        <button type="button" onClick={handleGoToDashboard} style={styles.dashboardButton}>
          {t("PROFILE_GO_TO_MY_FAMILY_DASHBOARD" , "Go to My Family Dashboard")}
        </button>
      </Card>
      {toast && (
        <Toast error={toast.error}  label={toast.label} onClose={() => setToast(null)} />
      )}
    </React.Fragment>
  );
};

export default FamilyDashboard;