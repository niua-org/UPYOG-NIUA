import React, { useState, useEffect, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { Card, Loader, EditIcon, TickMark, CloseSvg, Toast, AddIcon, PersonIcon } from "@nudmcdgnpm/digit-ui-react-components";
import { ADDRESS_TYPE_LABEL, getPersonalFields, getAddressBlocks, ProfilePhoto, FieldGrid, Section, PDFSvg, getDocumentBlocks, openDocument, clearFamilyMembers } from "../utils";
import { PROFILE_ROUTES } from "../utils/constants";
import AadhaarVerificationModal from "../verifications/AadhaarVerificationModal";

/**
 * Custom hook to fetch and manage the logged-in citizen's profile details.
 * Fetches user profile, addresses, and documents from userSearch API with unmount
 * and race-condition guards.
 *
 * @returns {{
 *   user: Object|null,
 *   loading: boolean,
 *   error: boolean,
 *   refetch: Function
 * }} Profile user state and actions
 */
const useProfileUser = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const uuid = Digit.UserService.getUser()?.info?.uuid;
  const tenantId =  Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();

  const isMountedRef = useRef(true);
  const requestIdRef = useRef(0);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const fetchUser = useCallback(async () => {
    if (!uuid) {
      if (isMountedRef.current) setLoading(false);
      return;
    }

    const currentRequestId = ++requestIdRef.current;
    if (isMountedRef.current) setLoading(true);

    try {
      // The updated /user/profile/_search endpoint expects a single uuid string rather than a list of UUIDs
      const response = await Digit.UserService.userSearch(
        tenantId,
        { uuid: uuid, include: ["ADDRESS", "PROFILE", "DOCUMENT"] },
        {}
      );
      if (isMountedRef.current && currentRequestId === requestIdRef.current) {
        setUser(response?.user?.[0] || null);
        setError(false);
      }
    } catch (e) {
      if (isMountedRef.current && currentRequestId === requestIdRef.current) {
        setError(true);
      }
    } finally {
      if (isMountedRef.current && currentRequestId === requestIdRef.current) {
        setLoading(false);
      }
    }
  }, [uuid, tenantId]);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  return { user, loading, error, refetch: fetchUser };
};

/**
 * ProfileDetails Component
 *
 * Renders the citizen profile details view including personal info, verified status badges,
 * family management actions (Create Family, Join Existing Family, Verify Profile),
 * address details, and uploaded documents.
 *
 * @returns {JSX.Element} Rendered profile details screen
 */
const ProfileDetails = () => {
  const navigate = Digit.Hooks.useCustomNavigate();
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const stateId = Digit.ULBService.getStateId();
  const { user, loading, error, refetch } = useProfileUser();
  const [profilePhoto, setProfilePhoto] = useState(null);
  const [toast, setToast] = useState(null);
  const [isHof, setIsHof] = useState(false);
  const [showVerifyModal, setShowVerifyModal] = useState(false);

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
  if (error || !user) return <div className="profile-message">{t("CORE_COMMON_PROFILE_LOAD_ERROR")}</div>;


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
      setShowVerifyModal(true);
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

  const getFamilyOptionIcon = (code) => {
    switch (code) {
      case "PROFILE_CREATE_FAMILY":
        return <AddIcon fill="#a82227" className="profile-action-icon-add" />;
      case "PROFILE_JOIN_EXISTING_FAMILY":
      case "PROFILE_FAMILY_DASHBOARD":
        return <PersonIcon className="profile-action-icon-person" />;
      default:
        return null;
    }
  };

  return (
    <React.Fragment>
      <Card>
      {/* Header: icon + title, edit button */}
      <div className="profile-header">
        <div className="profile-header-left">
          <div className="profile-header-icon">👤</div>
          <h2 className="profile-header-title">{t("CORE_COMMON_PROFILE_DETAILS")}</h2>
          {user?.isIdentityVerified ? (
            <span
              title={t("AADHAAR_VERIFIED")}
              className="profile-verified-badge"
            >
              <span className="profile-verified-icon-wrap">
                <TickMark fillColor="white" />
              </span>
              <span>{t("PROFILE_AADHAAR_VERIFIED")}</span>
            </span>
          ) : (
            <span
              role="button"
              tabIndex={0}
              onClick={() => setShowVerifyModal(true)}
              title={t("CLICK_TO_VERIFY_AADHAAR")}
              className="profile-unverified-badge"
            >
              <span className="profile-unverified-icon-wrap">
                <span className="profile-close-svg-wrap">
                  <CloseSvg />
                </span>
              </span>
              <span className="profile-unverified-text">
                {t("PROFILE_AADHAAR_NOT_VERIFIED")}
              </span>
            </span>
          )}
        </div>
        <div className="profile-details-header-actions">
          {user?.familyId && (
            <div className="profile-family-id-badge">
              <span className="profile-family-id-label">
                {t("CORE_COMMON_FAMILY_ID", "Family ID")}:
              </span>
              <span className="profile-family-id-value">
                {user.familyId}
              </span>
              {isHof && (
                <span className="profile-hof-badge">
                  {t("CORE_COMMON_HOF", "HOF")}
                </span>
              )}
            </div>
          )}
          {/* Show family action options as top-level buttons */}
          {familyOptions.map((option) => (
            <button
              key={option.code}
              type="button"
              onClick={() => handleFamilySelection(option)}
              className="profile-action-btn"
            >
              {getFamilyOptionIcon(option.code)}
              <span>{t(option.i18nKey, option.name)}</span>
            </button>
          ))}
          <button type="button" onClick={() => navigate(PROFILE_ROUTES.EDIT_PROFILE_URL, { state: { user } })} className="profile-edit-btn">
            <EditIcon /> {t("CORE_COMMON_EDIT")}
          </button>
        </div>
      </div>

      {/* Photo + personal fields */}
      <Section title={t("CORE_COMMON_PROFILE_PERSONAL_DETAILS")}>
        <div className="profile-card">
          <ProfilePhoto photo={profilePhoto} name={user.name} />
          <FieldGrid fields={personalFields} t={t} tenantId={tenantId} />
        </div>
      </Section>

      {/* One card per address type, only if there is address data */}
      {addressBlocks.length > 0 && (
        <Section title={t("CORE_COMMON_PROFILE_ADDRESS_DETAILS")}>
          {addressBlocks.map((block, index) => (
            <div key={`${block.type}-${index}`} className="profile-card">
              <div className="profile-card-label">
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
          <div className="profile-card">
            <div className="profile-details-document-grid">
              {documentBlocks.map((doc, index) => (
                <div
                  key={doc.id || index}
                  className="profile-details-document-card"
                  onClick={() => openDocument(doc.filestoreId, stateId)}
                  title="Click to view document"
                >
                  <PDFSvg width={40} height={40} />
                  <div className="profile-details-document-content">
                    <div className="profile-details-document-title">
                      {t(doc.documentType)}
                    </div>
                    <span className="profile-details-document-link">
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

    {/* Aadhaar Verification Modal */}
    {showVerifyModal && (
      <AadhaarVerificationModal
        isModal={true}
        isOpen={showVerifyModal}
        mode="profile"
        onClose={() => setShowVerifyModal(false)}
        onSuccess={() => {
          setShowVerifyModal(false);
          window.location.reload();
        }}
      />
    )}
  </React.Fragment>
  );
};

export default ProfileDetails;