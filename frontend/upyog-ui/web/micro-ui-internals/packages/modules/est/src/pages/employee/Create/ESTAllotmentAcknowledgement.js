// ESTAllotmentAcknowledgement.js
import React, { useEffect, useRef, useState } from "react";
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
import { createAllotmentData, estPayloadData } from "../../../utils";

const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between",
};

/* ---------------- Banner ---------------- */

const BannerPicker = ({ t, isSuccess, data }) => {
  const applicationNumber = data?.Allotments?.[0]?.assetNo || "";
  return (
    <Banner
      message={
        isSuccess ? t("EST_ALLOTED_SUCCESSFULL") : t("EST_APPLICATION_FAILED")
      }
      applicationNumber={applicationNumber}
      info={isSuccess ? t("EST_APPLICATION_NO") : ""}
      successful={isSuccess}
      style={{ width: "100%" }}
    />
  );
};

/* ---------------- Main ---------------- */

const ESTAllotmentAcknowledgement = ({ data = {}, onSuccess }) => {
  const { t } = useTranslation();
  const hasRun = useRef(false);

  const tenantId =
    Digit.ULBService.getCitizenCurrentTenant(true) ||
    Digit.ULBService.getCurrentTenantId();

  const user = Digit.UserService.getUser().info;

  const [finalMutation, setFinalMutation] = useState({
    isLoading: true,
    isSuccess: false,
    data: null,
  });

  const allotmentMutation =
    Digit.Hooks.estate.useESTAssetsAllotment(tenantId);

  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const tenants = storeData?.tenants || [];

  /* ---------------- API CALL ---------------- */

  useEffect(() => {
    if (hasRun.current) return;
    if (!data?.AssignAssetsData) return;

    hasRun.current = true;

    // 1️⃣ Allotment payload
    const allotmentPayload = createAllotmentData(data);

    // 2️⃣ Assets payload (FRONTEND ONLY)
    const assetsPayload = estPayloadData(data);

    console.log("🟢 Allotment Payload:", allotmentPayload);
    console.log("🟡 Assets Payload:", assetsPayload);

    // ✅ REQUIRED RequestInfo (VERY IMPORTANT)
    const requestInfo = {
      apiId: "EST",
      ver: "1.0",
      ts: Date.now(),
      action: "_create",
      did: "1",
      key: "",
      msgId: Date.now().toString(),
      authToken: Digit.UserService.getUser().access_token,
    };

    allotmentMutation.mutate(
      {
        RequestInfo: requestInfo,
        ...allotmentPayload,
      },
      {
        onSuccess: (allotmentRes) => {
          console.log("✅ Allotment API Response:", allotmentRes);

          // 🔥 SAFE MERGE (payload + response)
          const payloadAllotment = allotmentPayload?.Allotments?.[0] || {};
          const responseAllotment = allotmentRes?.Allotments?.[0] || {};

          const mergedResponse = {
            Allotments: [
              {
                ...responseAllotment,
                // keep dates from payload if backend skips them
                agreementStartDate: payloadAllotment.agreementStartDate,
                agreementEndDate: payloadAllotment.agreementEndDate,
                advancePaymentDate: payloadAllotment.advancePaymentDate,
              },
            ],
            Assets: assetsPayload?.Assets || [],
          };

          console.log("🔥 FINAL mutation.data:", mergedResponse);

          setFinalMutation({
            isLoading: false,
            isSuccess: true,
            data: mergedResponse,
          });

          onSuccess && onSuccess(mergedResponse);
        },
        onError: (err) => {
          console.error(
            "❌ Allotment API Error:",
            err?.response?.data || err
          );

          setFinalMutation({
            isLoading: false,
            isSuccess: false,
            data: null,
          });
        },
      }
    );
  }, [data, tenantId]);

  /* ---------------- PDF ---------------- */

  const handleDownloadPdf = async () => {
    try {
      const allotment = finalMutation.data?.Allotments?.[0];
      if (!allotment) return;

      const tenantInfo =
        tenants.find((t) => t.code === allotment.tenantId) || {};

      const pdfData = await getESTAllotmentAcknowledgementData(
        finalMutation.data,
        tenantInfo,
        t
      );

      Digit.Utils.pdf.generate(pdfData);
    } catch (err) {
      console.error("PDF generation error:", err);
    }
  };

  /* ---------------- UI ---------------- */

  if (finalMutation.isLoading) {
    return <div>Loading...</div>;
  }

  return (
    <Card>
      <BannerPicker
        t={t}
        isSuccess={finalMutation.isSuccess}
        data={finalMutation.data}
      />

      <StatusTable>
        <Row rowContainerStyle={rowContainerStyle} last />
      </StatusTable>

      {finalMutation.isSuccess && (
        <SubmitBar
          label={t("EST_ALLOTMENT_ACKNOWLEDGEMENT")}
          onSubmit={handleDownloadPdf}
        />
      )}

      <Link
        to={
          user?.type === "CITIZEN"
            ? "/upyog-ui/citizen"
            : "/upyog-ui/employee"
        }
      >
        <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
      </Link>
    </Card>
  );
};

export default ESTAllotmentAcknowledgement;
