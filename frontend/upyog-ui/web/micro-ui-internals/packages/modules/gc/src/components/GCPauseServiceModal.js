import React, { useState } from "react";
import { Modal, CardLabel, CardLabelError, CloseSvg, TextInput, InfoIcon, DatePicker } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import "../css/gc-inline-auto.css";

const CloseBtn = (props) => (
  <div className="icon-bg-secondary" onClick={props.onClick}>
    <CloseSvg />
  </div>
);

/**
 * GCDisconnectionModal Component
 * 
 * Allows citizens and employees to request disconnection of garbage collection service.
 * Pre-fills the next day as the effective disconnection date (non-changeable).
 * Requires paying pending dues before disconnecting the service.
 */
const GCPauseServiceModal = ({ isOpen, onClose, onConfirm, paymentAmount, paymentStatus }) => {
  const { t } = useTranslation();

  // Next date (tomorrow) in YYYY-MM-DD format
  const getNextDate = () => {
    const nextDay = new Date();
    nextDay.setDate(nextDay.getDate() + 1);
    const yyyy = nextDay.getFullYear();
    const mm = String(nextDay.getMonth() + 1).padStart(2, "0");
    const dd = String(nextDay.getDate()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}`;
  };

  const disconnectionDate = getNextDate();
  const [reason, setReason] = useState("");
  const [errors, setErrors] = useState({});

  if (!isOpen) return null;

  const currentBill = Number(paymentAmount) || 0;
  const hasPendingPayment = currentBill > 0 || paymentStatus === "PENDING_FOR_PAYMENT";

  const handleSubmit = () => {
    if (!reason || !reason.trim()) {
      setErrors({ reason: t("GC_DISCONNECTION_REASON_REQUIRED") });
      return;
    }

    setErrors({});
    onConfirm({
      disconnectionDate,
      reason: reason.trim(),
      hasPendingPayment,
    });
  };

  const getActionButtonLabel = () => {
    if (hasPendingPayment && currentBill > 0) {
      return `${t("GC_PAY_PREFIX")} ₹ ${currentBill} & ${t("GC_DISCONNECT_SERVICE_LABEL")}`;
    }
    return t("GC_CONFIRM_DISCONNECT");
  };

  return (
    <div className="gc-disconnect-service-modal">
      <Modal
        headerBarMain={<h1 className="heading-m">{t("GC_DISCONNECT_SERVICE_HEADER")}</h1>}
        headerBarEnd={<CloseBtn onClick={onClose} />}
        actionCancelLabel={t("CS_COMMON_CANCEL")}
        actionCancelOnSubmit={onClose}
        actionSaveLabel={getActionButtonLabel()}
        actionSaveOnSubmit={handleSubmit}
        formId="gc-disconnect-service-form"
      >
        <div className="gc-disconnect-modal-body">
          <p className="gc-disconnect-modal-desc">
            {t("GC_DISCONNECT_SERVICE_INSTRUCTIONS")}
          </p>

          {/* Current Bill Due Notice */}
          {hasPendingPayment && (
            <div className="gc-disconnect-payment-box">
              <div className="gc-disconnect-payment-row">
                <span className="gc-disconnect-payment-title">{t("GC_CURRENT_BILL_DUE")}:</span>
                <span className="gc-disconnect-payment-amount">₹ {currentBill}</span>
              </div>
              <p className="gc-disconnect-payment-note">
                {t("GC_DISCONNECT_PAY_CURRENT_BILL_NOTE")}
              </p>
            </div>
          )}

          {/* Disconnection Date */}
          <div className="gc-form-field">
            <div className="gc-field-label-wrapper">
              <CardLabel className="gc-field-label">
                {t("GC_DISCONNECTION_DATE")} <span className="gc-mandatory-star">*</span>
              </CardLabel>
              <div className="tooltip gc-info-tooltip">
                <InfoIcon />
                <span className="tooltiptext">{t("GC_DISCONNECTION_NEXT_DATE_TOOLTIP")}</span>
              </div>
            </div>
            <DatePicker
              date={disconnectionDate}
              disabled={true}
              onChange={() => {}}
            />
          </div>

          {/* Reason for Disconnection */}
          <div className="gc-form-field">
            <CardLabel className="gc-field-label">
              {t("GC_REASON_FOR_DISCONNECTION")} <span className="gc-mandatory-star">*</span>
            </CardLabel>
            <TextInput
              type="text"
              name="disconnectionReason"
              value={reason}
              onChange={(e) => {
                setReason(e.target.value);
                if (errors.reason) setErrors({});
              }}
              placeholder={t("GC_ENTER_REASON_PLACEHOLDER")}
            />
            {errors.reason && <CardLabelError>{errors.reason}</CardLabelError>}
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default GCPauseServiceModal;
