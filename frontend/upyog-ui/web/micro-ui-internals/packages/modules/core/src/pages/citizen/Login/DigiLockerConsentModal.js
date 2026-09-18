import { Card, Modal } from "@nudmcdgnpm/digit-ui-react-components";

const CloseIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#FFFFFF">
    <path d="M0 0h24v24H0V0z" fill="none" />
    <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z" />
  </svg>
);

// V1 and V2 must obtain the same explicit user consent before redirecting to
// DigiLocker, so the modal is shared rather than reimplemented by each UI.
const DigiLockerConsentModal = ({ isOpen, onCancel, onConfirm, t }) => {
  if (!isOpen) return null;

  return (
    <Modal
      headerBarMain={<h1 className="heading-m">{t("Consent")}</h1>}
      headerBarEnd={
        <div className="icon-bg-secondary" onClick={onCancel}>
          <CloseIcon />
        </div>
      }
      actionCancelLabel="Cancel"
      actionCancelOnSubmit={onCancel}
      actionSaveLabel="Ok"
      actionSaveOnSubmit={onConfirm}
      formId="modal-action"
    >
      <div style={{ width: "100%" }}>
        <Card>
          <p>By selecting this option, I am providing my consent to associate my Upyog account with my DigiLocker ID</p>
        </Card>
      </div>
    </Modal>
  );
};

export default DigiLockerConsentModal;
