import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { UploadFile, Toast, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "../utils/profileStyles";

/*
 * EditDocuments
 *
 * Handles Identity Proof and DOB Proof upload for the user profile.
 * If user already has documents (from userDetails.documents), shows them with re-upload option.
 * If no documents exist, shows upload fields directly.
 * On save — uploads files to filestore first, then:
 *   - If existing docs: calls updateUser with documents array in payload
 *   - If new docs: calls createUserDocument API
 */


const MAX_SIZE = 5 * 1024 * 1024;
const ACCEPTED = ".jpg,.jpeg,.png,.pdf";
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/jpg", "application/pdf"];

const DOC_TYPES = [
  { key: "IDENTITY_PROOF", label: "PROFILE_IDENTITY_PROOF" },
  { key: "DOB_PROOF",      label: "PROFILE_DOB_PROOF"      },
];

const EditDocuments = ({ stateCode, userDetails }) => {
  const { t } = useTranslation();
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const userInfo = Digit.UserService.getUser()?.info || {};

  const existingDocs = userDetails?.documents || [];
  const hasExisting = existingDocs.length > 0;

  // file state per doc type: { IDENTITY_PROOF: File|null, DOB_PROOF: File|null }
  const [files, setFiles] = useState({ IDENTITY_PROOF: null, DOB_PROOF: null });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (type, msg) => {
    setToast({ type, msg });
    setTimeout(() => setToast(null), 5000);
  };

  const validateFile = (file, key) => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      setErrors(prev => ({ ...prev, [key]: t("PROFILE_DOC_INVALID_TYPE") }));
      return false;
    }
    if (file.size > MAX_SIZE) {
      setErrors(prev => ({ ...prev, [key]: t("PROFILE_DOC_SIZE_EXCEEDED") }));
      return false;
    }
    setErrors(prev => ({ ...prev, [key]: null }));
    return true;
  };

  const handleUpload = (e, key) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!validateFile(file, key)) return;
    setFiles(prev => ({ ...prev, [key]: file }));
  };

  const handleDelete = (key) => {
    setFiles(prev => ({ ...prev, [key]: null }));
    setErrors(prev => ({ ...prev, [key]: null }));
  };

  const uploadToFilestore = async (file) => {
    const res = await Digit.UploadServices.Filestorage("profile", file, stateId);
    return res?.data?.files?.[0]?.fileStoreId || null;
  };

  const handleSave = async () => {
    const toUpload = DOC_TYPES.filter(({ key }) => files[key]);
    if (!toUpload.length) {
      showToast("error", t("PROFILE_NO_DOC_SELECTED"));
      return;
    }
    setSaving(true);
    try {
      // upload all selected files to filestore
      const uploaded = await Promise.all(
        toUpload.map(async ({ key }) => ({
          documentType: key,
          filestoreId: await uploadToFilestore(files[key]),
        }))
      );

      if (hasExisting) {
        // update flow — send documents in updateUser payload
        const sessionUser = Digit.UserService.getUser();
        const payload = {
          ...userInfo,
          documents: uploaded,
        };
        const { responseInfo } = await Digit.UserService.updateUser(payload, stateCode);
        if (responseInfo?.status === "200") showToast("success", t("PROFILE_DOCS_SAVED"));
      } else {
        // create flow — new document create API
        const documents = uploaded.map(({ documentType, filestoreId }) => ({
          userUuid: userInfo.uuid,
          tenantId,
          documentType,
          filestoreId,
          active: true,
        }));
        await Digit.UserService.createUserDocument(documents, stateId);
        showToast("success", t("PROFILE_DOCS_SAVED"));
      }
    } catch (err) {
      showToast("error", t("CORE_COMMON_PROFILE_UPDATE_ERROR"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      {hasExisting ? (
        // show existing docs with re-upload option
        <div style={profileStyles.card}>
          {existingDocs.map((doc, i) => (
            <div key={i} style={{ width: "100%" }}>
              <div style={profileStyles.cardHeader}>
                <div style={profileStyles.cardLabel}>{t(`PROFILE_${doc.documentType}`)}</div>
                <a href={doc.fileUrl} target="_blank" rel="noreferrer" style={{ fontSize: 13 }}>
                  {t("PROFILE_VIEW_DOC")} 📄
                </a>
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
        // no existing docs — show upload fields
        <>
          <p style={{ color: "#505A5F", marginBottom: 16 }}>{t("PROFILE_NO_DOCS_YET")}</p>
          <div style={profileStyles.card}>
            {DOC_TYPES.map(({ key, label }) => (
              <div key={key} style={{ width: "100%" }}>
                <div style={profileStyles.cardLabel}>{t(label)} <span style={profileStyles.required}>*</span></div>
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
            onClose={() => setToast(null)}
          />
        </div>
      )}
    </>
  );
};

export default EditDocuments;
