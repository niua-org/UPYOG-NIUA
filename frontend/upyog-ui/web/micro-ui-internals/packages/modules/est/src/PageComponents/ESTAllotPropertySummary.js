import React, { useState } from "react";
import {
  Card,
  CardHeader,
  CardSubHeader,
  CheckBox,
  SubmitBar,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";

const ESTPropertyAllotteeDetails = ({ onSubmit, onEdit }) => {
  const { t } = useTranslation();
  const [agree, setAgree] = useState(false);

  const handleAgree = () => setAgree(!agree);

  // Dummy data for demonstration
  const assetDetails = {
    assetNumber: "AS-12345000022BB",
    buildingName: "ABC Bhawan",
    locality: "BCD Nagar",
    totalArea: "500 sq. ft.",
    floor: "02",
    rate: "400/ sq. ft.",
    assetRefNumber: "AS-12345000022BB",
    assetType: "Rent",
  };

  const allotteeDetails = {
    name: "Rahul Sharma",
    phone: "8888000999",
    altPhone: "8888000877",
    email: "Rahul@gamil.com",
  };

  const agreementDetails = {
    duration: "10 years",
    rate: "Rs. 500/ sq. ft.",
    monthlyRent: "Rs.15000",
    advancePayment: "5,00,000",
    startDate: "11/03/2025",
    endDate: "11/02/2035",
    advancePaymentDate: "11/03/2025",
  };

  const documentDetails = {
    officeFile: "File number",
    citizenRequestLetter: "File name",
    allotmentLetter: "File name",
    signedDeed: "File name",
  };

  const renderRow = (label, value) => (
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        padding: "8px 0",
        borderBottom: "1px solid #e0e0e0",
      }}
    >
      <div style={{ width: "50%", fontWeight: 500, color: "#333" }}>{t(label)}</div>
      <div style={{ width: "50%", color: "#555" }}>{value}</div>
    </div>
  );

  return (
    <React.Fragment>
      <Card>
        {/* Header with Edit Button on Right */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            paddingRight: "16px",
            width: "auto",
          }}
        >
          <CardHeader>{t("EST_ALLOT_PROPERTY_SUMMARY")}</CardHeader>

          <div style={{ width: "auto" }}>
            <SubmitBar
              label={t("Edit")}
              onSubmit={onEdit}
              style={{ minWidth: "100px" }}
            />
          </div>
        </div>


        {/* Asset Details */}
        <CardSubHeader>{t("EST_ASSET_DETAILS")}</CardSubHeader>
        <div style={{ padding: "10px 20px" }}>
          {renderRow("EST_ASSET_NUMBER", assetDetails.assetNumber)}
          {renderRow("EST_BUILDING_NAME", assetDetails.buildingName)}
          {renderRow("EST_LOCALITY", assetDetails.locality)}
          {renderRow("EST_TOTAL_AREA", assetDetails.totalArea)}
          {renderRow("EST_FLOOR", assetDetails.floor)}
          {renderRow("EST_RATE", assetDetails.rate)}
          {renderRow("EST_ASSET_REFERENCE_NUMBER", assetDetails.assetRefNumber)}
          {renderRow("EST_ASSET_TYPE", assetDetails.assetType)}
        </div>

        {/* Allottee Details */}
        <CardSubHeader>{t("EST_ALLOTTEE_DETAILS")}</CardSubHeader>
        <div style={{ padding: "10px 20px" }}>
          {renderRow("EST_ALLOTTEE_NAME", allotteeDetails.name)}
          {renderRow("EST_PHONE_NUMBER", allotteeDetails.phone)}
          {renderRow("EST_ALTERNATE_PHONE_NUMBER", allotteeDetails.altPhone)}
          {renderRow("EST_EMAIL_ID", allotteeDetails.email)}
        </div>

        {/* Agreement Details */}
        <CardSubHeader>{t("EST_AGREEMENT_DETAILS")}</CardSubHeader>
        <div style={{ padding: "10px 20px" }}>
          {renderRow("EST_DURATION", agreementDetails.duration)}
          {renderRow("EST_RATE", agreementDetails.rate)}
          {renderRow("EST_MONTHLY_RENT", agreementDetails.monthlyRent)}
          {renderRow("EST_ADVANCE_PAYMENT", agreementDetails.advancePayment)}
          {renderRow("EST_AGREEMENT_START_DATE", agreementDetails.startDate)}
          {renderRow("EST_AGREEMENT_END_DATE", agreementDetails.endDate)}
          {renderRow("EST_ADVANCE_PAYMENT_DATE", agreementDetails.advancePaymentDate)}
        </div>

        {/* Document Details */}
        <CardSubHeader>{t("EST_DOCUMENT_DETAILS")}</CardSubHeader>
        <div style={{ padding: "10px 20px" }}>
          {renderRow("EST_E_OFFICE_FILE_NO", documentDetails.officeFile)}
          {renderRow("EST_CITIZEN_REQUEST_LETTER", documentDetails.citizenRequestLetter)}
          {renderRow("EST_ALLOTMENT_LETTER", documentDetails.allotmentLetter)}
          {renderRow("EST_SIGNED_DEED", documentDetails.signedDeed)}
        </div>

        {/* Declaration */}
        <div style={{ marginTop: "20px", padding: "0 20px" }}>
          <CheckBox
            label={t("EST_AGREE_TERMS_CONDITIONS")}
            onChange={handleAgree}
            checked={agree}
            styles={{ margin: "10px 0" }}
          />
          <SubmitBar
            label={t("Submit")}
            onSubmit={onSubmit}
            disabled={!agree}
          />
        </div>
      </Card>
    </React.Fragment>
  );
};

export default ESTPropertyAllotteeDetails;
