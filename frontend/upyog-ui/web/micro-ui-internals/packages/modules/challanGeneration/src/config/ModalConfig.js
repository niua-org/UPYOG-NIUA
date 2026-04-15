import React from "react";
import { UploadFile } from "@upyog/digit-ui-react-components";

/**
 * ModalConfig:
 * - Generates config for fee waiver modal
 * - Includes amount input and file upload
 */

export const ModalConfig = ({ t, action, setAmount, getChallanData, selectFile, setUploadedFile, uploadedFile }) => {
  const finalAmount = Math.max(getChallanData?.amount?.[0]?.amount || 0, getChallanData?.challanAmount || 0);
  return {
    label: {
      heading: ``,
      submit: `${action?.action}`,
      cancel: "WF_EMPLOYEE_NEWTL_CANCEL",
    },
    form: [
      {
        body: [
          {
            label: `${t("FEE_WAIVER_AMOUNT")} *`,
            populators: (
              <div style={{ marginBottom: "20px" }}>
                <input
                  className="employee-card-input focus-visible"
                  type="number"
                  style={{ marginBottom: 0, width: "100%" }}
                  onChange={(e) => setAmount(e.target.value)}
                  onWheel={(e) => e.target.blur()}
                  onKeyDown={(e) => {
                    if (e.key === "ArrowUp" || e.key === "ArrowDown") {
                      e.preventDefault();
                    }
                  }}
                />
                <span style={{ color: "green" }}>
                  <span style={{ color: " red", paddingRight: " 3px" }}>Note:</span>Please enter amount less than{" "}
                  <span style={{ fontWeight: "bolder", color: "green" }}> {finalAmount}</span>{" "}
                </span>
              </div>
            ),
          },
          {
            label: t("TL_APPROVAL_CHECKLIST_BUTTON_UP_FILE"),
            populators: (
              <div>
                <UploadFile
                  id={"workflow-doc"}
                  onUpload={selectFile}
                  onDelete={() => {
                    setUploadedFile(null);
                  }}
                  message={uploadedFile ? `1 ${t(`ES_PT_ACTION_FILEUPLOADED`)}` : t(`CS_ACTION_NO_FILEUPLOADED`)}
                />
              </div>
            ),
          },
        ],
      },
    ],
  };
};
