import { Card, KeyNote } from "@upyog/digit-ui-react-components";
import React from "react";

const EstatePayment = ({ payment }) => {
  return (
    <Card style={{ marginBottom: "20px" }}>
      <KeyNote keyValue="Payment ID" note={payment.paymentId} />
      <KeyNote keyValue="Application Number" note={payment.applicationNumber} />
      <KeyNote keyValue="Amount" note={`₹${payment.amount}`} />
      <KeyNote keyValue="Payment Date" note={payment.paymentDate} />
      <KeyNote keyValue="Payment Type" note={payment.paymentType} />
      <KeyNote keyValue="Status" note={payment.status} />
    </Card>
  );
};

export default EstatePayment;
