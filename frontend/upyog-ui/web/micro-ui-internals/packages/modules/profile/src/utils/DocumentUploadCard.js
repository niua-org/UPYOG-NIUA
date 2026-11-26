import React, { useState, useRef } from "react";
import { Dropdown, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "./profileStyles";

/**
 * Uploads a file to Digit Filestorage with retry logic for transient errors.
 *
 * RATIONALE:
 * The backend filestore service (`/filestore/v1/files`) intermittently encounters transient
 * 5xx errors or socket disconnects to the underlying storage (e.g. MinIO/S3). Because document
 * uploads are mandatory completion gates in the profile wizard, this helper retries only
 * transient 5xx/network errors (with a 500ms backoff) to prevent blocking the user unnecessarily.
 * Deterministic errors (4xx client validation errors or missing fileStoreId on a 200 response)
 * fail fast immediately on the first attempt without retrying.
 *
 * @param {string} module - Filestorage module name (e.g. "profile").
 * @param {File} file - File object to upload.
 * @param {string} tenant - Tenant ID for upload scoping.
 * @param {number} [maxRetries=2] - Maximum number of retries on transient errors.
 * @returns {Promise<{ name: string, fileStoreId: string }>} Uploaded file details containing file name and fileStoreId.
 * @throws {Error} Throws the last encountered error when all retries are exhausted or on non-transient errors.
 */
const uploadWithRetry = async (module, file, tenant, maxRetries = 2) => {
  let lastError;
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const res = await Digit.UploadServices.Filestorage(module, file, tenant);
      const fileStoreId = res?.data?.files?.[0]?.fileStoreId;
      if (fileStoreId) return { name: file.name, fileStoreId };
      throw new Error("NO_FILESTORE_ID");
    } catch (err) {
      lastError = err;
      // Fail fast on deterministic/non-transient errors (4xx client errors, or 200 with missing fileStoreId)
      const isTransient = err?.response?.status ? err.response.status >= 500 : err?.message !== "NO_FILESTORE_ID";
      if (!isTransient || attempt >= maxRetries) {
        throw lastError;
      }
      await new Promise((resolve) => setTimeout(resolve, 500));
    }
  }
  throw lastError;
};

/**
 * DocumentUploadCard
 *
 * Reusable document upload field used across the Profile module.
 * Renders a document type dropdown, a file picker (JPG/PNG/PDF, max 5MB),
 * an upload restriction hint, and an inline error label.
 *
 * Uploads the selected file to Digit Filestorage and returns `{ name, fileStoreId }`
 * via `onFileChange`. Validates file type and size before uploading.
 *
 * @param {object}   props
 * @param {string}   props.label             - Field label text.
 * @param {boolean}  [props.required]        - Appends a required asterisk to the label when true.
 * @param {Array<object>} props.options      - Dropdown options array (each item must have a `name` key).
 * @param {object}   props.selectedType      - Currently selected dropdown option.
 * @param {Function} props.onSelectType      - Callback fired when dropdown selection changes.
 * @param {string}   [props.typePlaceholder] - Placeholder text for the dropdown.
 * @param {{ name: string, fileStoreId: string } | null} props.file - Currently uploaded file `{ name, fileStoreId }`, or null.
 * @param {Function} props.onFileChange      - Callback fired with `{ name, fileStoreId }` on upload, or `null` on delete.
 * @param {string}   [props.error]           - External error message to display below the file box.
 * @param {Function} props.t                 - i18n translation function.
 * @param {string}   props.stateId           - State ID passed to Digit Filestorage for upload scoping.
 * @param {boolean}  [props.disabled=false]  - Disables the dropdown and file picker when true.
 *
 * @returns {JSX.Element}
 */
const DocumentUploadCard = ({ label, required, options, selectedType, onSelectType, typePlaceholder, file, onFileChange, error, t, stateId, disabled = false }) => {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const fileInputRef = React.useRef(null);
  const styles = profileStyles.documentUploadCard;

  const handleFileSelect = async (e) => {
    const selectedFile = e.target.files?.[0];
    // Reset file input value so choosing the same file again triggers onChange
    if (e.target) e.target.value = "";
    if (!selectedFile) return;

    const allowedMime = ["image/jpeg", "image/png", "image/jpg", "application/pdf"].includes(selectedFile.type);
    const hasAllowedExtension = /\.(jpe?g|png|pdf)$/i.test(selectedFile.name || "");
    const isAllowedType = (selectedFile.type ? allowedMime : true) && hasAllowedExtension;

    if (!isAllowedType) {
      setUploadError(t("PROFILE_DOC_INVALID_TYPE", "Only .jpg, .jpeg, .png and .pdf files are allowed"));
      return;
    }
    const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    if (selectedFile.size > MAX_FILE_SIZE) {
      setUploadError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED", "File size should be less than 5MB"));
      return;
    }
    setUploadError(null);
    setUploading(true);
    try {
      const uploadTenant = stateId || Digit.ULBService.getStateId() || Digit.ULBService.getCurrentTenantId() || "pg";
      const uploadedFile = await uploadWithRetry("profile", selectedFile, uploadTenant);
      onFileChange(uploadedFile);
    } catch (err) {
      console.error("Document upload error in DocumentUploadCard:", err);
      setUploadError(t("CORE_COMMON_PROFILE_FILE_UPLOAD_ERROR", "An error occurred while uploading the file"));
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = (e) => {
    e.stopPropagation();
    onFileChange(null);
    setUploadError(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const hasError = !!(uploadError || error);

  return (
    <div style={styles.wrapper}>
      <label style={profileStyles.label}>
        {label}{required && <span style={profileStyles.required}> *</span>}
      </label>
      <Dropdown
        selected={selectedType}
        select={onSelectType}
        option={options}
        optionKey="name"
        t={t}
        placeholder={typePlaceholder || t("CHOOSE_DOCUMENT_TYPE")}
        disable={disabled}
        style={styles.dropdownMargin}
      />
      <div style={{ ...styles.fileBox, ...(hasError ? styles.fileBoxError : {}), ...(disabled ? styles.fileBoxDisabled : {}) }}>
        <div style={styles.fileInner}>
          <input type="file" ref={fileInputRef} accept=".jpg,.jpeg,.png,.pdf" onChange={handleFileSelect} disabled={disabled || uploading} style={{ display: "none" }} />
          <button type="button" onClick={() => fileInputRef.current?.click()} disabled={disabled || uploading}
            style={{ ...styles.chooseBtn, ...(disabled || uploading ? styles.chooseBtnDisabled : {}) }}>
            {uploading ? t("UPLOADING") : t("CHOOSE_FILE")}
          </button>
          <span style={{ ...styles.fileName, ...(file?.name ? styles.fileNameUploaded : {}) }}>
            {uploading ? t("UPLOADING") : file?.name || t("NO_FILE_SELECTED")}
          </span>
        </div>
        {file && !disabled && (
          <button type="button" onClick={handleDelete} style={styles.deleteBtn}>✕</button>
        )}
      </div>
       <div style={styles.fileRestrictionText}>
        {t("CS_FILE_SIZE_RESTRICTIONS", "Only .jpg, .jpeg, .png and .pdf files up to 5MB are allowed")}
      </div>
      {hasError && <CardLabelError style={styles.errorMargin}>{uploadError || t(error)}</CardLabelError>}
    </div>
  );
};


export default DocumentUploadCard