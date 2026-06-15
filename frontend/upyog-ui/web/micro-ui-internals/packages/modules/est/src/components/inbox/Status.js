import React from "react";
import { CheckBox } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";

const Status = ({ onAssignmentChange, searchParams, businessServices, statusMap, moduleCode }) => {
  const { t } = useTranslation();

  if (!statusMap || statusMap.length === 0) {
    return null;
  }

  return (
    <div className="status-container">
      <div className="filter-label" style={{ fontWeight: "normal" }}>
        {t("ES_INBOX_STATUS")}
      </div>
      {statusMap?.map((status, index) => {
        const isChecked = searchParams?.applicationStatus?.some((selected) => selected.state === status.state);
        return (
          <CheckBox
            key={index}
            label={t(`ES_EST_COMMON_STATUS_${status.state}`)}
            checked={isChecked}
            onChange={(e) => onAssignmentChange(e, status)}
          />
        );
      })}
    </div>
  );
};

export default Status;
