/**
 * ChallanPayments Component
 * -----------------------------------------------------------------------------
 * Represents an individual Challan payment card inside the citizen's "My Payments" list.
 * Displays summary metadata including:
 *  - Challan Amount (extracted robustly across multiple data structures)
 *  - Challan Number
 *  - Offender / Citizen Name
 *  - Creation Date (formatted from Epoch timestamp)
 *  - Current Payment Status (PAID / UNPAID)
 * 
 * Provides interactive action buttons:
 *  - "Challan Summary": Navigates to citizen details view (/details/:challanNo).
 *  - "Pay Now": Available only if status is unpaid, directs to citizen payment checkout.
 * -----------------------------------------------------------------------------
 */

import React from "react";
import { Card, KeyNote, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { printReciept } from "../../../utils";

const convertEpochToDate = (dateEpoch) => {
  if (!dateEpoch) return null;
  const date = new Date(dateEpoch);
  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
};

const getChallanAmount = (payment) => {
  console.log("payment data ", payment);
  return payment?.challanAmount || 0;
};



const ChallanPayments = ({ payment, receiptData }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const tenantId =  Digit.ULBService.getCurrentTenantId();

  const challanNo = payment?.challanNo;
  const isPaid = payment?.applicationStatus === "PAID" || !!receiptData;
  const status = isPaid ? "PAID" : (payment?.applicationStatus || "UNPAID");

  const handleMakePayment = () => {
    navigate(`/upyog-ui/citizen/payment/my-bills/Challan_Generation/${challanNo}`, {
      state: { tenantId: tenantId, fromSearchResults: true },
    });
  };

  const handleDownloadReceipt = async () => {
    try {
      await printReciept("Challan_Generation", challanNo);
    } catch (e) {
      console.error("Failed to download receipt:", e);
    }
  };

  const handleViewDetails = () => {
    navigate(`/upyog-ui/citizen/challangeneration/details/${challanNo}`, {
      state: { challan: { ...payment, applicationStatus: isPaid ? "PAID" : status } },
    });
  };

  const amount = getChallanAmount(payment);
  const name = payment?.citizen?.name || t("CS_NA");
  const date = convertEpochToDate(payment?.auditDetails?.createdTime) || t("CS_NA");

  return (
    <Card style={{ marginBottom: "16px" }}>
      <KeyNote
        noteStyle={{ fontSize: "24px", fontWeight: "700", color: "#0B0C0C" }}
        keyValue={t("CHALLAN_AMOUNT")}
        note={`₹ ${amount}`}
      />
      <KeyNote keyValue={t("CHALLAN_NUMBER_LABEL")} note={challanNo || t("CS_NA")} />
      <KeyNote keyValue={t("CHALLAN_OWNER_NAME")} note={name} />
      <KeyNote keyValue={t("CHALLAN_CREATED_DATE")} note={date} />
      <KeyNote keyValue={t("CHALLAN_STATUS")} note={t(`CHALLAN_STATUS_${status}`, status)} />

      <div style={{ display: "flex", gap: "16px", marginTop: "16px" }}>
        <SubmitBar
          label={t("CHALLAN_SUMMARY")}
          onSubmit={handleViewDetails}
          style={{ width: "fit-content", minWidth: "200px" }}
        />
        {!isPaid && (
          <SubmitBar
            label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")}
            onSubmit={handleMakePayment}
            style={{ width: "fit-content", minWidth: "200px" }}
          />
        )}
      </div>
    </Card>
  );
};

export default ChallanPayments;
