import {
  Banner,
  Card,
  LinkButton,
  Row,
  StatusTable,
} from "@upyog/digit-ui-react-components";
import React, { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { estPayloadData } from "../../../utils";

/**
 * ESTAcknowledgement (final)
 *
 * - No loader shown for pending.
 * - Banner only shown for success or failed.
 * - If mutation.data already exists (e.g. after refresh), derive final state immediately.
 * - Defensive fallbacks if Digit/Hooks missing.
 */

const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between",
};

const BannerPicker = ({ t, resultStatus, applicationNumber }) => {
  let message = "";
  let info = "";
  let successful = false;

  if (resultStatus === "success") {
    message = window?.location?.href?.includes("edit")
      ? t("EST_UPDATE_SUCCESSFULL")
      : t("EST_SUBMIT_SUCCESSFULL");
    info = t("EST_APPLICATION_NO");
    successful = true;
  } else {
    // when called with "failed"
    message = t("EST_APPLICATION_FAILED");
    successful = false;
  }

  return (
    <Banner
      message={message}
      applicationNumber={applicationNumber || ""}
      info={info}
      successful={successful}
      style={{ width: "100%" }}
    />
  );
};

const createNoopMutation = () => ({
  mutate: (_payload, _callbacks) =>
    console.warn("Mutation hook missing; mutate noop called"),
  isLoading: false,
  isSuccess: false,
  isError: false,
  data: null,
  error: null,
});

const ESTAcknowledgement = ({ data, onSuccess }) => {
  const { t } = useTranslation();
  const hasRun = useRef(false);

  // null | "success" | "failed"
  const [resultStatus, setResultStatus] = useState(null);
  const [applicationNumber, setApplicationNumber] = useState("");

  // safe tenantId
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

  // initialize mutation safely (fallback to noop)
  let mutation = createNoopMutation();
  try {
    if (
      typeof Digit !== "undefined" &&
      Digit.Hooks &&
      Digit.Hooks.estate &&
      Digit.Hooks.estate.useESTCreateAPI
    ) {
      const raw = Digit.Hooks.estate.useESTCreateAPI(tenantId) || {};
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

  // defensive assetData from props
  const assetData = data?.Assetdata?.Assetdata || data?.Assetdata || {};

  // utility to extract estateNo from various shapes
  const extractEstateNo = (resp) => {
    try {
      const assets =
        resp?.Assets ||
        resp?.data?.Assets ||
        resp?.response?.Assets ||
        (Array.isArray(resp) ? resp : []) ||
        [];
      const asset0 = assets && assets.length ? assets[0] : {};
      return asset0?.estateNo || asset0?.applicationNo || resp?.estateNo || "";
    } catch {
      return "";
    }
  };

  // If mutation.data already present (e.g. after refresh), set immediate result
  useEffect(() => {
    try {
      const existing = mutation?.data;
      if (!existing) return;
      // if we already determined final state, don't override
      if (resultStatus === "success" || resultStatus === "failed") return;

      const estateNo = extractEstateNo(existing);
      if (estateNo) {
        setApplicationNumber(estateNo);
        setResultStatus("success");
      } else {
        setResultStatus("failed");
      }
    } catch (err) {
      console.error("Error deriving state from existing mutation.data:", err);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mutation.data]);

  // If mutation error flag toggles externally and we haven't set final result yet
  useEffect(() => {
    if (mutation?.isError && resultStatus !== "success") {
      setResultStatus("failed");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mutation.isError]);

  // main effect: build payload and call mutate once (if needed)
  useEffect(() => {
    // If we've already attempted submission, do nothing
    if (hasRun.current) return;

    // If mutation.data already exists we've handled it in the other effect, so skip calling mutate
    if (mutation?.data) {
      hasRun.current = true;
      return;
    }

    // If there's no Assetdata to submit, mark failed
    if (!data?.Assetdata) {
      console.warn("No data.Assetdata to submit. Marking as failed.");
      setResultStatus("failed");
      hasRun.current = true;
      return;
    }

    // Build payload
    let payload;
    try {
      payload = {
        RequestInfo: {
          apiId: "Rainmaker",
          authToken: Digit?.UserService?.getUser()?.access_token || "",
          userInfo: Digit?.UserService?.getUser()?.info || {},
        },
        ...estPayloadData(data),
      };
    } catch (err) {
      console.error("Payload build failed:", err);
      setResultStatus("failed");
      hasRun.current = true;
      return;
    }

    // call mutation
    try {
      if (mutation && typeof mutation.mutate === "function") {
        // DO NOT set pending state or show loader — per requirement we skip pending UI
        hasRun.current = true;

        mutation.mutate(payload, {
          onSuccess: (response) => {
            try {
              const estateNo = extractEstateNo(response || {});
              if (estateNo) {
                setApplicationNumber(estateNo);
                setResultStatus("success");
              } else {
                setResultStatus("failed");
              }
              if (typeof onSuccess === "function") onSuccess(response);
            } catch (err) {
              console.error("Error in onSuccess handler:", err);
              setResultStatus("failed");
            }
          },
          onError: (error) => {
            console.error("Create API error:", error);
            setResultStatus("failed");
          },
        });
      } else {
        console.warn("Mutation hook not available; marking failed.");
        setResultStatus("failed");
        hasRun.current = true;
      }
    } catch (err) {
      console.error("Error invoking mutation.mutate:", err);
      setResultStatus("failed");
      hasRun.current = true;
    }

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  // determine user link safely
  const getUserHomeLink = () => {
    try {
      const type = Digit?.UserService?.getUser()?.info?.type;
      return type === "CITIZEN" ? "/upyog-ui/citizen" : "/upyog-ui/employee";
    } catch {
      return "/upyog-ui/citizen";
    }
  };

  // if we have application number either from response or assetData fallback, show it
  const displayedApplicationNumber =
    applicationNumber || assetData?.estateNo || assetData?.applicationNo || "";

  return (
    <React.Fragment>
      <Card>
        {/* Render Banner only when we have success or failed */}
        {(resultStatus === "success" || resultStatus === "failed") && (
          <BannerPicker
            t={t}
            resultStatus={resultStatus}
            applicationNumber={displayedApplicationNumber}
          />
        )}

        <StatusTable>
          <Row
            rowContainerStyle={rowContainerStyle}
            last
            textStyle={{ whiteSpace: "pre", width: "60%" }}
          />
        </StatusTable>

        <div style={{ display: "flex", gap: "12px", marginTop: "16px" }}>
          <Link to={getUserHomeLink()}>
            <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
          </Link>
        </div>
      </Card>
    </React.Fragment>
  );
};

export default ESTAcknowledgement;
