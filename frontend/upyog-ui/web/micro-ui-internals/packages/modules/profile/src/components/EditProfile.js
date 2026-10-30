import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { Loader, Card } from "@nudmcdgnpm/digit-ui-react-components";
import EditProfileForm from "./EditProfileForm";
import EditAddress from "./EditAddress";
import EditDocuments from "./EditDocuments";
import { profileStyles } from "../utils/profileStyles";

/**
 * 
 * Edit Profile - Parent Component for edit purpose
 *
 * Renders the page header and tab switcher (Profile / Address).
 * On direct navigation (no routeState), fetches the user's addresses as a fallback.
 * Delegates all profile form logic to EditProfileForm and all address logic to EditAddress.
 * For employees, the tab switcher is hidden — only the profile form is shown.
 * 
 * Component is following Single Responsibility Principal
 */


const EditProfile = ({ stateCode, userType, cityDetails }) => {
  const { t } = useTranslation();
  const { state: routeState } = useLocation();
  const isEmployee = userType === "employee";
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const userInfo = Digit.UserService.getUser()?.info || {};
  const userDetails = routeState?.user;
  const [addresses, setAddresses] = useState(userDetails?.addresses || []);
  const [loading, setLoading] = useState(!userDetails);
  const [activeTab, setActiveTab] = useState("profile");

  // TABS config — in future if want to add new tab, just add here
  const TABS = [
    { key: "profile",  label: "CORE_PROFILE"  },
    { key: "address",  label: "CORE_ADDRESS"  },
    { key: "documents",  label: "CORE_DOCUMENTS" },
  ];

  // Fallback fetch only when navigated directly (no routeState)
  useEffect(() => {
    if (userDetails) return;
    let isActive = true;
    const loadProfileData = async () => {
      try {
        const res = await Digit.UserService.userSearchNewV2(tenantId, { uuid: [userInfo.uuid] }, {});
        if (isActive) setAddresses(res?.user?.[0]?.addresses || []);
      } finally {
        if (isActive) setLoading(false);
      }
    };
    loadProfileData();
    return () => { isActive = false; };
  }, []);

  if (loading) return <Loader />;

  return (
    <div>
      <Card>
        <div style={profileStyles.header}>
          <div style={profileStyles.headerLeft}>
            <div style={profileStyles.headerIcon}>👤</div>
            <h2 style={profileStyles.title}>{t("CORE_COMMON_PROFILE_DETAILS")}</h2>
          </div>
        </div>

        {!isEmployee && (
            <div style={profileStyles.tabs}>
              {TABS.map(({ key, label }) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => setActiveTab(key)}
                  style={{ ...profileStyles.tab, ...(activeTab === key ? profileStyles.tabActive : {}) }}
                >
                  {t(label)}
                </button>
              ))}
            </div>
          )}

        {activeTab === "profile" && <EditProfileForm stateCode={stateCode} userType={userType} cityDetails={cityDetails} userDetails={userDetails} />}
        {activeTab === "address" && <EditAddress initialAddresses={addresses} />}
        {activeTab === "documents" && <EditDocuments stateCode={stateCode} userDetails={userDetails} />}
      </Card>
    </div>
  );
};

export default EditProfile;
