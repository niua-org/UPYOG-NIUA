import React, { useState, useEffect, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { Loader, Card } from "@nudmcdgnpm/digit-ui-react-components";
import EditProfileForm from "./EditProfileForm";
import EditAddress from "./EditAddress";
import EditDocuments from "./EditDocuments";

/**
 * EditProfile - Parent Component for edit workflows (Profile, Address, Documents).
 *
 * UPYOG Standard Enhancements:
 * - Uses `routeState?.user` for instant zero-flicker initial render.
 * - Runs a live background fetch via `userSearch` (with ADDRESS, PROFILE, DOCUMENT)
 *   ensuring fresh data is always present even on page refresh (F5) or direct navigation.
 * - Synchronizes Address, Document, and Profile updates into a single source of truth.
 */
const EditProfile = ({ stateCode, userType, cityDetails }) => {
  const { t } = useTranslation();
  const { state: routeState } = useLocation();
  const isEmployee = userType === "employee";
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const userInfo = Digit.UserService.getUser()?.info || {};
  const userDetails = routeState?.user;
  const [currentUser, setCurrentUser] = useState(userDetails || null);
  const [addresses, setAddresses] = useState(userDetails?.addresses || []);
  const [loading, setLoading] = useState(!userDetails);
  const [activeTab, setActiveTab] = useState("profile");

  // TABS config — in future if want to add new tab, just add here
  const TABS = [
    { key: "profile", label: "CORE_PROFILE" },
    { key: "address", label: "CORE_ADDRESS" },
    { key: "documents", label: "CORE_DOCUMENTS" },
  ];

  /**
   * Fetches fresh user data from /user/profile/_search with all sub-entities.
   * Runs on mount and after critical updates.
   */
  const fetchUserData = useCallback(async (showLoader = false) => {
    if (!userInfo?.uuid) return;
    if (showLoader) setLoading(true);

    try {
      const res = await Digit.UserService.userSearch(
        tenantId,
        { uuid: userInfo.uuid, include: ["ADDRESS", "PROFILE", "DOCUMENT"] },
        {}
      );
      const latestUser = res?.user?.[0];
      if (latestUser) {
        setCurrentUser(latestUser);
        setAddresses(latestUser.addresses || []);
      }
    } catch (err) {
      console.error("Error fetching live profile data in EditProfile:", err);
    } finally {
      if (showLoader) setLoading(false);
    }
  }, [userInfo?.uuid, tenantId]);

  // Fetch on mount (silent if userDetails already exists, loader if direct URL/refresh)
  useEffect(() => {
    fetchUserData(!userDetails);
  }, [fetchUserData]);

  // Callback to update state after personal profile update
  const handleProfileUpdate = (updatedUserData) => {
    setCurrentUser((prev) => ({
      ...prev,
      ...updatedUserData,
    }));
  };

  // Callback when an address is added or updated in EditAddress
  const handleAddressUpdate = (updatedAddresses) => {
    setAddresses(updatedAddresses);
    setCurrentUser((prev) => ({
      ...prev,
      addresses: updatedAddresses,
    }));
  };

  if (loading) return <Loader />;

  return (
    <div>
      <Card>
        <div className="profile-header">
          <div className="profile-header-left">
            <div className="profile-header-icon">👤</div>
            <h2 className="profile-header-title">{t("CORE_COMMON_PROFILE_EDIT_DETAILS")}</h2>
          </div>
        </div>

        {!isEmployee && (
          <div className="profile-tabs">
            {TABS.map(({ key, label }) => (
              <button
                key={key}
                type="button"
                onClick={() => setActiveTab(key)}
                className={`profile-tab ${activeTab === key ? "profile-tab-active" : ""}`}
              >
                {t(label)}
              </button>
            ))}
          </div>
        )}

        {activeTab === "profile" && <EditProfileForm stateCode={stateCode} userType={userType} cityDetails={cityDetails} userDetails={currentUser} onProfileUpdate={handleProfileUpdate} />}
        {activeTab === "address" && <EditAddress initialAddresses={addresses} />}
        {activeTab === "documents" && <EditDocuments userDetails={currentUser} onDocumentsUpdate={handleProfileUpdate} />}
      </Card>
    </div>
  );
};

export default EditProfile;
