import React, { useState, useEffect } from "react";
import { Modal, StatusTable, Row, CloseSvg, LinkButton } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { downloadGCReceipt } from "../utils";
import "../css/gc-inline-auto.css";

const CloseBtn = (props) => (
  <div className="icon-bg-secondary" onClick={props.onClick}>
    <CloseSvg />
  </div>
);

const initialLimit = 5;

/**
 * GCPaymentHistoryModal Component
 * 
 * Reusable modal popup to display previous bills and payment history
 * for both Citizen and Employee views.
 * Supports "Load More" pagination to comfortably display multiple years of payment history.
 */
const GCPaymentHistoryModal = ({ isOpen, onClose, payments = [], tenantId }) => {
  const { t } = useTranslation();
  const [visibleCount, setVisibleCount] = useState(initialLimit);

  useEffect(() => {
    if (isOpen) {
      setVisibleCount(initialLimit);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const visiblePayments = payments.slice(0, visibleCount);
  const hasMore = payments.length > visibleCount;

  const handleLoadMore = () => {
    setVisibleCount((prev) => prev + initialLimit);
  };

  return (
    <div className="gc-payment-history-modal">
      <Modal
        headerBarMain={<h1 className="heading-m">{t("GC_PAYMENT_HISTORY_HEADER")}</h1>}
        headerBarEnd={<CloseBtn onClick={onClose} />}
        hideSubmit={true}
      >
        {payments && payments.length > 0 ? (
          <div className="gc-modal-content">
            {visiblePayments.map((payment, index) => {
              const detail = payment?.paymentDetails?.[0];
              const receiptNo = detail?.receiptNumber;
              const receiptDate = detail?.receiptDate ? Digit.DateUtils.ConvertEpochToDate(detail.receiptDate) : t("CS_NA");
              const amountPaid = payment?.totalAmountPaid ?? 0;
              const paymentMode = payment?.paymentMode ? t(`PAYMENT_MODE_${payment.paymentMode}`) : t("CS_NA");
              const txnId = payment?.transactionNumber;
              const billNo = detail?.bill?.billNumber;

              return (
                <div key={payment.id || index} className="gc-receipt-card">
                  <StatusTable className="gc-receipt-status-table">
                    <Row
                      className="border-none gc-receipt-row"
                      label={t("GC_RECEIPT_NUMBER_LABEL")}
                      text={receiptNo || t("CS_NA")}
                    />
                    <Row
                      className="border-none gc-receipt-row"
                      label={t("GC_PAYMENT_DATE_LABEL")}
                      text={receiptDate}
                    />
                    <Row
                      className="border-none gc-receipt-row"
                      label={t("GC_PAID_AMOUNT_LABEL")}
                      text={`₹ ${amountPaid}`}
                    />
                    <Row
                      className="border-none gc-receipt-row"
                      label={t("GC_PAYMENT_MODE_LABEL")}
                      text={paymentMode}
                    />
                    {txnId && (
                      <Row
                        className="border-none gc-receipt-row"
                        label={t("GC_TRANSACTION_ID_LABEL")}
                        text={txnId}
                      />
                    )}
                    {billNo && (
                      <Row
                        className="border-none gc-receipt-row"
                        label={t("GC_BILL_NUMBER_LABEL")}
                        text={billNo}
                      />
                    )}
                    <Row
                      className="border-none gc-receipt-row"
                      label={t("GC_ACTION_LABEL")}
                      text={
                        <span
                          className="gc-payment-history-link"
                          onClick={() => downloadGCReceipt(payment.tenantId || tenantId, payment)}
                        >
                          {t("GC_DOWNLOAD_RECEIPT")}
                        </span>
                      }
                    />
                  </StatusTable>
                </div>
              );
            })}

            {hasMore && (
              <div className="gc-load-more-container">
                <LinkButton
                  label={`${t("CS_COMMON_LOAD_MORE")} (${visiblePayments.length}/${payments.length})`}
                  onClick={handleLoadMore}
                />
              </div>
            )}
          </div>
        ) : (
          <div className="gc-no-history-text">
            {t("GC_NO_PAYMENT_HISTORY_FOUND")}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default GCPaymentHistoryModal;
