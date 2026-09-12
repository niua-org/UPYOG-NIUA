import React, { useState } from "react";
import {
  Card,
  CardHeader,
  CardSectionHeader,
  CardText,
  CheckBox,
  EditIcon,
  SubmitBar,
  Toast,
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { checkForNA, multiUnits } from "../../../utils";
import "../../../css/gc-inline-auto.css";

const ActionButton = ({ jumpTo }) => {
  const navigate = Digit.Hooks.useCustomNavigate();
  const { pathname } = useLocation();

  const routeTo = () => {
    const wizardPath = pathname.replace(/\/check(?:\/.*)?$/, "");
    navigate(`${wizardPath}/${jumpTo}`);
  };

  return (
    <button
      type="button"
      onClick={routeTo}
      className="gc-edit-step-btn"
      title="Edit section"
    >
      <EditIcon />
    </button>
  );
};

/**
 * GCCheckPage Component
 * 
 * Renders the final review/summary step before submitting the GC application.
 * Displays all collected data organized by section in structured responsive grid cards:
 * Applicant Details, Property Location, Garbage Specifications, and Documents.
 */
const GCCheckPage = ({ onSubmit, value = {}, renewApplication }) => {
  const { t } = useTranslation();
  const [agree, setAgree] = useState(false);

  const location = value?.gcpropertylocdetails || {};
  const specifications = value?.gcspecifications || {};
  const specialCategory = value?.gcspecialcategory?.specialCategory || value?.specialCategory || "";
  const documents = value?.gcdocuments?.documents || value?.documents || [];
  const isInheritance = value?.owner?.isInheritance || value?.gcspecifications?.isInheritance || false;

  // Extract owners robustly
  let owners = [];
  if (Array.isArray(value?.owner)) {
    owners = value.owner;
  } else if (Array.isArray(value?.owner?.owner)) {
    owners = value.owner.owner;
  } else if (value?.owner && typeof value.owner === "object") {
    if (value.owner.name || value.owner.mobileNumber) {
      owners = [value.owner];
    } else {
      owners = Object.values(value.owner).filter((o) => o?.name || o?.mobileNumber);
    }
  }

  const setDeclarationHandler = () => {
    setAgree(!agree);
  };

  const GCDocuments = Digit?.ComponentRegistryService?.getComponent("GCDocuments");

  return (
    <div style={{ maxWidth: "960px", marginLeft: 0, marginRight: "auto" }}>
      <Card className="gc-details-card">
        <CardHeader styles={{ fontSize: "28px", margin: 0, marginBottom: "8px" }}>
          {t("GC_SUMMARY")}
        </CardHeader>
        <CardText style={{ color: "#64748b", marginBottom: "20px" }}>
          {t("GC_CHECK_YOUR_DETAILS")}
        </CardText>

        {/* Section 1: Applicant Details */}
        {owners && owners.length > 0 && (
          <div className="gc-details-section">
            <div className="flex items-center justify-between mb-4">
              <div className="gc-section-title" style={{ margin: 0 }}>
                <span className="gc-section-title-icon">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </span>
                <span>{t("ES_APPLICANT_DETAILS")}</span>
              </div>
              <ActionButton jumpTo="applicant-details" />
            </div>

            {owners.map((owner, index) => (
              <div key={`owner-${index}`} className="gc-details-grid mb-3">
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_APPLICANT_NAME")}</span>
                  <span className="gc-detail-value">{owner?.applicantName || owner?.name || t("CS_NA")}</span>
                </div>
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_MOBILE_NUMBER")}</span>
                  <span className="gc-detail-value">{owner?.mobileNumber || t("CS_NA")}</span>
                </div>
                {owner?.alternateNumber && (
                  <div className="gc-detail-item">
                    <span className="gc-detail-label">{t("GC_ALT_MOBILE_NUMBER")}</span>
                    <span className="gc-detail-value">{owner.alternateNumber}</span>
                  </div>
                )}
                {(owner?.emailId || owner?.email) && (
                  <div className="gc-detail-item">
                    <span className="gc-detail-label">{t("GC_EMAIL")}</span>
                    <span className="gc-detail-value">{owner?.emailId || owner?.email}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Section 2: Property Location Details */}
        <div className="gc-details-section">
          <div className="flex items-center justify-between mb-4">
            <div className="gc-section-title" style={{ margin: 0 }}>
              <span className="gc-section-title-icon">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </span>
              <span>{t("GC_PROPERTY_LOCATION_DETAILS")}</span>
            </div>
            <ActionButton jumpTo="garbage-propertyLocation" />
          </div>

          <div className="gc-details-grid">
            {location?.propertyId && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_PROPERTY_ID")}</span>
                <span className="gc-detail-value">{location.propertyId}</span>
              </div>
            )}
            {location?.houseNo && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_HOUSE_NO")}</span>
                <span className="gc-detail-value">{location.houseNo}</span>
              </div>
            )}
            {location?.houseName && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_HOUSE_NAME")}</span>
                <span className="gc-detail-value">{location.houseName}</span>
              </div>
            )}
            {location?.streetName && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_STREET_NAME")}</span>
                <span className="gc-detail-value">{location.streetName}</span>
              </div>
            )}
            {location?.addressline1 && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_ADDRESS_LINE1")}</span>
                <span className="gc-detail-value">{location.addressline1}</span>
              </div>
            )}
            {location?.addressline2 && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_ADDRESS_LINE2")}</span>
                <span className="gc-detail-value">{location.addressline2}</span>
              </div>
            )}
            {location?.landmark && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_LANDMARK")}</span>
                <span className="gc-detail-value">{location.landmark}</span>
              </div>
            )}
            {location?.city && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_CITY")}</span>
                <span className="gc-detail-value">
                  {typeof location.city === "object" ? t(location.city.i18nKey || location.city.code) : t(location.city)}
                </span>
              </div>
            )}
            {location?.locality && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_LOCALITY")}</span>
                <span className="gc-detail-value">
                  {typeof location.locality === "object" ? t(location.locality.i18nKey || location.locality.code) : t(location.locality)}
                </span>
              </div>
            )}
            {location?.pincode && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_ADDRESS_PINCODE")}</span>
                <span className="gc-detail-value">{location.pincode}</span>
              </div>
            )}
          </div>
        </div>

        {/* Section 3: Garbage Specifications */}
        <div className="gc-details-section">
          <div className="flex items-center justify-between mb-4">
            <div className="gc-section-title" style={{ margin: 0 }}>
              <span className="gc-section-title-icon">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </span>
              <span>{t("GC_GARBAGE_SPECIFICATIONS")}</span>
            </div>
            <ActionButton jumpTo="garbage-specifications" />
          </div>

          <div className="gc-details-grid">
            {specifications?.oldGarbageId && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_OLD_GARBAGE_ID")}</span>
                <span className="gc-detail-value">{specifications.oldGarbageId}</span>
              </div>
            )}
            {specifications?.typeOfCollection && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_TYPE_OF_COLLECTION")}</span>
                <span className="gc-detail-value">
                  {typeof specifications.typeOfCollection === "object" ? t(specifications.typeOfCollection.i18nKey || specifications.typeOfCollection.code) : t(specifications.typeOfCollection)}
                </span>
              </div>
            )}
            {specifications?.propertyOwnerType && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_OWNER_OR_TENANT")}</span>
                <span className="gc-detail-value">
                  {typeof specifications.propertyOwnerType === "object" ? t(specifications.propertyOwnerType.i18nKey || specifications.propertyOwnerType.code) : t(specifications.propertyOwnerType)}
                </span>
              </div>
            )}
            {specifications?.name && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_NAME")}</span>
                <span className="gc-detail-value">{specifications.name}</span>
              </div>
            )}
            {specifications?.phoneNumber && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_PHONE_NUMBER")}</span>
                <span className="gc-detail-value">{specifications.phoneNumber}</span>
              </div>
            )}
            {specifications?.gender && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_GENDER")}</span>
                <span className="gc-detail-value">
                  {typeof specifications.gender === "object" ? t(specifications.gender.i18nKey || specifications.gender.code) : t(specifications.gender)}
                </span>
              </div>
            )}
            {specifications?.email && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_EMAIL")}</span>
                <span className="gc-detail-value">{specifications.email}</span>
              </div>
            )}
            {specifications?.category && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_CATEGORY")}</span>
                <span className="gc-detail-value">
                  {typeof specifications.category === "object" ? t(specifications.category.i18nKey || specifications.category.code) : t(specifications.category)}
                </span>
              </div>
            )}
            {specifications?.subCategory && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_SUB_CATEGORY")}</span>
                <span className="gc-detail-value">
                  {typeof specifications.subCategory === "object" ? t(specifications.subCategory.i18nKey || specifications.subCategory.code) : t(specifications.subCategory)}
                </span>
              </div>
            )}
            {specifications?.subCategoryType && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_SUB_CATEGORY_TYPE")}</span>
                <span className="gc-detail-value">
                  {typeof specifications.subCategoryType === "object" ? t(specifications.subCategoryType.i18nKey || specifications.subCategoryType.code) : t(specifications.subCategoryType)}
                </span>
              </div>
            )}
            {multiUnits.includes(specifications?.typeOfCollection?.code || specifications?.typeOfCollection) && specifications?.no_of_units && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_NO_OF_UNITS")}</span>
                <span className="gc-detail-value">{specifications.no_of_units}</span>
              </div>
            )}
            <div className="gc-detail-item">
              <span className="gc-detail-label">{t("GC_IS_INHERITANCE")}</span>
              <span className="gc-detail-value">{isInheritance ? t("YES") : t("NO")}</span>
            </div>
            {specialCategory && (
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_SPECIAL_CATEGORY")}</span>
                <span className="gc-detail-value">
                  {typeof specialCategory === "object" ? t(specialCategory.i18nKey || specialCategory.code) : t(specialCategory)}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Section 4: Garbage Documents */}
        {documents.length > 0 && (
          <div className="gc-details-section">
            <div className="flex items-center justify-between mb-4">
              <div className="gc-section-title" style={{ margin: 0 }}>
                <span className="gc-section-title-icon">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
                  </svg>
                </span>
                <span>{t("GC_GARBAGE_DOCUMENTS")}</span>
              </div>
              <ActionButton jumpTo="garbage-documents" />
            </div>

            <div className="chb-doc-card">
              {documents.map((doc, index) => (
                <div key={`doc-${index}`} className="chb-doc-item">
                  <div>
                    <CardSectionHeader>{t("GC_" + (doc?.documentType?.split(".").slice(0, 2).join("_")))}</CardSectionHeader>
                    <GCDocuments value={documents} Code={doc?.documentType} index={index} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Declaration & Submit */}
        <div className="mt-6 pt-4 border-t border-gray-100">
          <CheckBox
            label={t("GC_FINAL_DECLARATION_MESSAGE")}
            onChange={setDeclarationHandler}
            styles={{ height: "auto", marginBottom: "20px" }}
          />

          <SubmitBar
            label={t("GC_COMMON_BUTTON_SUBMIT")}
            onSubmit={onSubmit}
            disabled={!agree}
          />
        </div>
      </Card>
    </div>
  );
};

export default GCCheckPage;

