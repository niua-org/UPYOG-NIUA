import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Card, Loader, EditIcon } from "@nudmcdgnpm/digit-ui-react-components";
import { ADDRESS_TYPE_LABEL, getPersonalFields, getAddressBlocks, ProfilePhoto, FieldGrid, Section } from "../utils";
import { profileStyles } from "../utils/profileStyles";

const EDIT_PROFILE_URL = "/upyog-ui/citizen/profile/edit-details";


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
      if (!uuid) { setLoading(false); return; }
      try {
        const response = await Digit.UserService.userSearch(tenantId, { uuid: [uuid] }, {});
        if (isActive) setUser(response?.user?.[0] || null);
      } catch (e) {
        if (isActive) setError(true);
      } finally {
        if (isActive) setLoading(false);
      }
    };
    fetchUser();
    return () => { isActive = false; };
  }, [uuid, tenantId]);

  return { user, loading, error };
};

const ProfileDetails = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const { user, loading, error } = useProfileUser();
  const [profilePhoto, setProfilePhoto] = useState(null);
  console.log("userrr",user)

  useEffect(() => {
    const fetchPhoto = async () => {
      if (user?.photo) {
        try {
          const file = await Digit.UploadServices.Filefetch([user.photo], "pg");
          if (file?.data?.fileStoreIds?.[0]?.url) {
            setProfilePhoto(file?.data?.fileStoreIds?.[0]?.url.split(",")[0]);
          }
        } catch (err) {
          console.error("Error fetching profile photo:", err);
        }
      }
    };

    fetchPhoto();
  }, [user?.photo, tenantId]);

  if (loading) return <Loader />;
  if (error || !user) return <div style={profileStyles.message}>{t("CORE_COMMON_PROFILE_LOAD_ERROR")}</div>;

  // Personal & address data, filtered to only show fields with values
  const personalFields = getPersonalFields(user);
  const addressBlocks = getAddressBlocks(user.addresses);

  return (
    <Card>
      {/* Header: icon + title, edit button */}
      <div style={profileStyles.header}>
        <div style={profileStyles.headerLeft}>
          <div style={profileStyles.headerIcon}>👤</div>
          <h2 style={profileStyles.title}>{t("CORE_COMMON_PROFILE_DETAILS")}</h2>
        </div>
        <button type="button" onClick={() => navigate(EDIT_PROFILE_URL, { state: { user } })} style={profileStyles.editButton}>
          <EditIcon /> {t("CORE_COMMON_EDIT")}
        </button>
      </div>

      {/* Photo + personal fields */}
      <Section title={t("CORE_COMMON_PROFILE_PERSONAL_DETAILS")}>
        <div style={profileStyles.card}>
          <ProfilePhoto photo={profilePhoto} name={user.name} />
          <FieldGrid fields={personalFields} t={t} />
        </div>
      </Section>

      {/* One card per address type, only if there is address data */}
      {addressBlocks.length > 0 && (
        <Section title={t("CORE_COMMON_PROFILE_ADDRESS_DETAILS")}>
          {addressBlocks.map((block, index) => (
            <div key={`${block.type}-${index}`} style={profileStyles.card}>
              <div style={profileStyles.cardLabel}>
                {t(ADDRESS_TYPE_LABEL[block.type] || "CORE_COMMON_ADDRESS")}
              </div>
              <FieldGrid fields={block.fields} t={t} />
            </div>
          ))}
        </Section>
      )}
    </Card>
  );
};

export default ProfileDetails;