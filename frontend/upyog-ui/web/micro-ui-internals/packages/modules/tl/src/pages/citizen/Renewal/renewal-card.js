/**
 * One license on the citizen renewal list.
 * Rows and the Renew button come from CitizenRenewalList.json.
 * The click still runs the renewal checks: current financial year,
 * next year already filed, cancelled, and manually expired.
 */
import React, { useEffect, useState } from "react";
import {
  Card,
  Row,
  StatusTable,
  SubmitBar,
  Toast,
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { TLSearch } from "../../../../../../libraries/src/services/molecules/TL/Search";
import { convertEpochToDateCitizen, getvalidfromdate } from "../../../utils";
import {
  formatMyApplicationField,
  matchesVisibleWhen,
} from "../../../utils/tlMyApplicationsConfig";
import styles from "../../../styles/TLMyApplications.module.scss";

/** Fill :licenseNumber and :tenantId on the route named in the JSON action. */
const fillRoute = (template, license) =>
  String(template || "")
    .replace(":licenseNumber", license.licenseNumber || "")
    .replace(":tenantId", license.tenantId || "");

/**
 * A renewal for the current financial year, or a non-rejected filing for the
 * next year, blocks a new renewal. Cancelled and manually expired licenses
 * are blocked by status alone.
 */
const assessRenewal = (application, related = [], financialYears = []) => {
  const currentYear = financialYears.length
    ? getvalidfromdate("", financialYears)?.finYearRange
    : "";
  const renewalPresent = Boolean(
    currentYear && related.some((item) => item.financialYear === currentYear)
  );
  const yearEnd = String(application?.financialYear || "").split("-")[1];
  const nextYear = yearEnd ? `20${Number(yearEnd)}-${Number(yearEnd) + 1}` : "";
  const blocking = nextYear
    ? related.filter((item) => item.financialYear === nextYear && item.status !== "REJECTED")
    : [];
  return {
    renewalPresent,
    allowedToNextYear: blocking.length === 0,
    oldRenewalAppNo: blocking[0]?.applicationNumber || "",
    cancelled: application?.status === "CANCELLED",
    manualExpired: application?.status === "MANUALEXPIRED",
  };
};

const blockMessage = (result, messages, t) => {
  if (result.renewalPresent) return t(messages.renewalPresent || "TL_RENEWAL_PRESENT_ERROR");
  if (!result.allowedToNextYear && result.oldRenewalAppNo && !result.manualExpired) {
    return `${t(messages.renewalBlockedPrefix || "TL_ERROR_TOAST_RENEWAL_1")} ${result.oldRenewalAppNo} ${t(
      messages.renewalBlockedSuffix || "TL_ERROR_TOAST_RENEWAL_2"
    )}`;
  }
  if (result.cancelled) return t(messages.cancelled || "TL_ERROR_TOAST_RENEWAL_CANCEL");
  if (result.manualExpired) return t(messages.manualExpired || "TL_ERROR_TOAST_MUTUALLY_EXPIRED");
  return "";
};

const RenewalCard = ({
  application,
  resultFields = [],
  cardActions = [],
  emptyValue,
  messages = {},
  financialYears = [],
}) => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const license = application || {};
  const emptyLabel = t(emptyValue || "CS_NA");
  const [renewalPresent, setRenewalPresent] = useState(false);
  const [showToast, setShowToast] = useState(null);
  const licenseNumber = license.licenseNumber;
  const licenseTenantId = license.tenantId;
  const financialYear = license.financialYear;
  const status = license.status;

  useEffect(() => {
    let cancelled = false;
    if (!licenseNumber || !licenseTenantId || financialYears.length === 0) return undefined;
    TLSearch.numberOfApplications(licenseTenantId, {
      licenseNumbers: licenseNumber,
      offset: 0,
    }).then((related) => {
      if (cancelled) return;
      setRenewalPresent(
        assessRenewal(
          { financialYear, status },
          related || [],
          financialYears
        ).renewalPresent
      );
    }).catch(() => {
      if (!cancelled) setRenewalPresent(false);
    });
    return () => {
      cancelled = true;
    };
  }, [licenseNumber, licenseTenantId, financialYear, status, financialYears]);

  const renew = async (action) => {
    if (!license.licenseNumber || !license.tenantId) return;
    // The financial-year master is what tells us a renewal is already filed.
    if (financialYears.length === 0) {
      setShowToast({ error: true, label: t(messages.error || "CS_SOMETHING_WENT_WRONG") });
      return;
    }
    let related = [];
    try {
      related = await TLSearch.numberOfApplications(license.tenantId, {
        licenseNumbers: license.licenseNumber,
        offset: 0,
      });
    } catch {
      setShowToast({ error: true, label: t(messages.error || "CS_SOMETHING_WENT_WRONG") });
      return;
    }
    const result = assessRenewal(license, related || [], financialYears);
    setRenewalPresent(result.renewalPresent);
    const message = blockMessage(result, messages, t);
    if (message) {
      setShowToast({ error: true, label: message });
      return;
    }
    const route =
      fillRoute(action.route, license) ||
      `/upyog-ui/citizen/tl/tradelicence/renew-trade/${license.licenseNumber}/${license.tenantId}`;
    navigate(route);
  };

  const visibleActions = cardActions.filter(
    (action) => action.name === "renew" && matchesVisibleWhen(action.visibleWhen, license)
  );

  return (
    <>
      <Card className={styles["tl-myapps__card"]}>
        <StatusTable>
          {resultFields.map((field) => (
            <Row
              key={field.key || field.accessor}
              className="border-none"
              label={t(field.key)}
              text={formatMyApplicationField(license, field, {
                t,
                emptyLabel,
                formatDate: convertEpochToDateCitizen,
              })}
            />
          ))}
        </StatusTable>
        {renewalPresent ? (
          <p className={styles["tl-myapps__renewal-note"]}>
            {t(messages.renewalPresent || "TL_RENEWAL_PRESENT_ERROR")}
          </p>
        ) : (
          visibleActions.length > 0 && (
            <div className={styles["tl-myapps__actions"]}>
              {visibleActions.map((action) => (
                <SubmitBar
                  key={action.name}
                  label={t(action.label)}
                  onSubmit={() => renew(action)}
                  className={styles["tl-myapps__action-btn"]}
                />
              ))}
            </div>
          )
        )}
      </Card>
      {showToast ? (
        <Toast
          isDleteBtn={true}
          error={showToast.error}
          label={showToast.label}
          onClose={() => setShowToast(null)}
        />
      ) : null}
    </>
  );
};

export default RenewalCard;
