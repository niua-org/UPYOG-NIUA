import React from "react";
import { Modal, StatusTable, Row, CloseSvg } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { formatDateValue } from "../utils";
import "../css/gc-inline-auto.css";

const CloseBtn = (props) => (
  <div className="icon-bg-secondary" onClick={props.onClick}>
    <CloseSvg />
  </div>
);

/**
 * GCDisconnectionHistoryModal Component
 * 
 * Displays full disconnection / reconnection service history for an application.
 * Extracts events from:
 * 1. application.additionalDetail.disconnectionHistory
 * 2. Active application.additionalDetail (disconnectionDate / disconnectionReason)
 * 3. Workflow timeline actions (DISCONNECT / RECONNECT)
 */
const GCPauseHistoryModal = ({ isOpen, onClose, application }) => {
  const { t } = useTranslation();

  if (!isOpen) return null;

  const additionalDetail = application?.additionalDetail || {};
  const disconnectionHistory = additionalDetail?.disconnectionHistory || [];
  const currentDisconnectionDate = additionalDetail.disconnectionDate;
  const currentDisconnectionReason = additionalDetail.disconnectionReason;
  const isDisconnected = application?.status === "DISCONNECTED";

  const hasHistory = disconnectionHistory.length > 0 || (isDisconnected && Boolean(currentDisconnectionDate));

  const formatDate = (dVal) => formatDateValue(dVal, t("CS_NA"));

  return (
    <div className="gc-payment-history-modal gc-disconnection-history-modal">
      <Modal
        headerBarMain={<h1 className="heading-m">{t("GC_DISCONNECTION_HISTORY_HEADER")}</h1>}
        headerBarEnd={<CloseBtn onClick={onClose} />}
        hideSubmit={true}
      >
        {hasHistory ? (
          <div className="gc-modal-content">
            {/* Active Disconnection if currently disconnected */}
            {isDisconnected && currentDisconnectionDate && (
              <div className="gc-receipt-card" style={{ borderLeft: "4px solid #d9534f" }}>
                <StatusTable className="gc-receipt-status-table">
                  <Row
                    className="border-none gc-receipt-row"
                    label={t("GC_SERVICE_STATUS_LABEL")}
                    text={<span className="gc-status-disconnected">{t("GC_STATUS_DISCONNECTED")}</span>}
                  />
                  <Row
                    className="border-none gc-receipt-row"
                    label={t("GC_DISCONNECTION_DATE_LABEL")}
                    text={formatDate(currentDisconnectionDate)}
                  />
                  {currentDisconnectionReason && (
                    <Row
                      className="border-none gc-receipt-row"
                      label={t("GC_REASON_FOR_DISCONNECTION")}
                      text={currentDisconnectionReason}
                    />
                  )}
                </StatusTable>
              </div>
            )}

            {/* Past Disconnection Cycles from disconnectionHistory */}
            {disconnectionHistory.map((item, index) => (
              <div key={index} className="gc-receipt-card">
                <StatusTable className="gc-receipt-status-table">
                  <Row
                    className="border-none gc-receipt-row"
                    label={t("GC_CYCLE_LABEL")}
                    text={`#${disconnectionHistory.length - index}`}
                  />
                  <Row
                    className="border-none gc-receipt-row"
                    label={t("GC_DISCONNECTION_DATE_LABEL")}
                    text={formatDate(item.disconnectionDate)}
                  />
                  <Row
                    className="border-none gc-receipt-row"
                    label={t("GC_RECONNECTION_DATE_LABEL")}
                    text={formatDate(item.reconnectionDate)}
                  />
                  {item.disconnectionReason && (
                    <Row
                      className="border-none gc-receipt-row"
                      label={t("GC_REASON_FOR_DISCONNECTION")}
                      text={item.disconnectionReason}
                    />
                  )}
                </StatusTable>
              </div>
            ))}
          </div>
        ) : (
          <div className="gc-no-history-text" style={{ textAlign: "center", padding: "20px" }}>
            {t("GC_NO_DISCONNECTION_HISTORY_FOUND")}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default GCPauseHistoryModal;
