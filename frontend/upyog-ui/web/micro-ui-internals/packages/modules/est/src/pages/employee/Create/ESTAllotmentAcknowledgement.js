import React, { useEffect, useRef } from "react";
import {
  Banner,
  Card,
  LinkButton,
  Row,
  StatusTable,
  SubmitBar,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import getESTAllotmentAcknowledgementData from "../../../utils/getESTAllotmentAcknowledgementData";
import { createAllotmentData } from "../../../utils";

/**
 * Helper message shown in Banner
 */
const GetActionMessage = ({ t, isSuccess }) => {
  if (isSuccess) return t("EST_ALLOTED_SUCCESSFULL");
  return t("EST_APPLICATION_FAILED");
};

const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between",
};

/**
 * BannerPicker - decides banner content based on mutation state
 * NOTE: We will only render BannerPicker when isSuccess === true OR isError === true
 */
const BannerPicker = ({ t, isSuccess, data }) => {
  let message = "";
  let info = "";
  let successful = false;

  if (isSuccess) {
    message = t("EST_ALLOTED_SUCCESSFULL");
    info = t("EST_APPLICATION_NO");
    successful = true;
  } else {
    message = t("EST_APPLICATION_FAILED");
    successful = false;
  }

  const applicationNumber =
    (data && data?.Allotments && data.Allotments?.[0]?.assetNo) ||
    (data && data?.AssignAssetsData?.assetNo) ||
    "";

  return (
    <Banner
      message={message}
      applicationNumber={applicationNumber}
      info={successful ? info : ""}
      successful={successful}
      style={{ width: "100%" }}
    />
  );
};

/**
 * Create a safe noop mutation to avoid runtime crashes when Digit hooks missing
 */
const createNoopMutation = () => ({
  mutate: (_payload, _callbacks) =>
    console.warn("Mutation hook missing; mutate noop called"),
  isLoading: false,
  isSuccess: false,
  isError: false,
  data: null,
  error: null,
});

const ESTAllotmentAcknowledgement = ({ data, onSuccess }) => {
  const hasRun = useRef(false);
  const { t } = useTranslation();

  // safe tenantId resolution
  let tenantId;
  try {
    tenantId =
      (typeof Digit !== "undefined" &&
        (Digit.ULBService?.getCitizenCurrentTenant(true) ||
          Digit.ULBService?.getCurrentTenantId())) ||
      undefined;
  } catch (err) {
    tenantId = undefined;
  }

  // initialize mutation (or fallback to noop)
  let mutation = createNoopMutation();
  try {
    if (
      typeof Digit !== "undefined" &&
      Digit.Hooks &&
      Digit.Hooks.estate &&
      typeof Digit.Hooks.estate.useESTAssetsAllotment === "function"
    ) {
      const raw = Digit.Hooks.estate.useESTAssetsAllotment(tenantId) || {};
      mutation = {
        mutate: raw?.mutate || createNoopMutation().mutate,
        isLoading: Boolean(raw?.isLoading),
        isSuccess: Boolean(raw?.isSuccess),
        isError: Boolean(raw?.isError),
        data: raw?.data ?? null,
        error: raw?.error ?? null,
      };
    }
  } catch (err) {
    console.error("Error initializing mutation hook:", err);
    mutation = createNoopMutation();
  }

  // store useful values
  const user = (() => {
    try {
      return Digit.UserService.getUser().info;
    } catch {
      return {};
    }
  })();

  const { data: storeData } =
    (Digit.Hooks && Digit.Hooks.useStore && Digit.Hooks.useStore.getInitData && Digit.Hooks.useStore.getInitData()) ||
    {};
  const tenants = (storeData && storeData.tenants) || [];

  // ---- Allotment API call on first render ----
  useEffect(() => {
    // one-time submission
    if (hasRun.current) return;
    if (!data?.AssignAssetsData) {
      // nothing to submit; skip
      return;
    }

    // mark run
    hasRun.current = true;

    try {
      const allotmentPayload = {
        RequestInfo: {
          apiId: "Rainmaker",
          authToken: Digit.UserService.getUser()?.access_token,
          userInfo: Digit.UserService.getUser()?.info,
        },
        ...createAllotmentData(data),
      };

      // call mutate
      if (mutation && typeof mutation.mutate === "function") {
        mutation.mutate(allotmentPayload, {
          onSuccess: (responseData) => {
            // call optional parent callback
            try {
              if (typeof onSuccess === "function") onSuccess(responseData);
            } catch (err) {
              console.error("onSuccess callback error:", err);
            }
          },
          onError: (error) => {
            console.error("Allotment API Error:", error);
          },
        });
      } else {
        console.warn("Mutation not available; skipping allotment call.");
      }
    } catch (err) {
      console.error("Error building/submitting allotment payload:", err);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ---- PDF Download handler ----
  const handleDownloadPdf = async () => {
    try {
      const apiData = mutation?.data;
      // fallback to provided data if mutation hasn't returned
      const responseData = apiData || { Allotments: [{ assetNo: data?.AssignAssetsData?.assetNo, tenantId: data?.AssignAssetsData?.tenantId }] };

      if (!responseData || !responseData.Allotments || !responseData.Allotments.length) {
        console.error("No allotment data available for PDF.");
        return;
      }

      const firstAllotment = responseData.Allotments[0];
      const tenantInfo = tenants?.find((tnt) => tnt.code === firstAllotment.tenantId) || {};

      const pdfData = await getESTAllotmentAcknowledgementData(responseData, tenantInfo, t);
      Digit.Utils.pdf.generate(pdfData);
    } catch (error) {
      console.error("PDF generation error:", error);
    }
  };

  // determine user home link
  const getUserHomeLink = () => {
    try {
      const type = Digit?.UserService?.getUser()?.info?.type;
      return type === "CITIZEN" ? "/upyog-ui/citizen" : "/upyog-ui/employee";
    } catch {
      return "/upyog-ui/citizen";
    }
  };

  const isSuccess = Boolean(mutation.isSuccess);
  const isError = Boolean(mutation.isError);

  // data to show in banner: prefer mutation response, fallback to original data
  const bannerData = mutation.data || { Allotments: [{ assetNo: data?.AssignAssetsData?.assetNo, tenantId: data?.AssignAssetsData?.tenantId }] };

  return (
    <Card>
      {/* Render Banner only when we have success or failed */}
      {(isSuccess || isError) && (
        <BannerPicker t={t} isSuccess={isSuccess} data={bannerData} />
      )}

      <StatusTable>
        <Row rowContainerStyle={rowContainerStyle} last textStyle={{ whiteSpace: "pre", width: "60%" }} />
      </StatusTable>

      {isSuccess && (
        <SubmitBar label={t("EST_ALLOTMENT_ACKNOWLEDGEMENT")} onSubmit={handleDownloadPdf} />
      )}

      <div style={{ display: "flex", gap: "12px", marginTop: "16px" }}>
        <Link to={getUserHomeLink()}>
          <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
        </Link>
      </div>
    </Card>
  );
};

export default ESTAllotmentAcknowledgement;