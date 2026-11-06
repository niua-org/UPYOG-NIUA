import { Banner, Card, CardSubHeader, LinkButton, Loader, Row, StatusTable, SubmitBar, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router-dom";
import getGcAcknowledgementData from "../../../getGcAcknowledgementData";

const GetActionMessage = (props) => {
  const { t } = useTranslation();
  if (props?.isSuccess) {
    return t("ES_GC_RESPONSE_CREATE_ACTION") || "Application Submitted Successfully";
  } else if (props?.isLoading) {
    return t("CS_GC_APPLICATION_PENDING") || "Submission in Progress...";
  }
  return t("CS_GC_APPLICATION_FAILED") || "Application Submission Failed";
};

/**
 * GCAcknowledgement Component
 * 
 * Step 5 in the GC Application Wizard as defined in UPYOG Garbage Service HLD Section 2.5:
 * Displays generated application numbers for Owner Application and all Tenant Applications.
 * Allows downloading PDF acknowledgement and navigating back to citizen/employee home.
 */
const GCAcknowledgement = () => {
  const { t } = useTranslation();
  const { state } = useLocation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const navigate = Digit.Hooks.useCustomNavigate();
  const user = Digit.UserService.getUser().info;
  const [showToast, setShowToast] = useState(null);
  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants = [] } = storeData || {};

  const appList = state?.data?.garbageApplications || [];

  const getAppNo = (app) => app?.applicationNo || "";

  const primaryApp = appList[0] || {};
  const primaryAppNo = getAppNo(primaryApp);

  const isSuccess = state?.isSuccess && !!primaryAppNo;
  const isLoading = !state;

  const handleDownloadPdf = async (appToDownload = primaryApp) => {
    try {
      const tenantInfo = tenants.find((tenant) => tenant.code === (appToDownload?.tenantId || tenantId));
      const acknowledgementData = await getGcAcknowledgementData(appToDownload, tenantInfo, t);
      Digit.Utils.pdf.generate(acknowledgementData);
    } catch (error) {
      console.error("Download acknowledgement error:", error);
      setShowToast({ error: true, label: "CS_SOMETHING_WENT_WRONG" });
    }
  };

  useEffect(() => {
    if (showToast) {
      const timer = setTimeout(() => setShowToast(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [showToast]);

  if (isLoading) {
    return <Loader />;
  }

  return (
    <Card>
      <Banner
        message={GetActionMessage({ isSuccess, isLoading })}
        applicationNumber={primaryAppNo}
        info={isSuccess ? t("GC_APPLICATION_NO") || "Application No" : ""}
        successful={isSuccess}
        className="gc-gcacknowledgement-fullwidth"
      />

      {isSuccess && (
        <div style={{ marginTop: "24px" }}>
          <CardSubHeader style={{ fontSize: "18px", color: "#0b0c0c" }}>
            {t("GC_GENERATED_APPLICATIONS") || "Generated Garbage Applications"}
          </CardSubHeader>
          <p style={{ margin: "4px 0 16px 0", color: "#505a5f", fontSize: "14px" }}>
            {t("GC_MULTI_APP_SUCCESS_MSG") || "The following individual applications have been created for your property:"}
          </p>

          <StatusTable>
            {appList.map((app, idx) => {
              const appNo = getAppNo(app);
              const isOwner = idx === 0 || app?.garbageSpecification?.propertyType === "OWNER";
              const floor = app?.garbageSpecification?.floorNo;
              const label = isOwner
                ? `${t("GC_OWNER_APPLICATION") || "Owner Application"}`
                : `${t("GC_TENANT_APPLICATION") || "Tenant Application"} (Floor: ${floor || idx})`;

              return (
                <Row
                  key={idx}
                  label={label}
                  text={appNo}
                  textStyle={{ fontWeight: "bold", color: "#00703c" }}
                />
              );
            })}
          </StatusTable>
        </div>
      )}

      {isSuccess && (
        <SubmitBar
          label={t("CS_COMMON_DOWNLOAD_ACKNOWLEDGEMENT") || "Download Acknowledgement"}
          onSubmit={() => handleDownloadPdf(primaryApp)}
        />
      )}

      <div style={{ marginTop: "16px" }}>
        {user?.type === "CITIZEN" ? (
          <Link to={`/upyog-ui/citizen`}>
            <LinkButton label={t("CORE_COMMON_GO_TO_HOME") || "Go to Home"} />
          </Link>
        ) : (
          <Link to={`/upyog-ui/employee`}>
            <LinkButton label={t("CORE_COMMON_GO_TO_HOME") || "Go to Home"} />
          </Link>
        )}
      </div>

      {showToast && (
        <Toast
          error={showToast.error}
          warning={showToast.warning}
          label={t(showToast.label)}
          onClose={() => setShowToast(null)}
        />
      )}
    </Card>
  );
};

export default GCAcknowledgement;
