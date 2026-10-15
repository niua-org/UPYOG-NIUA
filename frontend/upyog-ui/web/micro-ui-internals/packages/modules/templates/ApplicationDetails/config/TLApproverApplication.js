import { Dropdown, UploadFile } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";

export const configTLApproverApplication = ({
  t,
  action,
  approvers,
  selectedApprover,
  setSelectedApprover,
  selectFile,
  uploadedFile,
  setUploadedFile,
  assigneeLabel,
  businessService,
}) => {
  let checkCondtions = true;
  if (action?.action == "SENDBACKTOCITIZEN" || action?.action == "APPROVE") checkCondtions = false;
  if (action.isTerminateState) checkCondtions = false;

  return {
    label: {
      heading: `WF_${action?.action}_APPLICATION`,
      submit: `WF_${businessService?.toUpperCase()}_${action?.action}`,
      cancel: "WF_EMPLOYEE_NEWTL_CANCEL",
    },
    form: [
      {
        body: [
          {
            label: !checkCondtions ? null : t("WF_ASSIGNEE_NAME_LABEL"),
            placeholder: !checkCondtions ? null : t("WF_ASSIGNEE_NAME_PLACEHOLDER"),
            // isMandatory: false,
            type: "dropdown",
            populators: !checkCondtions ? null : (
              <Dropdown
                option={approvers}
                autoComplete="off"
                optionKey="name"
                id="fieldInspector"
                select={setSelectedApprover}
                selected={selectedApprover}
              />
            ),
          },
          {
            // A blank or one-character note is not a review comment.
            label: `${t("WF_COMMON_COMMENTS")} *`,
            type: "textarea",
            populators: {
              name: "comments",
              validation: {
                required: "A review comment is required",
                validate: (value) => {
                  const text = String(value ?? "").trim();
                  // Spaces alone do not count. One letter is not a review comment.
                  if (!text) return "A review comment is required";
                  if (text.length < 2) return "A single character is not a valid review comment";
                  return true;
                },
              },
            },
          },
          {
            label: t("TL_APPROVAL_CHECKLIST_BUTTON_UP_FILE"),
            populators: (
              <UploadFile
                id={"workflow-doc"}
                // accept=".jpg"
                onUpload={selectFile}
                onDelete={() => {
                  setUploadedFile(null);
                }}
                message={uploadedFile ? `1 ${t(`ES_PT_ACTION_FILEUPLOADED`)}` : t(`CS_ACTION_NO_FILEUPLOADED`)}
              />
            )
          },
          //   {
          //     label: action.docUploadRequired ? t("ES_PT_UPLOAD_FILE") : null,
          //     populators: action.docUploadRequired ? (
          //       <UploadFile
          //         // accept=".jpg"
          //         onUpload={selectFile}
          //         onDelete={() => {
          //           setUploadedFile(null);
          //         }}
          //         message={uploadedFile ? `1 ${t(`ES_PT_ACTION_FILEUPLOADED`)}` : t(`ES_PT_ACTION_NO_FILEUPLOADED`)}
          //       />
          //     ) : null,
          //   },
        ],
      },
    ],
  };
};
