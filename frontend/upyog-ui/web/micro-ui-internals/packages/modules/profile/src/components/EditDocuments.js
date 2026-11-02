import React, { useState, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { UploadFile, Toast, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "../utils/profileStyles";
import { PDFSvg, openDocument } from "../utils";
import { buildCreateDocumentPayload, buildUpdateDocumentPayload } from "../utils/payloads";

/*
 * EditDocuments
 *
 * Handles Identity Proof and DOB Proof upload for the user profile.
 * Existing docs are shown with a re-upload option; otherwise empty upload fields.
 * Create → Digit.UserService.createUserDocument (/user/document/_create)
 * Update → Digit.UserService.updateUserDocument (/user/document/_update)
 * After either call, refreshDocuments re-fetches the user (include DOCUMENT)
 * so the list updates immediately — same pattern as EditAddress.refreshAddresses.
 */

const MAX_SIZE = 5 * 1024 * 1024;
const ACCEPTED = ".jpg,.jpeg,.png,.pdf";
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/jpg", "application/pdf"];

const DOC_TYPES = [
  { key: "IDENTITY_PROOF", label: "PROFILE_IDENTITY_PROOF" },
  { key: "DOB_PROOF", label: "PROFILE_DOB_PROOF" },
];

const EditDocuments = ({ userDetails, onDocumentsUpdate }) => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const userInfo = Digit.UserService.getUser()?.info || {};


  const [documents, setDocuments] = useState(
  (userDetails?.documents || []).filter((doc) => doc.active === true)
);
  const [files, setFiles] = useState({ IDENTITY_PROOF: null, DOB_PROOF: null });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const hasExisting = documents.length > 0;

  const showToast = (type, msg) => {
    setToast({ type, msg });
    setTimeout(() => setToast(null), 5000);
  };

  // callback to re-fetch documents after create/update — same idea as refreshAddresses
  const refreshDocuments = useCallback(async () => {
    if (!userInfo.uuid) return;
    const response = await Digit.UserService.userSearch(
      tenantId,
      { uuid: [userInfo.uuid], include: ["DOCUMENT"] },
      {}
    );
    const nextDocs = (response?.user?.[0]?.documents || []).filter((doc) => doc.active === true);
    setDocuments(nextDocs);
    onDocumentsUpdate?.({ documents: nextDocs });
  }, [userInfo.uuid, tenantId, onDocumentsUpdate]);

  const validateFile = (file, key) => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      setErrors((prev) => ({ ...prev, [key]: t("PROFILE_DOC_INVALID_TYPE") }));
      return false;
    }
    if (file.size > MAX_SIZE) {
      setErrors((prev) => ({ ...prev, [key]: t("PROFILE_DOC_SIZE_EXCEEDED") }));
      return false;
    }
    setErrors((prev) => ({ ...prev, [key]: null }));
    return true;
  };

  const handleUpload = (e, key) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!validateFile(file, key)) return;
    setFiles((prev) => ({ ...prev, [key]: file }));
  };

  const handleDelete = (key) => {
    setFiles((prev) => ({ ...prev, [key]: null }));
    setErrors((prev) => ({ ...prev, [key]: null }));
  };

  const uploadToFilestore = async (file) => {
    const res = await Digit.UploadServices.Filestorage("profile", file, tenantId);
    return res?.data?.files?.[0]?.fileStoreId || null;
  };

  const findExisting = (documentType) =>documents.find((doc) => doc.documentType === documentType && doc.active === true);

  const handleSave = async () => {
    const toUpload = DOC_TYPES.filter(({ key }) => files[key]);
    if (!toUpload.length) {
      showToast("error", t("PROFILE_NO_DOC_SELECTED"));
      return;
    }
    setSaving(true);
    try {
      const uploaded = await Promise.all(
        toUpload.map(async ({ key }) => ({
          documentType: key,
          filestoreId: await uploadToFilestore(files[key]),
          fileName: files[key]?.name,
        }))
      );

      const toCreate = [];
      for (const item of uploaded) {
        const existing = findExisting(item.documentType);
        if (existing?.id) {
          const document = buildUpdateDocumentPayload({
            existingDoc: existing,
            tenantId,
            filestoreId: item.filestoreId,
            fileName: item.fileName,
          });
          await Digit.UserService.updateUserDocument(document, tenantId, userInfo.uuid);
        } else {
          toCreate.push(
            buildCreateDocumentPayload({
              userUuid: userInfo.uuid,
              tenantId,
              documentType: item.documentType,
              filestoreId: item.filestoreId,
              fileName: item.fileName,
            })
          );
        }
      }

      if (toCreate.length) {
        await Digit.UserService.createUserDocument(toCreate, tenantId);
      }

      await refreshDocuments();
      setFiles({ IDENTITY_PROOF: null, DOB_PROOF: null });
      showToast("success", t("PROFILE_DOCS_SAVED"));
    } catch (err) {
      showToast("error", t("CORE_COMMON_PROFILE_UPDATE_ERROR"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      {hasExisting ? (
        <div style={profileStyles.card}>
          {documents.map((doc, i) => (
            <div key={doc.id || i} style={{ width: "100%" }}>
              <div style={profileStyles.cardHeader}>
                <div style={profileStyles.cardLabel}>{t(`PROFILE_${doc.documentType}`)}</div>
                <div
                  style={{ display: "flex", alignItems: "center", gap: "6px", cursor: "pointer" }}
                  onClick={() => openDocument(doc.filestoreId, tenantId)}
                  title="Click to view document"
                >
                  <PDFSvg width={100} height={100} />
                  <span style={{ fontSize: 13, color: "#1D70B8", textDecoration: "underline" }}>
                    {t("PROFILE_VIEW_DOC")}
                  </span>
                </div>
              </div>
              <UploadFile
                accept={ACCEPTED}
                onUpload={(e) => handleUpload(e, doc.documentType)}
                onDelete={() => handleDelete(doc.documentType)}
                file={files[doc.documentType]}
                message={t("PROFILE_REUPLOAD_DOC")}
                iserror={errors[doc.documentType]}
              />
            </div>
          ))}
        </div>
      ) : (
        <>
          <p style={{ color: "#505A5F", marginBottom: 16 }}>{t("PROFILE_NO_DOCS_YET")}</p>
          <div style={profileStyles.card}>
            {DOC_TYPES.map(({ key, label }) => (
              <div key={key} style={{ width: "100%" }}>
                <div style={profileStyles.cardLabel}>
                  {t(label)} <span style={profileStyles.required}>*</span>
                </div>
                <UploadFile
                  accept={ACCEPTED}
                  onUpload={(e) => handleUpload(e, key)}
                  onDelete={() => handleDelete(key)}
                  file={files[key]}
                  message={t("PROFILE_NO_FILE_SELECTED")}
                  iserror={errors[key]}
                />
              </div>
            ))}
          </div>
        </>
      )}

      <div style={profileStyles.actions}>
        <SubmitBar label={t("CORE_COMMON_SAVE")} onSubmit={handleSave} disabled={saving} />
      </div>

      {toast && (
        <div className="profile-toast-container">
          <Toast error={toast.type === "error"} label={toast.msg} onClose={() => setToast(null)} />
        </div>
      )}
    </>
  );
};

export default EditDocuments;
