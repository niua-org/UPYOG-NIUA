import React, { useState, useRef } from "react";
import { Dropdown, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "./profileStyles";

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
 * @param {boolean}  [props.required]        - Appends a required asterisk to the label.
 * @param {Array}    props.options            - Dropdown options array (each item must have a `name` key).
 * @param {object}   props.selectedType      - Currently selected dropdown option.
 * @param {Function} props.onSelectType      - Callback fired when dropdown selection changes.
 * @param {string}   [props.typePlaceholder] - Placeholder text for the dropdown.
 * @param {object}   props.file              - Currently uploaded file `{ name, fileStoreId }`, or null.
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
    if (!selectedFile) return;
    if (!["image/jpeg", "image/png", "image/jpg", "application/pdf"].includes(selectedFile.type)) {
      setUploadError(t("PROFILE_DOC_INVALID_TYPE"));
      return;
    }
    if (selectedFile.size > 5 * 1024 * 1024) {
      setUploadError(t("PROFILE_DOC_SIZE_EXCEEDED"));
      return;
    }
    setUploadError(null);
    setUploading(true);
    try {
      const res = await Digit.UploadServices.Filestorage("profile", selectedFile, stateId);
      const fileStoreId = res?.data?.files?.[0]?.fileStoreId;
      if (!fileStoreId) throw new Error("NO_FILESTORE_ID");
      onFileChange({ name: selectedFile.name, fileStoreId });
    } catch {
      setUploadError(t("CORE_COMMON_PROFILE_FILE_UPLOAD_ERROR"));
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
        {t("CS_FILE_SIZE_RESTRICTIONS")}
      </div>
      {hasError && <CardLabelError style={styles.errorMargin}>{t(uploadError || error)}</CardLabelError>}
    </div>
  );
};


export default DocumentUploadCard