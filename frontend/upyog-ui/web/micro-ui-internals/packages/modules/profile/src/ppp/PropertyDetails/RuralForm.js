import React, { useState } from "react";
import {
  Dropdown,
  TextInput,
  RadioButtons, UploadFile
} from "@nudmcdgnpm/digit-ui-react-components";

import { profileStyles } from "../../utils/profileStyles";

/**
 * Rural Property Form
 * Page Developed to get the Rural Property details of the family Members
 * These Details Will be filled by the Family Head
 * 
 */

const RuralPropertyForm = ({ data = {}, onChange, t, disabled = false }) => {
  const styles = profileStyles.ruralPropertyForm;

  const [gramPanchayat, setGramPanchayat] = useState(data.gramPanchayat || null);
  const [village, setVillage] = useState(data.village || null);
  const [ward, setWard] = useState(data.ward || null);
  const [propertyDocument, setPropertyDocument] = useState(data.propertyDocument || null);
  const [fileUploadError, setFileUploadError] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [propertyAddress, setPropertyAddress] = useState(data.propertyAddress || null);
  const [documentType, setDocumentType] = useState(data.documentType || null);
  const [propertyType, setPropertyType] = useState(data.propertyType || null);
  const [propertyUsage, setPropertyUsage] = useState(data.propertyUsage || null);
  const [surveyNumber, setSurveyNumber] = useState(data.surveyNumber || "");

  const ownershipOptions = [
  {
    code: "INDIVIDUAL",
    name: "Individual",
  },
  {
    code: "JOINT",
    name: "Joint Ownership",
  },
  {
    code: "INSTITUTION",
    name: "Institution / Organisation",
  },
];

  const [ownershipType, setOwnershipType] = useState(data.ownershipType ? ownershipOptions.find(
    (option) => option.code === data.ownershipType) : null);
  const [ownerName, setOwnerName] = useState(data.ownerName || "");
  const [ownerMobile, setOwnerMobile] = useState(data.ownerMobile || "");

  const gramPanchayatOptions = [
    {
      name: "Select Gram Panchayat",
      code: "SELECT",
    },
  ];

  const villageOptions = [
    {
      name: "Select Village",
      code: "SELECT",
    },
  ];

  const wardOptions = [
    {
      name: "Select Ward",
      code: "SELECT",
    },
  ];

  const propertyTypeOptions = [
    {
      code: "VACANT_LAND",
      name: "Vacant Land",
    },
    {
      code: "BUILDING",
      name: "Building / House",
    },
    {
      code: "LAND_WITH_BUILDING",
      name: "Land with Building",
    },
  ];
  const documentTypeOptions = [
    {
      code: "Pdf",
      name: "Pdf",
    },
    {
      code: "Notary",
      name: "Notary",
    },
  ];

  const propertyUsageOptions = [
    {
      name: "Select property usage",
      code: "SELECT",
    },
  ];

  const updateField = (key, value) => {
    onChange({
      [key]: value,
    });
  };

  const handleGramPanchayatChange = (value) => {
    setGramPanchayat(value);
    updateField("gramPanchayat", value);
  };

  const handleVillageChange = (value) => {
    setVillage(value);
    updateField("village", value);
  };

  const handleWardChange = (value) => {
    setWard(value);
    updateField("ward", value);
  };

  const handlePropertyAddressChange = (value) => {
    setPropertyAddress(value);
    updateField("propertyAddress", value);
  };

  const handlePropertyTypeChange = (value) => {
    setPropertyType(value);
    updateField("propertyType", value);
  };

  const handlePropertyUsageChange = (value) => {
    setPropertyUsage(value);
    updateField("propertyUsage", value);
  };

  const handleSurveyNumberChange = (value) => {
    setSurveyNumber(value);
    updateField("surveyNumber", value);
  };

  const handleOwnershipTypeChange = (value) => {
    setOwnershipType(value);
    updateField("ownershipType", value?.code);
  };

  const handleOwnerNameChange = (value) => {
    setOwnerName(value);
    updateField("ownerName", value);
  };

  const handleOwnerMobileChange = (value) => {
    setOwnerMobile(value);
    updateField("ownerMobile", value);
  };

  const handlePropertyDocumentChange = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setPropertyDocument(file);
    setFileUploadError(null);
    updateField("propertyDocument", null);

    if (file.size >= 5242880) {
      setFileUploadError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
      return;
    }

    setIsUploading(true);

    try {
      const response = await Digit.UploadServices.Filestorage(
        "property-upload",
        file,
        Digit.ULBService.getStateId()
      );
      const fileStoreId = response?.data?.files?.[0]?.fileStoreId;
      if (!fileStoreId) {
        throw new Error("File upload failed: filestoreId not received");
      }

      const uploadedDocument = { name: file.name, fileStoreId };
      setPropertyDocument(uploadedDocument);
      updateField("propertyDocument", uploadedDocument);
    } catch {
      setFileUploadError(t("CS_FILE_UPLOAD_ERROR"));
    } finally {
      setIsUploading(false);
    }
  };

  const handlePropertyDocumentDelete = () => {
    setPropertyDocument(null);
    setFileUploadError(null);
    updateField("propertyDocument", null);
  };
  const documentTypeChange = (value) => {
    setDocumentType(value);
    updateField("documentType", value);
  };

  return (
    <div style={{ ...styles.container, ...(disabled ? { opacity: 0.9, pointerEvents: "none" } : {}) }}>
      <div style={styles.header}>
        <div style={styles.title}>
          Register Your Property – Rural
        </div>
      </div>

      <div style={styles.section}>
        <div style={styles.sectionTitle}>
          1. Property Location
        </div>

        {/* Row 1 */}
        <div style={styles.fieldRow}>
          {/* Gram Panchayat */}
          <div style={styles.field}>
            <label style={profileStyles.label}> Gram Panchayat{" "}
              <span style={profileStyles.required}>*</span>
            </label>

            <Dropdown
              option={gramPanchayatOptions}
              optionKey="name"
              selected={gramPanchayat}
              select={handleGramPanchayatChange}
              placeholder="Select Gram Panchayat"
            />
          </div>

          {/* Village */}
          <div style={styles.field}>
            <label style={profileStyles.label}>
              Village Name{" "}
              <span style={profileStyles.required}>*</span>
            </label>

            <Dropdown
              option={villageOptions}
              optionKey="name"
              selected={village}
              select={handleVillageChange}
              placeholder="Enter Village Name"
            />
          </div>

        </div>

        {/* Row 2 */}
        <div style={styles.fieldRow}>

          {/* Ward */}
          <div style={styles.field}>
            <label style={profileStyles.label}>
              Ward Number{" "}
              <span style={profileStyles.required}>*</span>
            </label>

            <Dropdown
              option={wardOptions}
              optionKey="name"
              selected={ward}
              select={handleWardChange}
              placeholder="Enter Ward Number"
            />
          </div>

          {/* Property Address */}
          <div style={styles.field}>
            <label style={profileStyles.label}>
              Property Address / Locality{" "}
              <span style={profileStyles.required}>*</span>
            </label>

            <TextInput
              value={propertyAddress}
              onChange={(e) =>
                handlePropertyAddressChange(e.target.value)
              }
              placeholder="Enter Address/ Locality"
            />
          </div>

        </div>
      </div>

      <div style={styles.section}>
        <div style={styles.sectionTitle}>
          2. Property Details
        </div>

        {/* Row 1 */}
        <div style={styles.fieldRow}>
          <div style={styles.field}>
            <label style={profileStyles.label}>
              Property Type{" "}
              <span style={profileStyles.required}>*</span>
            </label>
            <Dropdown
              option={propertyTypeOptions}
              optionKey="name"
              selected={propertyType}
              select={handlePropertyTypeChange}
              placeholder="Select Property Type"
            />
          </div>
          {/* Property Usage */}
          <div style={styles.field}>
            <label style={profileStyles.label}>
              Property Usage{" "}
              <span style={profileStyles.required}>*</span>
            </label>
            <Dropdown
              option={propertyUsageOptions}
              optionKey="name"
              selected={propertyUsage}
              select={handlePropertyUsageChange}
              placeholder="Select Property Usage"
            />

          </div>

        </div>

        {/* Row 2 */}
        <div style={styles.fieldRow}>

          {/* Survey Number */}
          <div style={styles.field}>

            <label style={profileStyles.label}>
              Survey Number{" "}
              <span style={profileStyles.required}>*</span>
            </label>

            <TextInput
              value={surveyNumber}
              onChange={(e) =>
                handleSurveyNumberChange(e.target.value)
              }
              placeholder="Enter survey number"
            />

          </div>

          {/* Empty column to maintain 2-column layout */}
          <div />

        </div>

      </div>
      <div style={styles.section}>
        <div style={styles.sectionTitle}>
          3. Ownership Details
        </div>
        <div style={styles.fieldRow}>
          <div style={styles.field}>
            <label style={profileStyles.label}>
              Ownership Type{" "}
              <span style={profileStyles.required}>*</span>
            </label>
            <RadioButtons
              style={styles.radioGroup}
              innerStyles={{ display: "flex", alignItems: "center", marginBottom: 0 }}
              options={ownershipOptions}
              optionsKey="name"
              selectedOption={ownershipType}
              onSelect={handleOwnershipTypeChange}
            />
          </div>
          <div />
        </div>
        {ownershipType?.code === "JOINT" && (
          <div style={styles.jointOwnerSection}>

            <div style={styles.jointOwnerTitle}>
              Joint Owner Details
            </div>

            <div style={styles.fieldRow}>

              {/* Joint Owner Name */}
              <div style={styles.field}>

                <label style={profileStyles.label}>
                  Joint Owner Name{" "}
                  <span style={profileStyles.required}>
                    *
                  </span>
                </label>

                <TextInput
                  value={ownerName}
                  onChange={(e) =>
                    handleOwnerNameChange(e.target.value)
                  }
                  placeholder="Enter Property Owner Name"
                />

              </div>

              {/* Joint Owner Mobile */}
              <div style={styles.field}>

                <label style={profileStyles.label}>
                  Joint Owner Mobile Number{" "}
                  <span style={profileStyles.required}>
                    *
                  </span>
                </label>

                <TextInput
                  value={ownerMobile}
                  onChange={(e) =>
                    handleOwnerMobileChange(e.target.value)
                  }
                  placeholder="Enter Mobile Number"
                />

              </div>

            </div>

            <div style={styles.hint}>
              Additional joint owners can be added if required.
            </div>

          </div>
        )}
      </div>
      <div style={styles.field}>
        <div style={styles.sectionTitle}>
          4. Document Uplaod
        </div>
        <label style={profileStyles.label}>
          Address Proof{" "}
          <span style={profileStyles.required}>*</span>
        </label>
        <Dropdown
          option={documentTypeOptions}
          optionKey="name"
          selected={documentType}
          select={documentTypeChange}
          placeholder="Choose Document Type"
        />

        <UploadFile
          onUpload={handlePropertyDocumentChange}
          onDelete={handlePropertyDocumentDelete}
          file={propertyDocument}
          message={
            fileUploadError ||
            (isUploading ? "Uploading..." : propertyDocument?.name || t("PROFILE_NO_FILE_SELECTED"))
          }
          error={fileUploadError}
          disabled={isUploading}
          accept=".pdf,.jpg,.jpeg,.png"
        />

      </div>
    </div>
  );
};

export default RuralPropertyForm;