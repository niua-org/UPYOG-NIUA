import { Banner, Card, CardText, LinkButton, Loader, SubmitBar, resolveAppPath } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { convertToEditTrade, convertToResubmitTrade, convertToTrade, convertToUpdateTrade, resolveTlFinancialYear, stringToBoolean } from "../../../utils";
import getPDFData from "../../../utils/getTLAcknowledgementData";

const isTradeUpdateFlow = () =>
  window.location.href.includes("renew-trade") ||
  window.location.href.includes("edit-application");

const GetActionMessage = (props) => {
  const { t } = useTranslation();
  const update = isTradeUpdateFlow();
  if (props.isSuccess) {
    return update
      ? t("CS_TRADE_UPDATE_APPLICATION_SUCCESS")
      : t("CS_TRADE_APPLICATION_SUCCESS");
  }
  if (props.isLoading) {
    return update
      ? t("CS_TRADE_UPDATE_APPLICATION_PENDING")
      : t("CS_TRADE_APPLICATION_SUCCESS");
  }
  return update
    ? t("CS_TRADE_UPDATE_APPLICATION_FAILED")
    : t("CS_TRADE_APPLICATION_FAILED");
};

const BannerPicker = (props) => {
  return (
    <Banner
      message={GetActionMessage(props)}
      applicationNumber={props.data?.Licenses?.[0]?.applicationNumber}
      info={props.isSuccess ? props.t("TL_REF_NO_LABEL") : ""}
      successful={props.isSuccess}
    />
  );
};

const FailureCard = ({ t, data, isSuccess, isLoading, detail }) => (
  <Card>
    <BannerPicker t={t} data={data} isSuccess={!!isSuccess} isLoading={!!isLoading} />
    <CardText>{t("TL_FILE_TRADE_FAILED_RESPONSE")}</CardText>
    {detail ? <CardText>{detail}</CardText> : null}
    <Link to={resolveAppPath("/upyog-ui/citizen")}>
      <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
    </Link>
  </Card>
);

const serverErrorMessage = (err) => {
  const errors = err?.response?.data?.Errors;
  if (Array.isArray(errors) && errors.length) {
    return errors
      .map((item) => item?.message || item?.code)
      .filter(Boolean)
      .join(". ");
  }
  return err?.message || "";
};

const EMPTY_DOWNSTREAM = /json object can not be null/i;
const MAX_APPLY_RETRIES = 3;

