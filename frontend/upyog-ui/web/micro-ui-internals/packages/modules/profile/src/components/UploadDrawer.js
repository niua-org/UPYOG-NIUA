import React, { useState, useEffect } from "react";
import { GalleryIcon, RemoveIcon, UploadFile } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";

function UploadDrawer({ setProfilePic, closeDrawer, userType, removeProfilePic ,showToast}) {
  const [uploadedFile, setUploadedFile] = useState(null);
  const [file, setFile] = useState("");
  const [error, setError] = useState(null);
  const { t } = useTranslation();
  const selectfile = (e) => {
    const selected = e.target.files?.[0];
    if (e.target) e.target.value = "";
    if (selected) setFile(selected);
  };
  const removeimg = () => {removeProfilePic(); closeDrawer()};
  const onOverlayBodyClick = () => closeDrawer();

  const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

  useEffect(() => {
    (async () => {
      setError(null);
      if (file) {
        if (file.size > MAX_FILE_SIZE) {
          const maxErrMsg = t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED", "File size should be less than 5MB");
          showToast("error", maxErrMsg);
          setError(maxErrMsg);
          setFile("");
          return;
        }
        try {
          const tenant = Digit.ULBService.getStateId() || Digit.ULBService.getCurrentTenantId() || "pg";
          const response = await Digit.UploadServices.Filestorage(`${userType}-profile`, file, tenant);
          if (response?.data?.files?.length > 0) {
            const fileStoreId = response?.data?.files[0]?.fileStoreId;
            setUploadedFile(fileStoreId);
            setProfilePic(fileStoreId);
          } else {
            showToast("error", t("CORE_COMMON_PROFILE_FILE_UPLOAD_ERROR", "An error occurred while uploading the file"));
            setError(t("CORE_COMMON_PROFILE_FILE_UPLOAD_ERROR", "An error occurred while uploading the file"));
          }
        } catch (err) {
          showToast("error", t("CORE_COMMON_PROFILE_INVALID_FILE_INPUT", "Invalid file or upload failed"));
        }
      }
    })();
  }, [file]);

  return (
    <React.Fragment>
      <div
        className="core-upload-drawer-fullwidth"
        onClick={onOverlayBodyClick}
      ></div>
      <div className={`upload-drawer-bottom-bar ${userType === "citizen" ? "bottom-citizen" : "bottom-employee"}`}>
        <div
          className="core-upload-drawer-fullwidth-2"
        >
          <label htmlFor="file" className="core-upload-drawer-clickable">
            {" "}
            <GalleryIcon />
          </label>
          <label className="core-upload-drawer-clickable">{t("CORE_COMMON_GALLERY")}</label>
          <input type="file" id="file" accept="image/*, .png, .jpeg, .jpg" onChange={selectfile} className="core-upload-drawer-hidden" />
        </div>

        <div
          className="core-upload-drawer-fullwidth-3"
        >
          <button onClick={removeimg}>
            <RemoveIcon />
          </button>
          <label className="core-upload-drawer-clickable">{t("CORE_COMMON_REMOVE")}</label>
        </div>
      </div>
    </React.Fragment>
  );
}

export default UploadDrawer;
