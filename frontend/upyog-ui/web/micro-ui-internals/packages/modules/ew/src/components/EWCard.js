import React from "react";
import { useTranslation } from "react-i18next";
import { EmployeeModuleCard, PropertyHouse } from "@upyog/digit-ui-react-components";

const EWCard = () => {
  const { t } = useTranslation();

  if (!Digit.Utils.ewAccess()) {
    return null;
  }
  const links=[
     {
       label: t("INBOX"),
       link: `/upyog-ui/employee/ew/inbox`,
       role: "EW_CEMP"
     },
     {
      label: t("ES_COMMON_APPLICATION_SEARCH"),
      link: `/upyog-ui/employee/ew/my-applications`,
   },
  ]
  const EW_CEMP = Digit.UserService.hasAccess(["EW_VENDOR"]) || false;
  const propsForModuleCard = {
    Icon: <PropertyHouse />,
    moduleName: <div style={{ width: "200px", wordWrap: "break-word" }}>{t("TITLE_E_WASTE")}</div>,
    kpis: [],
    links:links.filter(link=>!link?.role||EW_CEMP),
  };

  return <EmployeeModuleCard {...propsForModuleCard} />;
};

export default EWCard;
