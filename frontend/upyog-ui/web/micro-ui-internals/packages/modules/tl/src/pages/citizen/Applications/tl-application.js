/**
 * Trade license card on citizen My Applications.
 * Rows and buttons come from CitizenMyApplications.json.
 */
import React from "react";
import {
  Card,
  Row,
  StatusTable,
  SubmitBar,
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { convertEpochToDateCitizen } from "../../../utils";
import {
  formatMyApplicationField,
  matchesVisibleWhen,
} from "../../../utils/tlMyApplicationsConfig";
import styles from "../../../styles/TLMyApplications.module.scss";

const TLApplicationCard = ({ application, resultFields = [], cardActions = [], emptyValue }) => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const { path: modulePath } = Digit.Hooks.useModuleBasePath();
  const license = application || {};
  const emptyLabel = t(emptyValue || "CS_NA");

  /** Card buttons named in CitizenMyApplications.json. Unknown names are ignored. */
  const runAction = (action) => {
    if (action.name === "viewDetails") {
      if (!license.applicationNumber || !license.tenantId) return;
      // Citizen application detail route under the trade-license module.
      navigate(
        `${modulePath}/tradelicence/application/${license.applicationNumber}/${license.tenantId}`
      );
      return;
    }
    if (action.name === "makePayment") {
      if (!license.applicationNumber) return;
      // Payment collection uses the license business service, or TL from the JSON action.
      navigate(
        `/upyog-ui/citizen/payment/collect/${license.businessService || action.businessService || "TL"}/${license.applicationNumber}`,
        { state: { tenantId: license.tenantId } }
      );
    }
  };

  // visibleWhen on the JSON action hides Make Payment until status is PENDINGPAYMENT.
  const visibleActions = cardActions.filter((action) =>
    matchesVisibleWhen(action.visibleWhen, license)
  );

  return (
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
      {visibleActions.length > 0 ? (
        <div className={styles["tl-myapps__actions"]}>
          {visibleActions.map((action) => (
            <SubmitBar
              key={action.name}
              label={t(action.label)}
              onSubmit={() => runAction(action)}
              className={styles["tl-myapps__action-btn"]}
            />
          ))}
        </div>
      ) : null}
    </Card>
  );
};

export default TLApplicationCard;