const TLAcknowledgement = ({ data, onSuccess, onUpdateSuccess }) => {
  const { t } = useTranslation();
  const [mutationHappened, setMutationHappened] = Digit.Hooks.useSessionStorage(
    "CITIZEN_TL_MUTATION_HAPPENED",
    false
  );
  const [submitError, setSubmitError] = useState(null);
  const [errorDetail, setErrorDetail] = useState("");
  const [applyRetrying, setApplyRetrying] = useState(false);
  const applyAttempts = useRef(0);
  const applyRetryingRef = useRef(false);
  const submittedRef = useRef(false);
  const formDataRef = useRef(data);
  if (data?.TradeDetails) formDataRef.current = data;
  const resubmit = window.location.href.includes("edit-application");
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const isCreateApi = !window.location.href.includes("renew-trade");
  const mutation = Digit.Hooks.tl.useTradeLicenseAPI(
    data?.cpt?.details?.address?.tenantId || data?.tenantId || tenantId,
    isCreateApi
  );
  const mutation1 = Digit.Hooks.tl.useTradeLicenseAPI(
    data?.cpt?.details?.address?.tenantId || data?.tenantId || tenantId,
    false
  );
  const mutation2 = Digit.Hooks.tl.useTradeLicenseAPI(
    data?.cpt?.details?.address?.tenantId || data?.tenantId || tenantId,
    false
  );
  const isEdit = window.location.href.includes("renew-trade");
  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};
  const stateId = Digit.ULBService.getStateId();
  const { data: fydata = {}, isLoading: isFyLoading } = Digit.Hooks.tl.useTradeLicenseMDMS(
    stateId,
    "egf-master",
    "FinancialYear"
  );
  const isDirectRenewal = sessionStorage.getItem("isDirectRenewal")
    ? stringToBoolean(sessionStorage.getItem("isDirectRenewal"))
    : null;

  useEffect(() => {
    if (submittedRef.current) return;
    // Wait for master data so the year that covers today can be matched.
    if (!resubmit && !isEdit && isFyLoading) return;
    if (!data || (typeof data === "object" && Object.keys(data).length === 0)) {
      return;
    }
    const onSuccessedit = () => {
      setMutationHappened(true);
    };
    const timer = setTimeout(() => {
      if (submittedRef.current) return;
      try {
        if (!data?.TradeDetails?.TradeName) {
          console.warn("TLAcknowledgement: missing TradeDetails on session", data);
        }
        const tenantId1 =
          data?.cpt?.details?.address?.tenantId || data?.tenantId || tenantId;
        data.tenantId = tenantId1;
        if (!resubmit) {
          const financialYear = resolveTlFinancialYear(fydata);
          if (!isEdit && financialYear) {
            try {
              sessionStorage.setItem("CurrentFinancialYear", financialYear);
            } catch (e) {
              /* ignore */
            }
          }
          const formdata = !isEdit
            ? convertToTrade(data, { financialYear })
            : convertToEditTrade(
                data,
                fydata["egf-master"]
                  ? fydata["egf-master"].FinancialYear.filter((y) => y.module === "TL")
                  : []
              );
          if (!formdata?.Licenses?.[0]) {
            setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE");
            return;
          }
          const license = formdata.Licenses[0];
          if (!isEdit) {
            const missing = [];
            if (!license?.financialYear) missing.push("financial year");
            if (!license?.tradeLicenseDetail?.structureType) missing.push("structure type");
            if (!license?.tradeLicenseDetail?.subOwnerShipCategory) missing.push("ownership");
            if (!license?.tradeLicenseDetail?.owners?.length) missing.push("owners");
            if (!license?.tradeLicenseDetail?.tradeUnits?.length) missing.push("trade units");
            if (!license?.tradeLicenseDetail?.address?.locality?.code) missing.push("locality");
            if (missing.length) {
              console.error("TLAcknowledgement: incomplete payload", {
                missing,
                financialYear: license?.financialYear,
                owners: license?.tradeLicenseDetail?.owners,
                tradeUnits: license?.tradeLicenseDetail?.tradeUnits,
                ownership: license?.tradeLicenseDetail?.subOwnerShipCategory,
                structureType: license?.tradeLicenseDetail?.structureType,
                locality: license?.tradeLicenseDetail?.address?.locality,
              });
              setErrorDetail(`Missing ${missing.join(", ")}`);
              setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE");
              return;
            }
          }
          // Avoid INITIATE 400 INVALID UOM when slab requires UOM but value missing
          const missingUom = (license?.tradeLicenseDetail?.tradeUnits || []).some(
            (u) => u?.uom && (u.uomValue == null || u.uomValue === "")
          );
          if (!isEdit && missingUom) {
            console.error("TLAcknowledgement: trade unit missing uomValue", license?.tradeLicenseDetail?.tradeUnits);
            setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE");
            return;
          }
          formdata.Licenses[0].tenantId =
            formdata?.Licenses[0]?.tenantId || tenantId1;
          if (!isEdit) {
            if (!mutation.isPending && !mutation.isSuccess && !mutationHappened) {
              submittedRef.current = true;
              mutation.mutate(formdata, {
                // Keep the form session until APPLY succeeds. Clearing it here
                // drops documents and makes a failed APPLY look like a blank submit.
                onError: (err) => {
                  console.error("TL INITIATE failed:", err?.response?.data || err);
                  setErrorDetail(serverErrorMessage(err));
                  setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE");
                },
              });
            }
          } else if (
            fydata["egf-master"] &&
            fydata["egf-master"].FinancialYear.length > 0 &&
            isDirectRenewal
          ) {
            if (!mutation2.isPending && !mutation2.isSuccess && !mutationHappened) {
              submittedRef.current = true;
              mutation2.mutate(formdata, {
                onUpdateSuccess,
                onError: () => setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE"),
              });
            }
          } else if (!mutation1.isPending && !mutation1.isSuccess && !mutationHappened) {
            submittedRef.current = true;
            mutation1.mutate(formdata, {
              onUpdateSuccess,
              onError: () => setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE"),
            });
          }
        } else {
          const formdata = convertToResubmitTrade(data);
          formdata.Licenses[0].tenantId =
            formdata?.Licenses[0]?.tenantId || tenantId1;
          if (!mutation2.isPending && !mutation2.isSuccess && !mutationHappened) {
            submittedRef.current = true;
            mutation2.mutate(formdata, {
              onSuccess: onSuccessedit,
              onError: () => setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE"),
            });
          }
        }
      } catch (err) {
        console.error("TLAcknowledgement submit failed:", err);
        setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE");
      }
    }, 0);
    return () => clearTimeout(timer);
    // mutation objects are stable enough; re-running on their identity double-submits.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data, fydata, isEdit, isFyLoading, resubmit, tenantId]);

  useEffect(() => {
    if (
      (mutation.isSuccess || (mutation1.isSuccess && isEdit && !isDirectRenewal)) &&
      !mutation2.isPending &&
      !mutation2.isSuccess &&
      !mutation2.isError
    ) {
      try {
        const Licenses = !isEdit
          ? convertToUpdateTrade(mutation.data, formDataRef.current || data)
          : convertToUpdateTrade(mutation1.data, formDataRef.current || data);
        const submitApply = (payload) => {
          mutation2.mutate(payload, {
            onSuccess: () => {
              applyRetryingRef.current = false;
              setApplyRetrying(false);
              onSuccess?.();
              onUpdateSuccess?.();
            },
            onError: (err) => {
              const message = serverErrorMessage(err);
              // Downstream MDMS, billing-slab, or workflow calls sometimes return an empty body.
              // tl-services then throws "json object can not be null". Retry before showing failure.
              if (EMPTY_DOWNSTREAM.test(message) && applyAttempts.current < MAX_APPLY_RETRIES) {
                applyAttempts.current += 1;
                applyRetryingRef.current = true;
                setApplyRetrying(true);
                setTimeout(() => submitApply(payload), 1500 * applyAttempts.current);
                return;
              }
              applyRetryingRef.current = false;
              setApplyRetrying(false);
              console.error("TL APPLY failed:", err?.response?.data || err);
              setErrorDetail(message);
              setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE");
            },
          });
        };
        submitApply(Licenses);
      } catch (er) {
        console.error("TLAcknowledgement update failed:", er);
        setSubmitError("TL_FILE_TRADE_FAILED_RESPONSE");
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mutation.isSuccess, mutation1.isSuccess]);

  const handleDownloadPdf = async () => {
    const { Licenses = [] } = mutation.data || mutation1.data || mutation2.data || {};
    const License = (Licenses && Licenses[0]) || {};
    const tenantInfo = tenants?.find((tenant) => tenant.code === License.tenantId);
    const pdfData = getPDFData({ ...License }, tenantInfo, t);
    pdfData.then((ress) => Digit.Utils.pdf.generate(ress));
  };

  if ((submitError || mutation2.isError) && !applyRetrying && !applyRetryingRef.current) {
    return (
      <FailureCard
        t={t}
        data={mutation.data || mutation1.data || mutation2.data}
        isSuccess={false}
        isLoading={false}
        detail={errorDetail}
      />
    );
  }

  const waitingCreate =
    !resubmit &&
    !isEdit &&
    (mutation.isIdle || mutation.isPending);
  const waitingRenew =
    !resubmit &&
    isEdit &&
    !isDirectRenewal &&
    (mutation1.isIdle || mutation1.isPending);

  if (waitingCreate || waitingRenew) {
    return <Loader />;
  }

  if (
    ((mutation?.isSuccess === false && mutation?.isIdle === false) ||
      (mutation1?.isSuccess === false && mutation1?.isIdle === false)) &&
    !isDirectRenewal &&
    !resubmit
  ) {
    return (
      <FailureCard
        t={t}
        data={mutation.data || mutation1.data}
        isSuccess={false}
        isLoading={mutation?.isPending || mutation1?.isPending}
      />
    );
  }

  // INITIATE done — wait for APPLY (mutation2), or show create result if APPLY already succeeded.
  if (
    applyRetrying ||
    mutation2.isPending ||
    (mutation2.isIdle && (mutation.isSuccess || mutation1.isSuccess))
  ) {
    return <Loader />;
  }

  const resultData = mutation2.data || mutation.data || mutation1.data;
  const isResultSuccess = Boolean(
    mutation2.isSuccess || mutation.isSuccess || mutation1.isSuccess
  );

  return (
    <Card>
      <BannerPicker
        t={t}
        data={resultData}
        isSuccess={isResultSuccess}
        isLoading={mutation2.isIdle || mutation2.isLoading}
      />
      {isResultSuccess && (
        <CardText>
          {!isDirectRenewal
            ? t("TL_FILE_TRADE_RESPONSE")
            : t("TL_FILE_TRADE_RESPONSE_DIRECT_REN")}
        </CardText>
      )}
      {!isResultSuccess && <CardText>{t("TL_FILE_TRADE_FAILED_RESPONSE")}</CardText>}
      {!isEdit && isResultSuccess && (
        <SubmitBar label={t("TL_DOWNLOAD_ACK_FORM")} onSubmit={handleDownloadPdf} />
      )}
      {isResultSuccess && isEdit && (
        <LinkButton
          label={
            <div className="response-download-button">
              <span>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="#a82227"
                >
                  <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" />
                </svg>
              </span>
              <span className="download-button">{t("TL_DOWNLOAD_ACK_FORM")}</span>
            </div>
          }
          onClick={handleDownloadPdf}
        />
      )}
      {resultData?.Licenses?.[0]?.status === "PENDINGPAYMENT" && (
        <Link
          to={{
            pathname: resolveAppPath(`/upyog-ui/citizen/payment/collect/${resultData.Licenses[0].businessService}/${resultData.Licenses[0].applicationNumber}`),
            state: { tenantId: resultData.Licenses[0].tenantId },
          }}
        >
          <SubmitBar label={t("COMMON_MAKE_PAYMENT")} />
        </Link>
      )}
      <Link to={resolveAppPath("/upyog-ui/citizen")}>
        <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
      </Link>
    </Card>
  );
};

export default TLAcknowledgement;
