import React from "react";
import { Header, Card, KeyNote } from "@upyog/digit-ui-react-components";

const PaymentHistory = () => {
  const payments = [
    {
      paymentId: "PAY-001",
      applicationNumber: "EST-APP-001",
      amount: "50000 INR",
      status: "Success"
    },
    {
      paymentId: "PAY-002",
      applicationNumber: "EST-APP-002",
      amount: "40000 INR", 
      status: "Success"
    }
  ];

  return (
    <div>
      <Header>Payment History</Header>
      {payments.map((payment, index) => (
        <Card key={index}>
          <KeyNote keyValue="Payment ID" note={payment.paymentId} />
          <KeyNote keyValue="Application Number" note={payment.applicationNumber} />
          <KeyNote keyValue="Amount" note={payment.amount} />
          <KeyNote keyValue="Status" note={payment.status} />
        </Card>
      ))}
    </div>
  );
};

export default PaymentHistory;
