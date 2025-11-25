import { Card, CardSubHeader, Header, Loader, Row, StatusTable, SubmitBar } from "@upyog/digit-ui-react-components";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory, useParams } from "react-router-dom";
import ViewTimeline from "../../components/ViewTimeline";

const ESTApplicationDetails = () => {
  const { t } = useTranslation();
  const history = useHistory();
  const { assetNo, tenantId } = useParams();
  
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
  }, [assetNo, tenantId, passedData]);

  const fetchAllotmentDetails = async () => {
    setIsLoading(true);
    try {
      const response = await Digit.ESTService.assetSearch({
        tenantId,
        filters: {
          AssetSearchCriteria: {
            tenantId,
            estateNo: assetNo
          }
        }
      });
      setData(response?.Assets?.[0] || null);
    } catch (error) {
      console.error("Error fetching asset details:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchBillData = async () => {
    try {
      const result = await Digit.PaymentService.fetchBill(tenantId, { 
        businessService: "est-services", 
        consumerCode: assetNo 
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
      pathname: `/upyog-ui/citizen/payment/my-bills/est-services/${data?.estateNo}`,
    });
  };

  if (isLoading) {
    return <Loader />;
  }

  if (!data) {
    return <div>{t("EST_APPLICATION_NOT_FOUND")}</div>;
  }

  // Create application object for ViewTimeline
  const applicationForTimeline = {
    tenantId: data?.tenantId || tenantId,
    applicationNo: data?.estateNo,
    workflow: {
      businessService: "EST"
    },
    channel: "CITIZEN",
    auditDetails: data?.auditDetails
  };

  return (
    <React.Fragment>
      <div>
        <div className="cardHeaderWithOptions" style={{ marginRight: "auto", maxWidth: "960px" }}>
          <Header styles={{ fontSize: "32px" }}>{t("EST_ALLOTMENT_DETAILS")}</Header>
        </div>
        
        <Card>
          <CardSubHeader style={{ fontSize: "24px" }}>{t("EST_BASIC_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("EST_ASSET_ID")} text={data?.assetId} />
            <Row className="border-none" label={t("EST_ESTATE_NUMBER")} text={data?.estateNo} />
            <Row className="border-none" label={t("EST_ASSET_STATUS")} text={data?.assetStatus || "PENDING"} />
          </StatusTable>

          <CardSubHeader style={{ fontSize: "24px" }}>{t("EST_ASSET_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("EST_ASSET_NAME")} text={data?.assetName || t("CS_NA")} />
            <Row className="border-none" label={t("EST_BUILDING_NAME")} text={data?.buildingName || t("CS_NA")} />
            <Row className="border-none" label={t("EST_ASSET_TYPE")} text={data?.assetType || t("CS_NA")} />
            <Row className="border-none" label={t("EST_LOCALITY")} text={data?.locality || t("CS_NA")} />
            <Row className="border-none" label={t("EST_FLOOR")} text={data?.floor || t("CS_NA")} />
          </StatusTable>

          <CardSubHeader style={{ fontSize: "24px" }}>{t("EST_PAYMENT_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("EST_RATE")} text={`₹${data?.rate || 0}`} />
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
          </StatusTable>

          <CardSubHeader style={{ fontSize: "24px" }}>{t("EST_ADDITIONAL_INFO")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("EST_AREA")} text={`${data?.additionalDetails?.area || 0} sq ft`} />
            <Row className="border-none" label={t("EST_CREATED_DATE")} text={
              data?.auditDetails?.createdTime 
                ? new Date(data.auditDetails.createdTime).toLocaleDateString("en-GB")
                : t("CS_NA")
            } />
          </StatusTable>
          
        </Card>

        {/* ViewTimeline Component */}
        <Card>
          <ViewTimeline
            application={applicationForTimeline}
            id={data?.estateNo}
            userType="citizen"
          />
        </Card>
      </div>
    </React.Fragment>
  );
};

export default ESTApplicationDetails;
