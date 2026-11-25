import React, { useState } from "react";
import {Dropdown, TextInput, RadioButtons, UploadFile} from "@nudmcdgnpm/digit-ui-react-components";

/**
 * Collects and updates rural property details for a family member.
 *
 * @param {object} props Component props.
 * @param {object} [props.data={}] Existing rural property details.
 * @param {Function} props.onChange Callback invoked when a property field changes.
 * @param {Function} props.t Translation function.
 * @param {boolean} [props.disabled=false] Whether editing is disabled.
 * @returns {JSX.Element} The rural property form.
 */

const RuralPropertyForm = ({ data = {}, onChange, t, disabled = false }) => {
  const stateId = Digit.ULBService.getStateId() || Digit.ULBService.getCurrentTenantId() || "pg";
  const maxUploadSize = 5 * 1024 * 1024;
  const allowedFileExtensions = [".jpg", ".jpeg", ".png", ".pdf"];
  const { data: ownershipOptions = [] } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "common-masters",
    [{ name: "PropertyOwnershipType" }],
    {
      select: (mdmsData) => {
        const options = mdmsData?.["common-masters"]?.PropertyOwnershipType;
        return options
          ?.filter((option) => option.active !== false)
          .map((option) => ({
            code: option.code,
            name: option.code === "ORGANISATION" ? "Organisation/Institution" : option.name || option.code,
          })) || [];
      },
    }
  );

  const { data: propertyUsageOptions = [] } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "PropertyTax",
    [{ name: "UsageCategory" }],
    {
      select: (mdmsData) => {
        const options = mdmsData?.["PropertyTax"]?.UsageCategory;
        return options
          ?.filter((option) => option.active !== false)
          .map((option) => ({
            code: option.code,
            name: option.name || option.code,
          })) || [];
      },
    }
  );

  const { data: propertyTypeOptions = [] } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "PropertyTax",
    [{ name: "PropertyType" }],
    {
      select: (mdmsData) => {
        const options = mdmsData?.["PropertyTax"]?.PropertyType;
        return (
          options
            ?.filter((option) => option.active !== false)
            .map((option) => ({
              code: option.code,
              name: option.name || option.code,
            })) || []
        );
      },
    }
  );

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

  const savedOwnershipCode = data.ownershipType === "INSTITUTION" ? "ORGANISATION" : data.ownershipType;
  const ownershipType = ownershipOptions.find((option) => option.code === savedOwnershipCode) || null;
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

  const documentTypeOptions = [
    {
      code: "Aadhaar",
      name: "Aadhaar",
    },
    {
      code: "VoterID",
      name: "Voter ID",
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

    const extension = file.name.slice(file.name.lastIndexOf(".")).toLowerCase();
    if (!allowedFileExtensions.includes(extension) || file.size > maxUploadSize) {
      setPropertyDocument(null);
      updateField("propertyDocument", null);
      setFileUploadError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
      event.target.value = "";
      return;
    }

    setPropertyDocument(file);
    setFileUploadError(null);
    updateField("propertyDocument", null);
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
    <div className="rural-property-form-container" style={disabled ? { opacity: 0.9, pointerEvents: "none" } : undefined}>
      <div className="rural-property-header">
        <div className="rural-property-title">
          {t("PROFILE_REGISTER_PROPERTY_RURAL")}
        </div>
      </div>

      <div className="rural-property-section">
        <div className="rural-property-section-title">
          {t("PROFILE_PROPERTY_LOCATION")}
        </div>

        {/* Row 1 */}
        <div className="rural-property-field-row profile-property-rural-row">
          {/* Gram Panchayat */}
          <div className="rural-property-field">
            <label className="profile-form-label">{t("PROFILE_GRAM_PANCHAYAT")}{" "}
              <span className="profile-form-required">*</span>
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
          <div className="rural-property-field">
            <label className="profile-form-label">
              {t("PROFILE_VILLAGE_NAME")}{" "}
              <span className="profile-form-required">*</span>
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
        <div className="rural-property-field-row profile-property-rural-row">

          {/* Ward */}
          <div className="rural-property-field">
            <label className="profile-form-label">
              {t("PROFILE_WARD_NUMBER")}{" "}
              <span className="profile-form-required">*</span>
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
          <div className="rural-property-field">
            <label className="profile-form-label">
              {t("PROFILE_PROPERTY_ADDRESS_LOCALITY")}{" "}
              <span className="profile-form-required">*</span>
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

      <div className="rural-property-section">
        <div className="rural-property-section-title">
          {t("PROFILE_PROPERTY_DETAILS_SECTION")}
        </div>

        {/* Row 1 */}
        <div className="rural-property-field-row profile-property-rural-row">
          <div className="rural-property-field">
            <label className="profile-form-label">
              {t("PROFILE_PROPERTY_TYPE")}{" "}
              <span className="profile-form-required">*</span>
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
          <div className="rural-property-field">
            <label className="profile-form-label">
              {t("PROFILE_PROPERTY_USAGE")}{" "}
              <span className="profile-form-required">*</span>
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
        <div className="rural-property-field-row profile-property-rural-row">

          {/* Survey Number */}
          <div className="rural-property-field">

            <label className="profile-form-label">
              {t("PROFILE_SURVEY_NUMBER")}{" "}
              <span className="profile-form-required">*</span>
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
      <div className="rural-property-section">
        <div className="rural-property-section-title">
          {t("PROFILE_OWNERSHIP_DETAILS")}
        </div>
        <div className="rural-property-field-row profile-property-rural-row">
          <div className="rural-property-field">
            <label className="profile-form-label">
              {t("PROFILE_OWNERSHIP_TYPE")}{" "}
              <span className="profile-form-required">*</span>
            </label>
            <RadioButtons
              additionalWrapperClass="rural-property-radio-group"
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
          <div className="rural-property-joint-owner-section">

            <div className="rural-property-joint-owner-title">
              {t("PROFILE_JOINT_OWNER_DETAILS")}
            </div>

            <div className="rural-property-field-row profile-property-rural-row">

              {/* Joint Owner Name */}
              <div className="rural-property-field">

                <label className="profile-form-label">
                  {t("PROFILE_JOINT_OWNER_NAME")}{" "}
                  <span className="profile-form-required">
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
              <div className="rural-property-field">

                <label className="profile-form-label">
                  {t("PROFILE_JOINT_OWNER_MOBILE")}{" "}
                  <span className="profile-form-required">
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

            <div className="rural-property-hint">
              {t("PROFILE_JOINT_OWNER_HINT")}
            </div>

          </div>
        )}
      </div>
      <div className="rural-property-field">
        <div className="rural-property-section-title">
          {t("PROFILE_DOCUMENT_UPLOAD")}
        </div>
        <label className="profile-form-label">
          {t("PROFILE_ADDRESS_PROOF")}{" "}
          <span className="profile-form-required">*</span>
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
            (isUploading ? t("UPLOADING") : propertyDocument?.name || t("PROFILE_NO_FILE_SELECTED"))
          }
          error={fileUploadError}
          disabled={isUploading}
          accept=".pdf,.jpg,.jpeg,.png"
        />
        <div className="document-upload-file-restriction">
        {t("CS_FILE_SIZE_RESTRICTIONS")}
      </div>

      </div>
    </div>
  );
};

export default RuralPropertyForm;