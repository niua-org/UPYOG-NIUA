import React from "react";
import { useTranslation } from "react-i18next";
import { CheckBox } from "@upyog/digit-ui-react-components";

/**
 * StatusCount component:
 * - Renders a checkbox for a status item
 * - Handles selection and checked state logic
 */

const StatusCount = ({ status, searchParams, onAssignmentChange, businessServices, clearCheck, setclearCheck, setSearchParams, _searchParams }) => {
  const { t } = useTranslation();


  return (
    <CheckBox
      onChange={(e) => onAssignmentChange(e, status)}
      checked={(() => {
        //IIFE
        if (!clearCheck) return _searchParams?.status?.some((e) => e === status.code);
        else {
          setclearCheck(false);
          return false;
        }
      })()}
      label={`${t(status.name)}`}
    />
  );
};

export default StatusCount;
