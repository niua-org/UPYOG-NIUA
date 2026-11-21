import { Loader, Modal, FormComposer } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useState, useEffect } from "react";
import { configFamilyApproverApplication } from "../config";

/**
 * Heading title atom for modal header.
 *
 * @param {Object} props - Heading properties.
 * @param {string} props.label - Heading text.
 * @returns {React.ReactElement} Header text element.
 */
const Heading = (props) => <h1 className="heading-m">{props.label}</h1>;

/**
 * Close cross icon SVG.
 *
 * @returns {React.ReactElement} Close icon SVG.
 */
const Close = () => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#FFFFFF">
    <path d="M0 0h24v24H0V0z" fill="none" />
    <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z" />
  </svg>
);

/**
 * Close button wrapper atom.
 *
 * @param {Object} props - Button properties.
 * @param {Function} props.onClick - Click handler callback.
 * @returns {React.ReactElement} Close button element.
 */
const CloseBtn = (props) => (
  <div className="icon-bg-secondary" onClick={props.onClick}>
    <Close />
  </div>
);

/**
 * FamilyActionModal component for taking workflow actions (APPROVE, REJECT)
 * on Family Service applications. Handles comment input, optional/required document attachments,
 * and compiles the standard Family workflow update payload.
 *
 * @param {Object} props - Component properties.
 * @param {Function} props.t - Localization translation function.
 * @param {Object} props.action - Selected workflow action definition (e.g. APPROVE, REJECT).
 * @param {string} props.tenantId - Tenant identifier.
 * @param {Function} props.closeModal - Callback to dismiss the action modal.
 * @param {Function} props.submitAction - Callback to execute the mutation with compiled payload.
 * @param {Object} props.applicationData - Full family data object from search response.
 * @param {string} props.businessService - Business service identifier (family-service).
 * @param {string} props.moduleCode - Module identifier code (family-service).
 * @returns {React.ReactElement} Action modal dialog.
 */
const FamilyActionModal = ({
  t,
  action,
  tenantId,
  closeModal,
  submitAction,
  applicationData,
  businessService,
  moduleCode,
}) => {
  const [config, setConfig] = useState({});
  const [file, setFile] = useState(null);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [error, setError] = useState(null);
  const [isUploading, setIsUploading] = useState(false);

  /**
   * Handles local file selection and begins upload.
   *
   * @param {React.ChangeEvent<HTMLInputElement>} e - File change event.
   */
  function selectFile(e) {
    const selected = e.target.files?.[0];
    if (!selected) {
      setIsUploading(false);
      return;
    }
    setIsUploading(true);
    setFile(selected);
  }

  useEffect(() => {
    (async () => {
      setError(null);
      if (file) {
        if (file.size >= 5242880) {
          setError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
          setIsUploading(false);
        } else {
          try {
            const response = await Digit.UploadServices.Filestorage("family-service", file, tenantId);
            if (response?.data?.files?.length > 0) {
              setUploadedFile(response.data.files[0].fileStoreId);
            } else {
              setError(t("CS_FILE_UPLOAD_ERROR"));
            }
          } catch (err) {
            setError(t("CS_FILE_UPLOAD_ERROR"));
          } finally {
            setIsUploading(false);
          }
        }
      }
    })();
  }, [file]);

  useEffect(() => {
    if (action) {
      setConfig(
        configFamilyApproverApplication({ t, action, selectFile, uploadedFile, setUploadedFile, isUploading, error })
      );
    }
  }, [action, uploadedFile, isUploading, error]);

  /**
   * Compiles the Family workflow payload and invokes submitAction.
   *
   * @param {Object} data - Form data containing user comments.
   */
  function submit(data) {
    if (isUploading) return;
    if (action?.docUploadRequired && !uploadedFile) {
      setError(t("CS_DOCUMENT_UPLOAD_REQUIRED"));
      return;
    }
    const workflow = {
      action: action?.action,
      comment: data?.comments,
      businessService,
      moduleName: moduleCode,
    };
    if (uploadedFile) {
      workflow.documents = [
        {
          documentType: `${action?.action} DOC`,
          fileName: file?.name,
          fileStoreId: uploadedFile,
        },
      ];
    }
    const payload = {
      Family: {
        ...applicationData,
        workflow,
      },
    };
    submitAction(payload);
  }

  return action && config.form ? (
    <Modal
      headerBarMain={<Heading label={t(config.label.heading)} />}
      headerBarEnd={<CloseBtn onClick={closeModal} />}
      actionCancelLabel={t(config.label.cancel)}
      actionCancelOnSubmit={closeModal}
      actionSaveLabel={t(config.label.submit)}
      actionSaveOnSubmit={() => {}}
      formId="modal-action"
    >
      <FormComposer
        config={config.form}
        noBoxShadow
        inline
        childrenAtTheBottom
        onSubmit={submit}
        defaultValues={{}}
        formId="modal-action"
      />
    </Modal>
  ) : (
    <Loader />
  );
};

export default FamilyActionModal;
