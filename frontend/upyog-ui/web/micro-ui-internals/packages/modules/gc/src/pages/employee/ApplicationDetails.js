import { Header, Loader, MultiLink, ActionBar, SubmitBar, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import ApplicationDetailsTemplate from "../../../../templates/ApplicationDetails";
import GCPaymentHistoryModal from "../../components/GCPaymentHistoryModal";
import GCPauseServiceModal from "../../components/GCPauseServiceModal";
import GCPauseHistoryModal from "../../components/GCPauseHistoryModal";
import { downloadGCReceipt, downloadGCAcknowledgement, createDisconnectPayload, createReconnectPayload } from "../../utils";
import "../../css/gc-inline-auto.css";

/**
 * ApplicationDetails Component
 * 
 * Renders the employee view of the GC application details page.
 * Fetches application data using `useGCSearch`, shapes it for the shared
 * `ApplicationDetailsTemplate`, and renders workflow actions for employee processing.
 * 
 * Extracts applicant details, address, garbage collection unit information from
 * the API response and formats them into section-based detail cards.
 */
const ApplicationDetails = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const params = useParams();
  // Reconstruct application number from URL params
  let reconstructedAppNo = params.applicationNo;
  if (params["*"]) {
    reconstructedAppNo = `${params.applicationNo}/${params["*"]}`;
  }
  const applicationNo = decodeURIComponent(reconstructedAppNo);

  const [showOptions, setShowOptions] = useState(false);
  const [showPaymentHistoryModal, setShowPaymentHistoryModal] = useState(false);
  const [showDisconnectionHistoryModal, setShowDisconnectionHistoryModal] = useState(false);
  const [showDisconnectModal, setShowDisconnectModal] = useState(false);
  const [showToast, setShowToast] = useState(null);
  const closeToast = () => setShowToast(null);

  useEffect(() => {
    if (showToast) {
      const timer = setTimeout(() => setShowToast(null), 10000);
      return () => clearTimeout(timer);
    }
  }, [showToast]);

  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};

  const { data: reciept_data, isLoading: recieptDataLoading } = Digit.Hooks.useRecieptSearch(
    { tenantId, businessService: "garbage-service", consumerCodes: applicationNo, isEmployee: true },
    { enabled: !!applicationNo }
  );
  const { isLoading, data: applicationDetails, refetch: refetchApplication } = Digit.Hooks.gc.useGCApplicationDetail(
    t, tenantId, applicationNo,
    { enabled: !!applicationNo, cacheTime: 0, staleTime: 0 }
  );

  const application = applicationDetails?.applicationData?.applicationData;
  const businessService = "garbage-service";

  let workflowDetails = Digit.Hooks.useWorkflowDetails({
    tenantId: application?.tenantId || tenantId,
    id: applicationNo, // Use the application number from the URL directly
    moduleCode: "garbage-service",
    config: { enabled: !!application, staleTime: 0 },
  });

  const {
    isLoading: updatingApplication,
    isError: updateApplicationError,
    data: updateResponse,
    error: updateError,
    mutate,
  } = Digit.Hooks.gc.useGCApplicationAction(tenantId);

  const getAcknowledgement = () => downloadGCAcknowledgement(application, tenants, t);

  const downloadOptions = [];
  downloadOptions.push({ label: t("GC_DOWNLOAD_ACKNOWLEDGEMENT"), onClick: getAcknowledgement });
  if (reciept_data?.Payments?.length > 0 && !recieptDataLoading) {
    downloadOptions.push({
      label: t("GC_FEE_RECEIPT"),
      onClick: () => downloadGCReceipt(reciept_data.Payments[0].tenantId, reciept_data.Payments),
    });
    downloadOptions.push({
      label: t("GC_VIEW_PAYMENT_HISTORY"),
      onClick: () => setShowPaymentHistoryModal(true),
    });
  }
  downloadOptions.push({
    label: t("GC_VIEW_DISCONNECTION_HISTORY"),
    onClick: () => setShowDisconnectionHistoryModal(true),
  });
  if (isLoading || workflowDetails?.isLoading) return <Loader />;

  if (!application) {
    return (
      <div style={{ padding: "16px", textAlign: "center" }}>
        <h2>{t("CS_GC_APPLICATION_NOT_FOUND")}</h2>
      </div>
    );
  }

  const appNo = application?.grbgApplicationNumber;
  const rawStatus = application?.status || "";
  const appStatus = rawStatus.toUpperCase();
  const paymentStatus = application?.paymentStatus;
  const paymentAmount = application?.paymentAmount;

  const user = Digit.UserService.getUser();
  const userRoles = user?.info?.roles?.map((e) => e.code) || [];
  const nextActions = workflowDetails?.data?.nextActions || [];
  const hasWorkflowAction = nextActions.some((e) => {
    return userRoles.some((role) => e.roles?.includes(role)) || !e.roles;
  });

  const canDisconnect = nextActions.some((a) => a.action === "DISCONNECT" && (userRoles.some((r) => a.roles?.includes(r)) || !a.roles));
  const canReconnect = nextActions.some((a) => a.action === "RECONNECT" && (userRoles.some((r) => a.roles?.includes(r)) || !a.roles));
  const isPendingPayment = paymentStatus === "PENDING_FOR_PAYMENT" || (Number(paymentAmount) > 0 && paymentStatus !== "PAID");

  const handleMakePayment = () => {
    navigate(`/upyog-ui/employee/payment/collect/garbage-service/${encodeURIComponent(appNo)}`);
  };

  const handleDisconnectService = async (params) => {
    try {
      const payload = createDisconnectPayload(application, params);

      mutate(payload, {
        onSuccess: () => {
          setShowDisconnectModal(false);

          if (params.hasPendingPayment) {
            setShowToast({
              key: "info",
              message: t("GC_REDIRECTING_TO_PAYMENT"),
              label: t("GC_REDIRECTING_TO_PAYMENT"),
            });
            setTimeout(() => {
              navigate(`/upyog-ui/employee/payment/collect/garbage-service/${encodeURIComponent(appNo)}`);
            }, 1500);
          } else {
            setShowToast({
              key: "success",
              message: t("GC_DISCONNECT_REQUEST_SUCCESS"),
              label: t("GC_DISCONNECT_REQUEST_SUCCESS"),
            });
            refetchApplication();
            workflowDetails?.revalidate();
          }
        },
        onError: (err) => {
          setShowToast({
            key: "error",
            message: err?.message || t("GC_DISCONNECT_REQUEST_FAILED"),
            error: { message: err?.message || t("GC_DISCONNECT_REQUEST_FAILED") },
          });
        },
      });
    } catch (error) {
      setShowToast({
        key: "error",
        message: error?.message || t("GC_DISCONNECT_REQUEST_FAILED"),
        error: { message: error?.message || t("GC_DISCONNECT_REQUEST_FAILED") },
      });
    }
  };

  const handleContinueService = async () => {
    try {
      const payload = createReconnectPayload(application, "GC_SERVICE_RECONNECTION_REQUESTED");

      mutate(payload, {
        onSuccess: () => {
          setShowToast({
            key: "success",
            message: t("GC_RECONNECT_SERVICE_SUCCESS"),
            label: t("GC_RECONNECT_SERVICE_SUCCESS"),
          });
          refetchApplication?.();
          workflowDetails?.revalidate?.();
        },
        onError: (err) => {
          setShowToast({
            key: "error",
            message: err?.message || t("GC_RECONNECT_SERVICE_FAILED"),
            error: { message: err?.message || t("GC_RECONNECT_SERVICE_FAILED") },
          });
        },
      });
    } catch (error) {
      setShowToast({
        key: "error",
        message: error?.message || t("GC_RECONNECT_SERVICE_FAILED"),
        error: { message: error?.message || t("GC_RECONNECT_SERVICE_FAILED") },
      });
    }
  };

  const rawDetailsArray = applicationDetails?.applicationData?.applicationDetails || [];
  const detailsArray = rawDetailsArray.map((section) => {
    if (section?.title === "GC_APPLICATION_DETAILS") {
      /**
       * applicationValues:
       * Formats the key summary rows (Application Status, Payment Status, etc.)
       * and appends interactive history modal links with custom styling and skip:true flag.
       */
      const applicationValues = (section?.values || []).map((row) => {
        // Application Status styling (e.g. Red badge for DISCONNECTED)
        if (row?.title === "GC_APPLICATION_STATUS_LABEL") {
          return {
            ...row,
            value: (
              <span className={appStatus === "DISCONNECTED" ? "gc-status-disconnected" : ""}>
                {t(`GC_STATUS_${appStatus}`)}
              </span>
            ),
            skip: true,
          };
        }
        // Payment Status styling (Green for PAID, Orange for PENDING)
        if (row?.title === "GC_PAYMENT_STATUS_LABEL") {
          return {
            ...row,
            value: (
              <span className={paymentStatus === "PAID" ? "gc-status-paid" : "gc-status-pending"}>
                {t(`GC_STATUS_${paymentStatus}`)}
              </span>
            ),
            skip: true,
          };
        }
        return row;
      });

      return {
        ...section,
        values: [
          ...applicationValues,
          // Interactive link row to open the Payment History modal
          {
            title: "GC_PAYMENT_HISTORY_LABEL",
            value: (
              <span
                className="gc-payment-history-link"
                onClick={() => setShowPaymentHistoryModal(true)}
              >
                {t("GC_VIEW_DETAILS")}
              </span>
            ),
            skip: true,
          },
          // Interactive link row to open the Service Disconnection & Reconnection History modal
          {
            title: "GC_DISCONNECTION_HISTORY_LABEL",
            value: (
              <span
                className="gc-payment-history-link"
                onClick={() => setShowDisconnectionHistoryModal(true)}
              >
                {t("GC_VIEW_DETAILS")}
              </span>
            ),
            skip: true,
          },
        ],
      };
    }
    return section;
  });

  return (
    <div>
      <div className={"employee-application-details"} style={{ marginBottom: "15px" }}>
        <Header styles={{ marginLeft: "0px", paddingTop: "10px", fontSize: "32px" }}>{t("GC_APPLICATION_DETAILS")}</Header>
        <div style={{ zIndex: "10", display: "flex", flexDirection: "row-reverse", alignItems: "center", marginTop: "-45px" }}>
          {downloadOptions && downloadOptions.length > 0 && (
            <MultiLink
              className="multilinkWrapper employee-mulitlink-main-div"
              onHeadClick={() => setShowOptions(!showOptions)}
              displayOptions={showOptions}
              options={downloadOptions}
              downloadBtnClassName={"employee-download-btn-className"}
              optionsClassName={"employee-options-btn-className"}
            />
          )}
        </div>
      </div>
      <ApplicationDetailsTemplate
        id={applicationNo}
        applicationDetails={{ applicationDetails: detailsArray, applicationData: application }}
        isLoading={isLoading}
        isDataLoading={isLoading}
        applicationData={application}
        mutate={mutate}
        workflowDetails={workflowDetails}
        businessService={businessService}
        moduleCode="garbage-service"
        showToast={showToast}
        setShowToast={setShowToast}
        closeToast={closeToast}
        timelineStatusPrefix={""}
        forcedActionPrefix={"WF_EMPLOYEE_GC"}
        statusAttribute={"status"}
        isAction={canDisconnect || canReconnect || isPendingPayment}
        MenuStyle={{ color: "#FFFFFF", fontSize: "18px" }}
      />

      {(canDisconnect || canReconnect || isPendingPayment) && (
        <ActionBar>
          <div className="gc-btn-row">
            {canDisconnect && (
              <SubmitBar
                label={t("GC_DISCONNECT_SERVICE")}
                onSubmit={() => setShowDisconnectModal(true)}
              />
            )}
            {canReconnect && (
              <SubmitBar
                label={t("GC_RECONNECT_SERVICE")}
                onSubmit={handleContinueService}
              />
            )}
            {isPendingPayment && (
              <SubmitBar
                label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")}
                onSubmit={handleMakePayment}
              />
            )}
          </div>
        </ActionBar>
      )}

      <GCPauseServiceModal
        isOpen={showDisconnectModal}
        onClose={() => setShowDisconnectModal(false)}
        onConfirm={handleDisconnectService}
        applicationNo={appNo}
        paymentAmount={paymentAmount}
        paymentStatus={paymentStatus}
      />

      <GCPaymentHistoryModal
        isOpen={showPaymentHistoryModal}
        onClose={() => setShowPaymentHistoryModal(false)}
        payments={reciept_data?.Payments}
        tenantId={tenantId}
      />

      <GCPauseHistoryModal
        isOpen={showDisconnectionHistoryModal}
        onClose={() => setShowDisconnectionHistoryModal(false)}
        application={application}
      />

      {showToast && (
        <Toast
          error={showToast.key === "error"}
          label={showToast.label || showToast.message}
          onClose={() => setShowToast(null)}
        />
      )}
    </div>
  );
};

export default ApplicationDetails;