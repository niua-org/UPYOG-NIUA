import React, { useState, useCallback, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { UploadFile, Toast, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "../utils/profileStyles";
import { PDFSvg, openDocument } from "../utils";
import {
  buildCreateDocumentPayload,
  buildUpdateDocumentPayload,
} from "../utils/payloads";

/*
 * EditDocuments
 *
 * Handles Identity Proof and DOB Proof upload for the user profile.
 *
 * Initial creation:
 * Both Identity Proof and DOB Proof are mandatory.
 *
 * Existing documents:
 * Each document can be independently re-uploaded.
 * Missing documents can be uploaded individually.
 *
 * Create → Digit.UserService.createUserDocument
 * Update → Digit.UserService.updateUserDocument
 */

const MAX_SIZE = 5 * 1024 * 1024;

const ACCEPTED = ".jpg,.jpeg,.png,.pdf";

const ALLOWED_TYPES = [
  "image/jpeg",
  "image/png",
  "image/jpg",
  "application/pdf",
];

const DOC_TYPES = [
  { key: "IDENTITY_PROOF", label: "PROFILE_IDENTITY_PROOF" },
  { key: "DOB_PROOF", label: "PROFILE_DOB_PROOF" },
];

const INITIAL_FILES = {
  IDENTITY_PROOF: null,
  DOB_PROOF: null,
};

const styles = profileStyles.documentUpload;

// Uploaded file preview: icon
const UploadedDocTile = ({ doc, onView, t }) => (
  <button type="button" style={styles.fileTile} onClick={onView} title={t("PROFILE_VIEW_DOC")}>
    <PDFSvg width={48} height={48} />
    <span style={styles.viewLink}>{t("PROFILE_VIEW_DOC")}</span>
  </button>
);

const EditDocuments = ({ userDetails, onDocumentsUpdate }) => {
  const { t } = useTranslation();

  const tenantId = Digit.ULBService.getCurrentTenantId();
  const userInfo = Digit.UserService.getUser()?.info || {};

  const [documents, setDocuments] = useState(
    (userDetails?.documents || []).filter((doc) => doc.active === true)
  );

  const [files, setFiles] = useState(INITIAL_FILES);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const toastTimerRef = useRef(null);


  // it will sync detail from userdetail when refresh page
  useEffect(() => {
  if (userDetails?.documents?.length) {
    setDocuments(userDetails.documents.filter((doc) => doc.active === true));
  }
}, [userDetails?.documents]);

  // Cleanup toast timer on unmount
  useEffect(() => {
    return () => {
      if (toastTimerRef.current) {
        clearTimeout(toastTimerRef.current);
      }
    };
  }, []);

  const showToast = useCallback((type, msg) => {
    if (toastTimerRef.current) {
      clearTimeout(toastTimerRef.current);
    }

    setToast({ type, msg });

    toastTimerRef.current = setTimeout(() => {
      setToast(null);
      toastTimerRef.current = null;
    }, 5000);
  }, []);

  // Find an active document by document type
  const findExisting = useCallback(
    (documentType) =>
      documents.find(
        (doc) =>
          doc.documentType === documentType &&
          doc.active === true
      ),
    [documents]
  );

  // Refresh documents after successful create/update
  const refreshDocuments = useCallback(async () => {
    if (!userInfo.uuid) return;

    const response = await Digit.UserService.userSearch(
      tenantId,
      {
        uuid: userInfo.uuid,
        include: ["DOCUMENT"],
      },
      {}
    );

    const nextDocs = (response?.user?.[0]?.documents || []).filter(
      (doc) => doc.active === true
    );

    setDocuments(nextDocs);

    onDocumentsUpdate?.({ documents: nextDocs });
  }, [userInfo.uuid, tenantId, onDocumentsUpdate]);

  // Validate uploaded file
  const validateFile = (file, key) => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      setErrors((prev) => ({
        ...prev,
        [key]: t("PROFILE_DOC_INVALID_TYPE"),
      }));

      return false;
    }

    if (file.size > MAX_SIZE) {
      setErrors((prev) => ({
        ...prev,
        [key]: t("PROFILE_DOC_SIZE_EXCEEDED"),
      }));

      return false;
    }

    setErrors((prev) => ({
      ...prev,
      [key]: null,
    }));

    return true;
  };

  // Handle file selection
  const handleUpload = (e, key) => {
    const file = e.target.files?.[0];

    if (!file) return;

    if (!validateFile(file, key)) {
      return;
    }

    setFiles((prev) => ({
      ...prev,
      [key]: file,
    }));
  };

  // Handle file deletion
  const handleDelete = (key) => {
    setFiles((prev) => ({
      ...prev,
      [key]: null,
    }));

    setErrors((prev) => ({
      ...prev,
      [key]: null,
    }));
  };

  // Upload file to filestore
  const uploadToFilestore = async (file) => {
    const response = await Digit.UploadServices.Filestorage(
      "profile",
      file,
      tenantId
    );

    const filestoreId = response?.data?.files?.[0]?.fileStoreId;

    if (!filestoreId) {
      throw new Error("File upload failed: filestoreId not received");
    }

    return filestoreId;
  };

  // Save documents
  // Resolves to true when the documents are saved (or already complete and `proceedIfUnchanged` is set)
  const handleSave = async () => {
    if (saving) return;

    const isInitialCreation = !DOC_TYPES.some(({ key }) =>
      findExisting(key)
    );

    const toUpload = DOC_TYPES.filter(({ key }) => files[key]);

    // Both documents are mandatory during initial creation
    if (
      isInitialCreation &&
      DOC_TYPES.some(({ key }) => !files[key])
    ) {
      showToast("error", t("PROFILE_BOTH_DOCS_REQUIRED"));
      return;
    }

    // At least one document must be selected for re-upload
    if (!toUpload.length) {
      // Create Family flow: nothing new to upload but both documents already exist, so just continue
      if (proceedIfUnchanged && DOC_TYPES.every(({ key }) => findExisting(key))) return true;
      showToast("error", t("PROFILE_NO_DOC_SELECTED"));
      return;
    }

    if (!userInfo.uuid) {
      showToast("error", t("CORE_COMMON_PROFILE_UPDATE_ERROR"));
      return;
    }

    setSaving(true);

    try {
      // Upload all selected files to filestore first
      const uploaded = await Promise.all(
        toUpload.map(async ({ key }) => ({
          documentType: key,
          filestoreId: await uploadToFilestore(files[key]),
          fileName: files[key].name,
        }))
      );

      const toCreate = [];
      const toUpdate = [];

      // Prepare create and update payloads
      for (const item of uploaded) {
        const existing = findExisting(item.documentType);

        if (existing?.id) {
          const document = buildUpdateDocumentPayload({
            existingDoc: existing,
            tenantId,
            filestoreId: item.filestoreId,
            fileName: item.fileName,
          });

          toUpdate.push(document);
        } else {
          const document = buildCreateDocumentPayload({
            uuid: userInfo.uuid,
            tenantId,
            documentType: item.documentType,
            filestoreId: item.filestoreId,
            fileName: item.fileName,
          });

          toCreate.push(document);
        }
      }

      // Execute update API calls
      for (const document of toUpdate) {
        await Digit.UserService.updateUserDocument(
          document,
          tenantId,
          userInfo.uuid
        );
      }

      // Execute create API call
      if (toCreate.length) {
        await Digit.UserService.createUserDocument(
          toCreate,
          tenantId
        );
      }

      // Refresh documents after successful operations
      await refreshDocuments();

      setFiles({ ...INITIAL_FILES });
      setErrors({});

      showToast("success", t("PROFILE_DOCS_SAVED"));
    } catch (error) {
      console.error("Error saving profile documents:", error);

      showToast(
        "error",
        t("CORE_COMMON_PROFILE_UPDATE_ERROR")
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
     
      <div style={styles.grid}>
        {DOC_TYPES.map(({ key, label }) => {
          const existing = findExisting(key);

          return (
            <div key={key} style={styles.card}>
              <div style={styles.header}>
                <div style={styles.title}>
                  {t(label)}
                  {!existing && <span style={profileStyles.required}> *</span>}
                </div>
              </div>

            {existing ? (
              <UploadedDocTile
                doc={existing}
                t={t}
                onView={() => openDocument(existing.filestoreId, tenantId)}
              />
            ) : (
              <div style={styles.emptyTile}>{t("PROFILE_NO_DOC_UPLOADED")}</div>
            )}

            <div>
              <div style={styles.uploadLabel}>
                {existing ? t("PROFILE_REPLACE_DOC") : t("PROFILE_UPLOAD_DOC")}
              </div>
              <UploadFile
                accept={ACCEPTED}
                onUpload={(e) => handleUpload(e, key)}
                onDelete={() => handleDelete(key)}
                file={files[key]}
                message={existing ? t("PROFILE_REUPLOAD_DOC") : t("PROFILE_NO_FILE_SELECTED")}
                iserror={errors[key]}
              />
            </div>
            </div>
          );
        })}
      </div>

      <div style={profileStyles.actions}>
        <SubmitBar
          label={t("CORE_COMMON_SAVE")}
          onSubmit={handleSave}
          disabled={saving}
        />
      </div>

      {toast && (
        <div className="profile-toast-container">
          <Toast
            error={toast.type === "error"}
            label={toast.msg}
            onClose={() => {
              if (toastTimerRef.current) {
                clearTimeout(toastTimerRef.current);
                toastTimerRef.current = null;
              }

              setToast(null);
            }}
          />
        </div>
      )}
    </>
  );
};

export default EditDocuments;
