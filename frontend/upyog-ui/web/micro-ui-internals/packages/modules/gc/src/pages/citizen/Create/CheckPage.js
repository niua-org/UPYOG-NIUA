import React, { useState } from "react";
import {
  Card,
  CardHeader,
  CardSectionHeader,
  CardSubHeader,
  CardText,
  CheckBox,
  EditIcon,
  LinkButton,
  Row,
  StatusTable,
  SubmitBar,
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { multiUnits } from "../../../utils";

const ActionButton = ({ jumpTo }) => {
  const navigate = Digit.Hooks.useCustomNavigate();
  const { pathname } = useLocation();

  const routeTo = () => {
    const wizardPath = pathname.replace(/\/check(?:\/.*)?$/, "");
    navigate(`${wizardPath}/${jumpTo}`);
  };

  return (
    <LinkButton
      label={<EditIcon />}
      className="check-page-link-button"
      onClick={routeTo}
    />
  );
};

/**
 * GCCheckPage Component
 * 
 * Step 4 in the GC Application Wizard as defined in UPYOG Garbage Service HLD Section 2.4:
 * Displays comprehensive preview summary:
 * - Property & Address details (with Property ID and detected floor count)
 * - Owner details & specifications
 * - Owner uploaded documents
 * - All Tenant cards & specifications (if added)
 * - Tenant uploaded documents
 * - Declaration: "I hereby declare that all details and tenant mappings are accurate."
 * - Submit Button
 */
const GCCheckPage = ({ onSubmit, value = {}, renewApplication }) => {
  const { t } = useTranslation();
  const [agree, setAgree] = useState(false);
  const user = Digit.UserService.getUser()?.info || {};

  const location = value?.gcpropertylocdetails || {};
  const address = value?.address || location || {};
  const specifications = value?.gcspecifications || {};
  const gcdocuments = value?.gcdocuments || {};
  const ownerDocs = gcdocuments?.ownerDocuments || [];
  const tenantDocsMap = gcdocuments?.tenantDocuments || {};
  const allDocs = gcdocuments?.documents || value?.documents || [];

  const ownerForm = value?.owner || {};
  const ownerInfo = location?.ownerInfo || {};
  const ownerName = ownerForm?.applicantName || ownerInfo?.name || user?.name || t("CS_NA");
  const ownerMobile = ownerForm?.mobileNumber || ownerInfo?.mobileNumber || user?.mobileNumber || t("CS_NA");
  const ownerEmail = ownerForm?.emailId || ownerInfo?.emailId || user?.emailId || t("CS_NA");
  const ownerGender = (typeof ownerForm?.gender === "object" ? ownerForm?.gender?.code : ownerForm?.gender) || ownerInfo?.gender || "";
  const ownerGuardian = ownerForm?.guardianName || ownerInfo?.fatherOrHusbandName || "";
  const ownerRelationship = (typeof ownerForm?.relationShipType === "object" ? ownerForm?.relationShipType?.name || ownerForm?.relationShipType?.code : ownerForm?.relationShipType) || "";
  const ownerAltMobile = ownerForm?.alternateNumber || "";

  const tenants = specifications?.tenants || [];
  const GCDocuments = Digit?.ComponentRegistryService?.getComponent("GCDocuments");

  const getTranslatedValue = (val) => {
    if (!val) return t("CS_NA");
    if (typeof val === "object") {
      return val?.name || (val?.label && val?.label !== "Locality" ? val.label : null) || (val?.i18nKey ? t(val.i18nKey) : val?.code) || t("CS_NA");
    }
    return t(String(val));
  };

  return (
    <Card>
      <CardHeader>{t("COMMON_SUMMARY") || t("GC_SUMMARY") || "Application Review Summary"}</CardHeader>
      <CardText>{t("GC_CHECK_YOUR_DETAILS") || "Please verify the property, owner, tenant details and uploaded documents before submission."}</CardText>

      {/* 1. Property Location Details */}
      <div className="gc-check-page-flex-row" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "16px" }}>
        <CardSubHeader className="gc-check-page-header">
          {t("GC_PROPERTY_LOCATION_DETAILS") || "Property Location Details"}
        </CardSubHeader>
        <ActionButton jumpTo="garbage-propertyLocation" />
      </div>

      <StatusTable>
        <Row label={t("GC_PROPERTY_ID")} text={location?.propertyId || t("CS_NA")} />
        <Row label={t("GC_DETECTED_FLOORS") || "Detected Floors"} text={location?.detectedFloorCount ? String(location.detectedFloorCount) : "1"} />
        <Row label={t("GC_HOUSE_NO")} text={location?.houseNo || t("CS_NA")} />
        <Row label={t("GC_HOUSE_NAME")} text={location?.houseName || t("CS_NA")} />
        <Row label={t("GC_STREET_NAME")} text={location?.streetName || t("CS_NA")} />
        <Row label={t("GC_ADDRESS_LINE1")} text={location?.addressline1 || t("CS_NA")} />
        {location?.addressline2 && <Row label={t("GC_ADDRESS_LINE2")} text={location.addressline2} />}
        <Row label={t("GC_LANDMARK")} text={location?.landmark || t("CS_NA")} />
        <Row label={t("GC_CITY")} text={location?.city ? getTranslatedValue(location.city) : t("CS_NA")} />
        <Row label={t("GC_LOCALITY")} text={location?.locality ? getTranslatedValue(location.locality) : t("CS_NA")} />
        <Row label={t("GC_ADDRESS_PINCODE")} text={location?.pincode || t("CS_NA")} />
      </StatusTable>

      {/* 2. Owner Details */}
      <div className="gc-check-page-flex-row" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "24px" }}>
        <CardSubHeader className="gc-check-page-header">
          {t("ES_TITILE_OWNER_DETAILS") || "Owner Details"}
        </CardSubHeader>
        <ActionButton jumpTo="applicant-details" />
      </div>

      <StatusTable>
        <Row label={t("GC_OWNER_NAME")} text={ownerName} />
        <Row label={t("GC_MOBILE_NUMBER")} text={ownerMobile} />
        <Row label={t("GC_EMAIL")} text={ownerEmail} />
        {ownerGender && <Row label={t("GC_GENDER")} text={t(ownerGender)} />}
        {ownerGuardian && <Row label={t("GC_GUARDIAN_NAME")} text={ownerGuardian} />}
        {ownerRelationship && <Row label={t("GC_RELATIONSHIP")} text={t(ownerRelationship)} />}
        {ownerAltMobile && <Row label={t("GC_ALT_MOBILE_NUMBER")} text={ownerAltMobile} />}
      </StatusTable>

      {/* 3. Address Details */}
      <div className="gc-check-page-flex-row" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "24px" }}>
        <CardSubHeader className="gc-check-page-header">
          {t("ES_ADDRESS_DETAILS") || "Address Details"}
        </CardSubHeader>
        <ActionButton jumpTo="address-details" />
      </div>

      <StatusTable>
        {address?.addressType && <Row label={t("COMMON_ADDRESS_TYPE")} text={getTranslatedValue(address.addressType)} />}
        <Row label={t("GC_HOUSE_NO")} text={address?.houseNo || location?.houseNo || t("CS_NA")} />
        <Row label={t("GC_STREET_NAME")} text={address?.streetName || location?.streetName || t("CS_NA")} />
        <Row label={t("GC_ADDRESS_LINE1")} text={address?.addressLine1 || location?.addressline1 || t("CS_NA")} />
        {(address?.addressLine2 || location?.addressline2) && <Row label={t("GC_ADDRESS_LINE2")} text={address?.addressLine2 || location.addressline2} />}
        {(address?.landmark || location?.landmark) && <Row label={t("GC_LANDMARK")} text={address?.landmark || location.landmark} />}
        <Row label={t("GC_CITY")} text={address?.city ? getTranslatedValue(address.city) : (location?.city ? getTranslatedValue(location.city) : t("CS_NA"))} />
        <Row label={t("GC_LOCALITY")} text={address?.locality ? getTranslatedValue(address.locality) : (location?.locality ? getTranslatedValue(location.locality) : t("CS_NA"))} />
        <Row label={t("GC_ADDRESS_PINCODE")} text={address?.pincode || location?.pincode || t("CS_NA")} />
      </StatusTable>

      {/* 4. Collection Specifications */}
      <div className="gc-check-page-flex-row" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "24px" }}>
        <CardSubHeader className="gc-check-page-header">
          {t("GC_GARBAGE_SPECIFICATIONS") || "Collection Specifications"}
        </CardSubHeader>
        <ActionButton jumpTo="garbage-specifications" />
      </div>

      <StatusTable>
        <Row label={t("GC_TYPE_OF_COLLECTION")} text={getTranslatedValue(specifications?.typeOfCollection)} />
        <Row label={t("GC_CATEGORY")} text={getTranslatedValue(specifications?.category)} />
        <Row label={t("GC_SUB_CATEGORY")} text={getTranslatedValue(specifications?.subCategory)} />
        <Row label={t("GC_SUB_CATEGORY_TYPE")} text={getTranslatedValue(specifications?.subCategoryType)} />
        <Row label={t("GC_SPECIAL_CATEGORY")} text={getTranslatedValue(specifications?.specialCategory)} />
        {specifications?.oldGarbageId && (
          <Row label={t("GC_OLD_GARBAGE_ID")} text={specifications.oldGarbageId} />
        )}
        <Row label={t("GC_IS_INHERITANCE")} text={specifications?.isInheritance ? t("YES") : t("NO")} />
        {multiUnits.includes(specifications?.typeOfCollection?.code) && (
          <Row label={t("GC_NO_OF_UNITS")} text={String(specifications?.no_of_units || 1)} />
        )}
      </StatusTable>

      {/* Owner Uploaded Documents */}
      {ownerDocs.length > 0 && (
        <div style={{ marginTop: "12px", marginBottom: "20px" }}>
          <CardSubHeader style={{ fontSize: "16px", color: "#1d70b8" }}>
            {t("GC_OWNER_DOCUMENTS") || "Owner Uploaded Documents"} ({ownerDocs.length})
          </CardSubHeader>
          <StatusTable>
            <Card className="chb-doc-card">
              {ownerDocs.map((doc, index) => (
                <div key={`owner-doc-${index}`} className="chb-doc-item">
                  <CardSectionHeader>{t("GC_" + doc?.documentType?.replaceAll(".", "_"))}</CardSectionHeader>
                  {GCDocuments ? (
                    <GCDocuments value={ownerDocs} Code={doc?.documentType} index={index} />
                  ) : (
                    <span>{doc?.documentType}</span>
                  )}
                </div>
              ))}
            </Card>
          </StatusTable>
        </div>
      )}

      {/* 3. Tenant Cards & Specifications (if tenants added) */}
      {tenants && tenants.length > 0 && (
        <div style={{ marginTop: "24px", paddingTop: "16px", borderTop: "2px solid #b1b4b6" }}>
          <div className="gc-check-page-flex-row" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <CardSubHeader className="gc-check-page-header">
              {t("GC_REGISTERED_TENANTS_HEADER") || `Registered Tenant Applications (${tenants.length})`}
            </CardSubHeader>
            <ActionButton jumpTo="garbage-specifications" />
          </div>
          <p style={{ margin: "4px 0 16px 0", color: "#505a5f", fontSize: "14px" }}>
            {t("GC_TENANT_REVIEW_DESC") || "Individual Garbage Applications will be generated for each tenant mapped below."}
          </p>

          {tenants.map((tenant, idx) => {
            const thisTenantDocs = tenantDocsMap[tenant.id] || [];
            const floorLabel = tenant.floorNo === "0" ? t("GC_FLOOR_GROUND") || "Ground Floor" : `${tenant.floorNo}${tenant.floorNo === "1" ? "st" : tenant.floorNo === "2" ? "nd" : tenant.floorNo === "3" ? "rd" : "th"} Floor`;

            return (
              <div
                key={tenant.id || idx}
                style={{
                  border: "1px solid #28a197",
                  backgroundColor: "#f9fcfb",
                  padding: "16px",
                  borderRadius: "4px",
                  marginBottom: "16px",
                }}
              >
                <span style={{ fontWeight: "bold", fontSize: "16px", color: "#005a54" }}>
                  {t("GC_TENANT") || "Tenant"} #{idx + 1} ({tenant.name})
                </span>

                <StatusTable style={{ marginTop: "8px" }}>
                  <Row label={t("GC_FLOOR_UNIT") || "Assigned Floor"} text={floorLabel} />
                  <Row label={t("GC_TENANT_NAME") || "Tenant Name"} text={tenant.name || t("CS_NA")} />
                  <Row label={t("GC_TENANT_MOBILE_NO") || "Tenant Mobile No"} text={tenant.mobileNumber || t("CS_NA")} />
                  <Row label={t("GC_TENANT_EMAIL_ID") || "Tenant Email ID"} text={tenant.email || t("CS_NA")} />
                </StatusTable>

                {thisTenantDocs.length > 0 && (
                  <div style={{ marginTop: "12px" }}>
                    <CardSubHeader style={{ fontSize: "14px", color: "#005a54" }}>
                      {t("GC_TENANT_DOCS") || "Tenant Documents"} ({thisTenantDocs.length})
                    </CardSubHeader>
                    <StatusTable>
                      <Card className="chb-doc-card">
                        {thisTenantDocs.map((doc, dIdx) => (
                          <div key={`tdoc-${dIdx}`} className="chb-doc-item">
                            <CardSectionHeader>{t("GC_" + doc?.documentType?.replaceAll(".", "_"))}</CardSectionHeader>
                            {GCDocuments ? (
                              <GCDocuments value={thisTenantDocs} Code={doc?.documentType} index={dIdx} />
                            ) : (
                              <span>{doc?.documentType}</span>
                            )}
                          </div>
                        ))}
                      </Card>
                    </StatusTable>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Fallback for documents if not separated into owner/tenants */}
      {ownerDocs.length === 0 && allDocs.length > 0 && (
        <div style={{ marginTop: "24px" }}>
          <div className="gc-check-page-flex-row" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <CardSubHeader className="gc-check-page-header">
              {t("GC_GARBAGE_DOCUMENTS")}
            </CardSubHeader>
            <ActionButton jumpTo="garbage-documents" />
          </div>
          <StatusTable>
            <Card className="chb-doc-card">
              {allDocs.map((doc, index) => (
                <div key={`doc-${index}`} className="chb-doc-item">
                  <CardSectionHeader>{t("GC_" + doc?.documentType?.replaceAll(".", "_"))}</CardSectionHeader>
                  {GCDocuments ? (
                    <GCDocuments value={allDocs} Code={doc?.documentType} index={index} />
                  ) : (
                    <span>{doc?.documentType}</span>
                  )}
                </div>
              ))}
            </Card>
          </StatusTable>
        </div>
      )}

      {/* 4. Declaration Checkbox */}
      <div style={{ marginTop: "24px" }}>
        <CheckBox
          label={t("GC_FINAL_DECLARATION_MESSAGE") || "I hereby declare that all property details and tenant mappings are accurate."}
          onChange={() => setAgree(!agree)}
          checked={agree}
          styles={{ height: "auto" }}
        />
      </div>

      <SubmitBar
        label={t("GC_COMMON_BUTTON_SUBMIT") || "Submit Application"}
        onSubmit={onSubmit}
        disabled={!agree}
      />
    </Card>
  );
};

export default GCCheckPage;
