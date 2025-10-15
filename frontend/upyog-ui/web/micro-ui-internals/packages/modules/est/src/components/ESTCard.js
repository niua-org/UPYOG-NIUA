import React from "react";
import { useTranslation } from "react-i18next";
import { EmployeeModuleCard, PropertyHouse } from "@upyog/digit-ui-react-components";

const ESTCard = () => {
  const { t } = useTranslation();

  //  if (!Digit.Utils.estAccess()) {
  //   return null;
  // }

  const links = [
    {
      label: t("INBOX"),
      link: `/upyog-ui/employee/est/inbox`,
      role: "EST_CEMP",
    },
    {
      label: t("ES_COMMON_APPLICATION_SEARCH"),
      link: `/upyog-ui/employee/est/my-applications`,
    },
    {
      label: t("EST_MANAGE_PROPERTIES"),
      link: `/upyog-ui/employee/est/manage-properties`,
    },
    {
      label: t("EST_ALL_PROPERTIES"),
      link: `/upyog-ui/employee/est/all-properties`,
    },
  ];

  const EST_CEMP = Digit.UserService.hasAccess(["EST_VENDOR"]) || false;

  const propsForModuleCard = {
    Icon: <PropertyHouse />,
    moduleName: <div style={{ width: "200px", wordWrap: "break-word" }}>{t("ESTATE_MANAGEMENT")}</div>,
    kpis: [],
    links: links.filter((link) => !link?.role || EST_CEMP),
  };

  return <EmployeeModuleCard {...propsForModuleCard} />;
};

export default ESTCard;
