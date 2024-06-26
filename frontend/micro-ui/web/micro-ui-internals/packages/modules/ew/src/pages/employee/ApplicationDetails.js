    import { Header, MultiLink } from "@upyog/digit-ui-react-components";
    import _ from "lodash";
    import React, { useEffect, useState } from "react";
    import { useTranslation } from "react-i18next";
    import { useParams } from "react-router-dom";
    import ApplicationDetailsTemplate from "../../../../templates/ApplicationDetails";
    // import getEwAcknowledgementData from "../../utils/getEwAcknowledgementData";

    const EWApplicationDetails = () => {
      const { t } = useTranslation();
      const { data: storeData } = Digit.Hooks.useStore.getInitData();
      console.log("sss",storeData)
      const tenantId = Digit.ULBService.getCurrentTenantId();
      const { tenants } = storeData || {};
      const { id: requestId } = useParams();
      const [showToast, setShowToast] = useState(null);
      const [appDetailsToShow, setAppDetailsToShow] = useState({});
      const [showOptions, setShowOptions] = useState(false);
      const [enableAudit, setEnableAudit] = useState(false);
      const [businessService, setBusinessService] = useState("ewst");
     console.log("appDetailsToShow324287864",appDetailsToShow)



      const { isLoading, isError, data: applicationDetails, error } = Digit.Hooks.ew.useEwApplicationDetail(t, tenantId, requestId);
      console.log("app",applicationDetails)
      const {
        isLoading: updatingApplication,
        isError: updateApplicationError,
        data: updateResponse,
        error: updateError,
        mutate,
      } = Digit.Hooks.ew.useEWApplicationAction(tenantId);

      let workflowDetails = Digit.Hooks.useWorkflowDetails({
        tenantId: applicationDetails?.applicationData?.tenantId || tenantId,
        id: applicationDetails?.applicationData?.applicationData?.requestId,
        moduleCode: businessService,
        role: "EW_VENDOR",
      });


      const { isLoading: auditDataLoading, isError: isAuditError, data: auditData } = Digit.Hooks.ew.useEWSearch(
        {
          tenantId,
          filters: { requestId: requestId, audit: true },
        },
      );

      const closeToast = () => {
        setShowToast(null);
      };

      useEffect(() => {
        if (applicationDetails) {
          setAppDetailsToShow(_.cloneDeep(applicationDetails));
        
        }
      }, [applicationDetails]);



      useEffect(() => {

        if (workflowDetails?.data?.applicationBusinessService && !(workflowDetails?.data?.applicationBusinessService === "ewst" && businessService === "ewst")) {
          setBusinessService(workflowDetails?.data?.applicationBusinessService);
        }
      }, [workflowDetails.data]);

  
      // const handleDownloadPdf = async () => {
      //   const EwasteApplication = appDetailsToShow?.applicationData;
      //   console.log("ewaste",EwasteApplication)
      //   const tenantInfo = "pg.citya"
      //   tenants.find((tenant) => tenant.code === EwasteApplication.tenantId);
      //   const data = await getEwAcknowledgementData(EwasteApplication.applicationData, tenantInfo, t);
      //   Digit.Utils.pdf.generate(data);
      // };

      // const petDetailsPDF = {
      //   order: 1,
      //   label: t("EW_APPLICATION"),
      //   onClick: () => handleDownloadPdf(),
      // };
      let downloadOptions = [""];



      const { data: reciept_data, isLoading: recieptDataLoading } = Digit.Hooks.useRecieptSearch(
        {
          tenantId: tenantId,
          businessService: "ewst",
          consumerCodes: appDetailsToShow?.applicationData?.applicationData?.requestId,
          isEmployee: false,
        },
        { enabled: appDetailsToShow?.applicationData?.applicationData?.requestId ? true : false }
      );
      
      const printCertificate = async () => {
        let response = await Digit.PaymentService.generatePdf(tenantId, { EwasteApplication: [applicationDetails?.applicationData?.applicationData] }, "ewasteservicecertificate");
        const fileStore = await Digit.PaymentService.printReciept(tenantId, { fileStoreIds: response.filestoreIds[0] });
        window.open(fileStore[response?.filestoreIds[0]], "_blank");
      };

 
      if (appDetailsToShow?.applicationData?.tenantId === "pg.citya")
      downloadOptions.push({
        label: t("EW_CERTIFICATE"),
        onClick: () => printCertificate(),
      });
    console.log("rec1",reciept_data)

      return (
        <div>
          <div className={"employee-application-details"} style={{ marginBottom: "15px" }}>

            <Header styles={{ marginLeft: "0px", paddingTop: "10px", fontSize: "32px" }}>{t("EW_APPLICATION_DETAILS")}</Header>
            { downloadOptions && downloadOptions.length > 0 && (
              <MultiLink
                className="multilinkWrapper employee-mulitlink-main-div"
                onHeadClick={() => setShowOptions(!showOptions)}
                displayOptions={showOptions}
                options={downloadOptions}
                downloadBtnClassName={"employee-download-btn-className"}
                optionsClassName={"employee-options-btn-className"}
              // ref={menuRef}
              />
            )}
          </div>

          <ApplicationDetailsTemplate
            applicationDetails={appDetailsToShow?.applicationData}
            isLoading={isLoading}
            isDataLoading={isLoading}
            applicationData={appDetailsToShow?.applicationData?.applicationData}
            mutate={mutate}
            workflowDetails={workflowDetails}
            businessService={businessService}
            moduleCode="ewaste-services"
            showToast={showToast}
            setShowToast={setShowToast}
            closeToast={closeToast}
            timelineStatusPrefix={"EW_COMMON_STATUS_"}
            forcedActionPrefix={"EMPLOYEE_EW"}
            statusAttribute={"state"}
            MenuStyle={{ color: "#FFFFFF", fontSize: "18px" }}
          />
        </div>
      );
    };

    export default React.memo(EWApplicationDetails);
