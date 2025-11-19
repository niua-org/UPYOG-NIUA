import { Card, CardSubHeader, Header, Loader, Row, StatusTable, SubmitBar } from "@upyog/digit-ui-react-components";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory, useParams } from "react-router-dom";

const ESTApplicationDetails = () => {
  const { t } = useTranslation();
  const history = useHistory();
  const { allotmentId, tenantId } = useParams();
  
  const passedData = history.location?.state?.applicationData;
  const [data, setData] = useState(passedData || null);
  const [isLoading, setIsLoading] = useState(!passedData);
  const [billData, setBillData] = useState(null);
  const [paymentStatus, setPaymentStatus] = useState("CHECKING");

  useEffect(() => {
    if (!passedData) {
      fetchAllotmentDetails();
    }
    fetchBillData();
  }, [allotmentId, tenantId, passedData]);

  const fetchAllotmentDetails = async () => {
    setIsLoading(true);
    try {
      const response = await Digit.ESTService.allotmentSearch({
        tenantId,
        filters: { allotmentId: allotmentId }
      });
      setData(response?.Allotments?.[0] || null);
    } catch (error) {
      console.error("Error fetching allotment details:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchBillData = async () => {
    try {
      const result = await Digit.PaymentService.fetchBill(tenantId, { 
        businessService: "est-services", 
        consumerCode: allotmentId 
      });
      setBillData(result);
      
      if (result?.Bill?.[0]?.totalAmount > 0) {
        setPaymentStatus("PENDING");
      } else {
        setPaymentStatus("PAID");
      }
    } catch (error) {
      console.error("Error fetching bill data:", error);
      setPaymentStatus("UNKNOWN");
    }
  };

  const handleMakePayment = () => {
    history.push({
      pathname: `/upyog-ui/citizen/payment/my-bills/est-services/${allotmentId}`,
      state: { 
        tenantId: tenantId, 
        allotmentId: allotmentId,
        consumerCode: allotmentId
      },
    });
  };

  if (isLoading) {
    return <Loader />;
  }

  if (!data) {
    return <div>{t("EST_APPLICATION_NOT_FOUND")}</div>;
  }

  return (
    <React.Fragment>
      <div>
        <div className="cardHeaderWithOptions" style={{ marginRight: "auto", maxWidth: "960px" }}>
          <Header styles={{ fontSize: "32px" }}>{t("EST_ALLOTMENT_DETAILS")}</Header>
        </div>
        <Card>
          <StatusTable>
            <Row className="border-none" label={t("EST_ALLOTMENT_ID")} text={data?.allotmentId} />
            <Row className="border-none" label={t("EST_ASSET_NUMBER")} text={data?.assetNo} />
          </StatusTable>

          <CardSubHeader style={{ fontSize: "24px" }}>{t("EST_PAYMENT_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row 
              className="border-none" 
              label={t("EST_TOTAL_AMOUNT")} 
              text={
                paymentStatus === "PENDING"
                  ? (
                      <span>
                       ₹ {billData?.Bill?.[0]?.totalAmount || t("CS_NA")}  <strong style={{ color: '#a82227' }}>({t("PENDING_PAYMENT")})</strong>
                      </span>
                    )
                  : paymentStatus === "PAID"
                  ? (
                      <span style={{ color: 'green' }}>
                        <strong>({t("PAYMENT_PAID")})</strong>
                      </span>
                    )
                  : t("CS_NA")
              }
            />
            <Row className="border-none" label={t("EST_MONTHLY_RENT")} text={`₹${data?.monthlyRent || 0}`} />
            <Row className="border-none" label={t("EST_ADVANCE_PAYMENT")} text={`₹${data?.advancePayment || 0}`} />
          </StatusTable>

          <CardSubHeader style={{ fontSize: "24px" }}>{t("EST_ALLOTTEE_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("EST_ALLOTTEE_NAME")} text={data?.alloteeName || t("CS_NA")} />
            <Row className="border-none" label={t("EST_MOBILE_NUMBER")} text={data?.mobileNo || t("CS_NA")} />
            <Row className="border-none" label={t("EST_ALT_MOBILE_NUMBER")} text={data?.alternateMobileNo || t("CS_NA")} />
            <Row className="border-none" label={t("EST_EMAIL_ID")} text={data?.emailId || t("CS_NA")} />
          </StatusTable>

          <CardSubHeader style={{ fontSize: "24px" }}>{t("EST_ALLOTMENT_INFO")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("EST_STATUS")} text={data?.status || "PENDING"} />
            <Row className="border-none" label={t("EST_DURATION")} text={`${data?.duration || 0} years`} />
            <Row className="border-none" label={t("EST_RENT_RATE")} text={`₹${data?.rentRate || 0}`} />
            <Row className="border-none" label={t("EST_EOFFICEFILE_NO")} text={data?.eofficeFileNo || t("CS_NA")} />
          </StatusTable>

          {(data?.status === "PENDING" || paymentStatus === "PENDING") && (
            <div style={{ marginTop: "20px", textAlign: "center" }}>
              <SubmitBar 
                label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")} 
                onSubmit={handleMakePayment}
              />
            </div>
          )}
        </Card>
      </div>
    </React.Fragment>
  );
};

export default ESTApplicationDetails;