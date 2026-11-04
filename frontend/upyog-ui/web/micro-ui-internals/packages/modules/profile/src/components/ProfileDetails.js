import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Card, Loader, EditIcon, Dropdown, ArrowDown } from "@nudmcdgnpm/digit-ui-react-components";
import { ADDRESS_TYPE_LABEL, getPersonalFields, getAddressBlocks, ProfilePhoto, FieldGrid, Section, PDFSvg, getDocumentBlocks, openDocument } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import {PROFILE_ROUTES} from "../utils/constants";

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
        const response = await Digit.UserService.userSearch(tenantId, { uuid: [uuid], include: ["ADDRESS", "PROFILE", "DOCUMENT"] }, {});
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
  const navigate = Digit.Hooks.useCustomNavigate();
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const stateId = Digit.ULBService.getStateId();
  const { user, loading, error } = useProfileUser();
  const [profilePhoto, setProfilePhoto] = useState(null);

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

  // hook to fetch family creation options from MDMS, filtering for active options
  const { data: FamilyCreateOptions } = Digit.Hooks.useEnabledMDMS(stateId, "FamilyCreateType", [{ name: "FamilyOptions" }],
    {
      select: (data) => {
        const formattedData = data?.["FamilyCreateType"]?.["FamilyOptions"]
        const activeData = formattedData?.filter((option) => option.active);
        return activeData;
      },
  });
  let familyOptions = [];
  FamilyCreateOptions && FamilyCreateOptions.map((family) => {familyOptions.push({ i18nKey: `${family.code}`, code: `${family.code}`, name: `${family.name}` })});

  if (loading) return <Loader />;
  if (error || !user) return <div style={profileStyles.message}>{t("CORE_COMMON_PROFILE_LOAD_ERROR")}</div>;


  // Personal & address data, filtered to only show fields with values
  const personalFields = getPersonalFields(user);
  const addressBlocks = getAddressBlocks(user.addresses);
  const documentBlocks = getDocumentBlocks(user.documents)

  /**
   * Handle family dropdown action selection
   * Navigates to the corresponding route based on the selected MDMS code
   */
  const handleFamilySelection = (option) => {
    const targetRoute = PROFILE_ROUTES[option?.code];
    if (targetRoute) {
      navigate(targetRoute);
    }
  };

  const handleVerifyProfile = () => {
    // Navigate to the profile verification page
    navigate(PROFILE_ROUTES.VERIFY);
  };

  return (
    <Card>
      {/* Header: icon + title, edit button */}
      <div style={profileStyles.header}>
        <div style={profileStyles.headerLeft}>
          <div style={profileStyles.headerIcon}>👤</div>
          <h2 style={profileStyles.title}>{t("CORE_COMMON_PROFILE_DETAILS")}</h2>
        </div>
        <div style={profileStyles.headerActions} >
          {user?.isIdentityVerified ? (
          <div>
          <Dropdown
            option={familyOptions}
            optionKey="name"
            select={handleFamilySelection}
            style={profileStyles.dropdownWrap}
            optionCardStyles={profileStyles.dropdownMenu}
            customSelector={
              <div style={profileStyles.dropdownButton}>
                <span>{t("PROFILE_ADD_FAMILY")}</span>
                <ArrowDown/>
              </div>
            }
          />
          </div>
         )
          : 
          (
          <button type="button" onClick={handleVerifyProfile} style={profileStyles.editButton}>{t("VERIFY_PROFILE")}</button> 
          )}
          <button type="button" onClick={() => navigate(PROFILE_ROUTES.EDIT_PROFILE_URL, { state: { user } })} style={profileStyles.editButton}>
            <EditIcon /> {t("CORE_COMMON_EDIT")}
          </button>
        </div>
      </div>

      {/* Photo + personal fields */}
      <Section title={t("CORE_COMMON_PROFILE_PERSONAL_DETAILS")}>
        <div style={profileStyles.card}>
          <ProfilePhoto photo={profilePhoto} name={user.name} />
          <FieldGrid fields={personalFields} t={t} tenantId={tenantId} />
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
              <FieldGrid fields={block.fields} t={t} tenantId={tenantId} />
            </div>
          ))}
        </Section>
      )}
      {/* Document Details Section */}
      {documentBlocks.length > 0 && (
        <Section title={t("CORE_COMMON_PROFILE_DOCUMENTS")}>
          <div style={profileStyles.card}>
            <div style={profileStyles.documentGrid}>
              {documentBlocks.map((doc, index) => (
                <div
                  key={doc.id || index}
                  style={{ ...profileStyles.documentCard, cursor: "pointer" }}
                  onClick={() => openDocument(doc.filestoreId, stateId)}
                  title="Click to view document"
                >
                  <PDFSvg width={40} height={40} />
                  <div style={{ flex: 1 }}>
                    <div style={profileStyles.documentTitle}>
                      {t(doc.documentType)}
                    </div>
                    <span style={profileStyles.documentLink}>
                      {t("PROFILE_VIEW_DOC")}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Section>
      )}
    </Card>
  );
};

export default ProfileDetails;