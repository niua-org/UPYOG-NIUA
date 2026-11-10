import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Card, Loader, EditIcon, Dropdown, ArrowDown, TickMark, CloseSvg, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import { ADDRESS_TYPE_LABEL, getPersonalFields, getAddressBlocks, ProfilePhoto, FieldGrid, Section, PDFSvg, getDocumentBlocks, openDocument, clearFamilyMembers } from "../utils";
import { profileStyles } from "../utils/profileStyles";
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
      if (!uuid) { setLoading(false); return; }
      try {
        // The updated /user/profile/_search endpoint expects a single uuid string rather than a list of UUIDs
        const response = await Digit.UserService.userSearch(tenantId, { uuid: uuid, include: ["ADDRESS", "PROFILE", "DOCUMENT"] }, {});
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
  const [toast, setToast] = useState(null);
  const [isHof, setIsHof] = useState(false);

  useEffect(() => {
    let isActive = true;
    const checkHofStatus = async () => {
      if (user?.familyId && tenantId) {
        try {
          const res = await Digit.UserService.isHof(
            { familyId: user.familyId, tenantId },
            { tenantId }
          );
          if (isActive && res?.isHof) {
            setIsHof(true);
          }
        } catch (err) {
          console.error("Error checking HOF status:", err);
        }
      }
    };

    checkHofStatus();
    return () => {
      isActive = false;
    };
  }, [user?.familyId, tenantId]);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(timer);
  }, [toast]);

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
  FamilyCreateOptions &&
    FamilyCreateOptions.forEach((family) => {
      if (user?.familyId) {
        // If the citizen already has a familyId, hide Create Family and Join Existing Family
        if (family.code === "PROFILE_CREATE_FAMILY" || family.code === "PROFILE_JOIN_EXISTING_FAMILY") {
          return;
        }
      } else {
        // If the citizen does not have a familyId, hide Family Dashboard
        if (family.code === "PROFILE_FAMILY_DASHBOARD") {
          return;
        }
      }
      familyOptions.push({ i18nKey: `${family.code}`, code: `${family.code}`, name: `${family.name}` });
    });



  if (loading) return <Loader />;
  if (error || !user) return <div style={profileStyles.message}>{t("CORE_COMMON_PROFILE_LOAD_ERROR")}</div>;


  // Personal & address data, filtered to only show fields with values
  const personalFields = getPersonalFields(user);
  const addressBlocks = getAddressBlocks(user.addresses);
  const documentBlocks = getDocumentBlocks(user.documents)

  /**
   * Handle family dropdown action selection.
   * If citizen is not Aadhaar verified, displays an error toast notification and blocks navigation.
   * If verified, navigates to the corresponding route based on selected MDMS code.
   */
  const handleFamilySelection = (option) => {
    // Intercept if citizen is not Aadhaar verified
    if (!user?.isIdentityVerified) {
      setToast({
        error: true,
        message: t("ERR_PROFILE_NOT_AADHAAR_VERIFIED"),
      });
      return;
    }

    const targetRoute = PROFILE_ROUTES[option?.code];
    if (targetRoute) {
      if (option?.code === "PROFILE_CREATE_FAMILY") {
        clearFamilyMembers();
      }
      navigate(targetRoute);
    }
  };

  return (
    <React.Fragment>
      <Card>
      {/* Header: icon + title, edit button */}
      <div style={profileStyles.header}>
        <div style={profileStyles.headerLeft}>
          <div style={profileStyles.headerIcon}>👤</div>
          <h2 style={profileStyles.title}>{t("CORE_COMMON_PROFILE_DETAILS")}</h2>
          {user?.isIdentityVerified ? (
            <span
              title={t("AADHAAR_VERIFIED", "Aadhaar Verified")}
              style={profileStyles.verifiedBadge}
            >
              <span style={profileStyles.verifiedIconWrap}>
                <TickMark fillColor="white" />
              </span>
              <span>{t("PROFILE_AADHAAR_VERIFIED", "Aadhaar Verified")}</span>
            </span>
          ) : (
            <span
              role="button"
              tabIndex={0}
              onClick={() => navigate(PROFILE_ROUTES.VERIFY)}
              title={t("CLICK_TO_VERIFY_AADHAAR")}
              style={profileStyles.unverifiedBadge}
            >
              <span style={profileStyles.unverifiedIconWrap}>
                <span style={profileStyles.closeSvgWrap}>
                  <CloseSvg />
                </span>
              </span>
              <span style={profileStyles.unverifiedText}>
                {t("PROFILE_AADHAAR_NOT_VERIFIED", "Aadhaar Not Verified")}
              </span>
            </span>
          )}
        </div>
        <div style={profileStyles.headerActions} >
          {user?.familyId && (
            <div style={profileStyles.familyIdBadge}>
              <span style={profileStyles.familyIdLabel}>
                {t("CORE_COMMON_FAMILY_ID", "Family ID")}:
              </span>
              <span style={profileStyles.familyIdValue}>
                {user.familyId}
              </span>
              {isHof && (
                <span style={profileStyles.hofBadge}>
                  {t("CORE_COMMON_HOF", "HOF")}
                </span>
              )}
            </div>
          )}
          {/* Show the My Family dropdown dynamically based on available options */}
          {familyOptions.length > 0 && (
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
                    <ArrowDown />
                  </div>
                }
              />
            </div>
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
                  style={profileStyles.documentCard}
                  onClick={() => openDocument(doc.filestoreId, stateId)}
                  title="Click to view document"
                >
                  <PDFSvg width={40} height={40} />
                  <div style={profileStyles.documentContent}>
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

    {/* Floating Toast Notification */}
    {toast && (
      <div className="profile-toast-container">
        <Toast
          error={toast.error}
          label={toast.message}
          onClose={() => setToast(null)}
        />
      </div>
    )}
  </React.Fragment>
  );
};

export default ProfileDetails;