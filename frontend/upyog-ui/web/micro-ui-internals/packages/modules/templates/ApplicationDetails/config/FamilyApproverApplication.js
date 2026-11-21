import { UploadFile, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";

/**
 * Builds the FormComposer modal configuration for Family Service workflow actions.
 * Configures comments textarea and file upload field based on action requirements.
 *
 * @param {Object} params - Configuration parameters.
 * @param {Function} params.t - Localization translation function.
 * @param {Object} params.action - Workflow action descriptor.
 * @param {Function} params.selectFile - File selection event handler.
 * @param {string|null} params.uploadedFile - File store ID of uploaded document.
 * @param {Function} params.setUploadedFile - State updater to reset uploaded file.
 * @param {boolean} params.isUploading - Whether file upload is in progress.
 * @param {string|null} [params.error] - Upload error message if any.
 * @returns {Object} FormComposer configuration object.
 */
export const configFamilyApproverApplication = ({
  t,
  action,
  selectFile,
  uploadedFile,
  setUploadedFile,
  isUploading,
  error,
}) => {
  const LoadingSpinner = () => <div className="loading-spinner" />;
  return {
    label: {
      heading: `WF_FAMILY_${action?.action}`,
      submit: `WF_FAMILY_${action?.action}`,
      cancel: "CS_COMMON_BACK",
    },
    form: [
      {
        body: [
          {
            label: t("ES_FAMILY_ACTION_COMMENTS") + " *",
            type: "textarea",
            populators: {
              name: "comments",
              validation: { required: true },
            },
          },
          {
            label: `${t("ES_FAMILY_ATTACH_FILE")}${action?.docUploadRequired ? " *" : ""}`,
            populators: (
              <div>
                <UploadFile
                  id={"workflow-doc"}
                  onUpload={selectFile}
                  onDelete={() => setUploadedFile(null)}
                  message={
                    isUploading ? (
                      <div className="tmpl-application-details-content-flex-row">
                        <LoadingSpinner />
                        <span>{t("CS_UPLOADING")}</span>
                      </div>
                    ) : uploadedFile ? (
                      `1 ${t("CS_ACTION_FILEUPLOADED")}`
                    ) : (
                      t("CS_ACTION_NO_FILEUPLOADED")
                    )
                  }
                />
                {error && <CardLabelError>{error}</CardLabelError>}
              </div>
            ),
          },
        ],
      },
    ],
  };
};
