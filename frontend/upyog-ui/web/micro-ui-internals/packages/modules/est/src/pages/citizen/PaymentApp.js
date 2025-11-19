import React, { useState } from "react";
import { Toast } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import ESTPaymentApplication from "../../components/ESTPaymentApplication";

const PaymentApp = ({ path }) => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [showToast, setShowToast] = useState(null);

  // Mock payment data
  const dummyPaymentData = [
    {
      paymentId: "PAY-001",
      applicationNumber: "EST-APP-001",
      amount: "5000",
      paymentDate: "2023-12-15",
      status: "Success",
      paymentType: "Rent",
      receiptNumber: "RCP-001"
    },
    {
      paymentId: "PAY-002",
      applicationNumber: "EST-APP-002",
      amount: "4000",
      paymentDate: "2023-12-20",
      status: "Success",
      paymentType: "Lease",
      receiptNumber: "RCP-002"
    },
    {
      paymentId: "PAY-003",
      applicationNumber: "EST-APP-003",
      amount: "6000",
      paymentDate: "2023-12-25",
      status: "Failed",
      paymentType: "Rent",
      receiptNumber: "RCP-003"
    }
  ];

  const {
    isLoading,
    isSuccess,
    data: { PaymentHistory: searchResult, Count: count } = {},
  } = { 
    isLoading: false, 
    isSuccess: true, 
    data: { 
      PaymentHistory: dummyPaymentData, 
      Count: dummyPaymentData.length 
    } 
  };

  return (
    <React.Fragment>
      <ESTPaymentApplication
        t={t}
        isLoading={isLoading}
        tenantId={tenantId}
        setShowToast={setShowToast}
        data={isSuccess && !isLoading ? searchResult : []}
        count={count}
      />

      {showToast && (
        <Toast
          error={showToast.error}
          warning={showToast.warning}
          label={t(showToast.label)}
          isDleteBtn={true}
          onClose={() => {
            setShowToast(null);
          }}
        />
      )}
    </React.Fragment>
  );
};

export default PaymentApp;