import React, { useState, useRef } from "react";
import { Dropdown, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "./profileStyles";

/**
 * New Document Upload Card Component
 * Used Accross Whole Profile Module to show the New Document Fields
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
      setUploadError(t("PROFILE_DOC_INVALID_TYPE", "Only JPG, PNG, and PDF files are allowed"));
      return;
    }
    if (selectedFile.size > 5 * 1024 * 1024) {
      setUploadError(t("PROFILE_DOC_SIZE_EXCEEDED", "File size cannot exceed 5MB"));
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
      setUploadError(t("CORE_COMMON_PROFILE_FILE_UPLOAD_ERROR", "Failed to upload file"));
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
        placeholder={typePlaceholder || t("CHOOSE_DOCUMENT_TYPE", "Choose Document Type")}
        disable={disabled}
        style={styles.dropdownMargin}
      />
      <div style={{ ...styles.fileBox, ...(hasError ? styles.fileBoxError : {}), ...(disabled ? styles.fileBoxDisabled : {}) }}>
        <div style={styles.fileInner}>
          <input type="file" ref={fileInputRef} accept=".jpg,.jpeg,.png,.pdf" onChange={handleFileSelect} disabled={disabled || uploading} style={{ display: "none" }} />
          <button type="button" onClick={() => fileInputRef.current?.click()} disabled={disabled || uploading}
            style={{ ...styles.chooseBtn, ...(disabled || uploading ? styles.chooseBtnDisabled : {}) }}>
            {uploading ? t("UPLOADING", "Uploading...") : t("CHOOSE_FILE", "Choose file")}
          </button>
          <span style={{ ...styles.fileName, ...(file?.name ? styles.fileNameUploaded : {}) }}>
            {uploading ? t("UPLOADING", "Uploading...") : file?.name || t("NO_FILE_SELECTED", "No file selected")}
          </span>
        </div>
        {file && !disabled && (
          <button type="button" onClick={handleDelete} style={styles.deleteBtn}>✕</button>
        )}
      </div>
      {hasError && <CardLabelError style={styles.errorMargin}>{t(uploadError || error)}</CardLabelError>}
    </div>
  );
};


export default DocumentUploadCard