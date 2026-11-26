import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, Loader, Toast } from "@nudmcdgnpm/digit-ui-react-components";

import { PROFILE_ROUTES } from "../utils/constants";
import AadhaarVerificationModal from "../verifications/AadhaarVerificationModal";

/**
 * Family dashboard screen. 
 * Loads the logged-in user's family details using the family dashboard API
 * and displays the family members and their relationships.
 * Routed from the MDMS route `PROFILE_FAMILY_DASHBOARD`.
 */


const FamilyDashboard = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  const [showAddMemberModal, setShowAddMemberModal] = useState(false);
  const loggedInUserUuid = Digit.UserService.getUser()?.info?.uuid;

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
        const data = await Digit.UserService.familyDashboard(tenantId, familyId);
        setDashboard(data?.dashboard);
      } catch (error) {
        console.error( "Family Dashboard Error:", error);
        setToast({
          error: true,
          label: error.message === "FAMILY_ID_NOT_FOUND" ? "FAMILY_ID_NOT_FOUND" : "FAMILY_ID_NOT_FOUND_FETCH_DETAILS",
        });
      } finally {
        setLoading(false);
      }
    };

    fetchFamilyDashboard();
  }, [tenantId]);
  /**
 * Generates initials from a member's name. Uses the first two characters for a single-word name
 * and the first character of the first and last words for a multi-word name.
 */

  const handleAddNewMember = () => {
    setShowAddMemberModal(true);
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
      <Card className="family-dashboard-page-card"> <Loader /></Card>
    );
  }

  if (!dashboard) {
    return (
      <Card className="family-dashboard-page-card">
        <div className="family-dashboard-page-title">{t("PROFILE_MY_FAMILY")}</div>
        <div className="family-dashboard-summary">{t("PROFILE_FAMILY_LOAD_ERROR")}</div>
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
      <Card className="family-dashboard-page-card">
        <div className="family-dashboard-header-row">
          <div className="family-dashboard-page-title">{t("PROFILE_MY_FAMILY")} </div>
          <div className="family-dashboard-family-id-badge">{dashboard.familyId} </div>
        </div>

        <div className="family-dashboard-summary">
          <div className="family-dashboard-summary-line">
            <span className="family-dashboard-summary-label">{t("PROFILE_HEAD_OF_FAMILY")}:</span>
            <span className="family-dashboard-summary-value">{headOfFamily?.name ||dashboard.headName}
            </span>
          </div>

          <div className="family-dashboard-summary-line">
            <span className="family-dashboard-summary-label">
              {t("PROFILE_TOTAL_REGISTERED_MEMBERS")}:
            </span>
            <span className="family-dashboard-summary-value"> {dashboard.totalMembersCount} </span>
          </div>
        </div>
        <div className="family-dashboard-members-header">
          <div className="family-dashboard-members-title">
            {t("PROFILE_REGISTERED_MEMBERS")}
          </div>
        </div>
        <div className="family-dashboard-members-grid">
          {members.map((member) => {
            const name =
              member.name ||
              member.citizenDetails?.name ||
              "";
            const relationship = member.relationshipWithHead || member.citizenDetails?.relationshipWithHead ||"";
            const isCurrentUser = member.userUuid === loggedInUserUuid;
            const isHead = member.isHead === true;
            const isVerified = member.isVerified === true;
            
            return (
              <div key={member.id} className="family-dashboard-member-card">
                <div className="family-dashboard-avatar">{getInitials(name)} </div>
                <div className="family-dashboard-member-name">{name}</div>
                <div className="family-dashboard-member-relationship">
                  <span className="family-dashboard-bullet">•</span>
                  <span>
                     {isCurrentUser ? (
                      <span className="family-dashboard-you-text">{t("CORE_COMMON_YOU")}</span>
                    ) : (
                      <>
                      {relationship}
                      {isHead && <span className="family-dashboard-you-text">{t("CORE_COMMON_YOU_HEAD")}</span>}
                      </>
                    )}
                  </span>
                </div>
                {isVerified && (
                  <div className="family-dashboard-verified-badge">
                    {t("PROFILE_VERIFIED")}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </Card>
      {toast && (
        <Toast error={toast.error}  label={toast.label} onClose={() => setToast(null)} />
      )}

      {/* Aadhaar Verification Modal to add family member */}
      {showAddMemberModal && (
        <AadhaarVerificationModal
          isOpen={showAddMemberModal}
          isModal={true}
          mode="family"
          onClose={() => setShowAddMemberModal(false)}
          onSuccess={({ verifiedMember }) => {
            setShowAddMemberModal(false);
            navigate(`${PROFILE_ROUTES.PROFILE_CREATE_FAMILY}/profileDetails?member=${verifiedMember?.userUuid}`);
          }}
        />
      )}
    </React.Fragment>
  );
};

export default FamilyDashboard;