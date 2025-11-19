import { Card, Header, KeyNote, Loader, Table } from "@upyog/digit-ui-react-components";
import React, { useState, useMemo } from "react";
import { useTranslation } from "react-i18next";

export const ESTPaymentHistory = () => {
  const { t } = useTranslation();
  
  // Mock data
  const isLoading = false;
  const paymentsList = [
    {
      paymentId: "PAY-001",
      applicationNumber: "EST-APP-001",
      totalAmountPaid: 5000,
      receiptDate: "2023-12-15",
      receiptNumber: "RCP-001",
      paymentType: "Rent",
      status: "Success"
    },
    {
      paymentId: "PAY-002", 
      applicationNumber: "EST-APP-002",
      totalAmountPaid: 4000,
      receiptDate: "2023-12-20",
      receiptNumber: "RCP-002",
      paymentType: "Lease",
      status: "Success"
    }
  ];
  
  if (isLoading) {
    return <Loader />;
  }

  return (
    <React.Fragment>
      <Header>Payment History ({paymentsList.length})</Header>
      
      {paymentsList?.length > 0 ? (
        <div>
          {paymentsList.map((payment, index) => (
            <Card key={index} style={{ marginBottom: "20px" }}>
              <KeyNote keyvalue="Payment ID" note={payment.paymentId}/>
              <KeyNote keyvalue="Application Number" note={payment.applicationNumber}/>
              <KeyNote keyvalue="Total Amount Paid" note={payment.totalAmountPaid}/>
              <KeyNote keyvalue="Receipt Date" note={payment.receiptDate}/>
              <KeyNote keyvalue="Receipt Number" note={payment.receiptNumber}/>
              <KeyNote keyvalue="Payment Type" note={payment.paymentType}/>
              <KeyNote keyvalue="Status" note={payment.status}/>
            </Card>
          ))}
        </div>
      ) : (
        <div style={{ padding: "20px", textAlign: "center" }}>
          <p>No payments found</p>
        </div>
      )}
    </React.Fragment>
  );
};

export default ESTPaymentHistory;
