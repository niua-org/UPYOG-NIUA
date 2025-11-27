import React from "react";
import { useTranslation } from "react-i18next";
import { EmployeeModuleCard, PropertyHouse } from "@upyog/digit-ui-react-components";
import { useHistory } from "react-router-dom";

const ESTCard = () => {
  const { t } = useTranslation();
  const history = useHistory();

  const EST_CEMP = Digit.UserService.hasAccess(["EST_CEMP"]) || false;

  const links = [
    {
      label: t("INBOX"),
      link: `/upyog-ui/employee/est/inbox`,
    },
    {
      label: t("ES_COMMON_APPLICATION_SEARCH"),
      link: `/upyog-ui/employee/est/search-applications`,
      onClick: () => handleNavigation(`/upyog-ui/employee/est/search-applications`),
    },
    {
      label: t("EST_CREATE_ASSET"),
      link: `/upyog-ui/employee/est/create-asset`,
    },
    {
      label: t("EST_MANAGE_PROPERTIES"),
      link: `/upyog-ui/employee/est/manage-properties`,
    },
  ];

  const propsForModuleCard = {
    Icon: <PropertyHouse />,
    moduleName: (
      <div style={{ width: "200px", wordWrap: "break-word" }}>
        {t("ESTATE_MANAGEMENT")}
      </div>
    ),
    kpis: [],
    links: links.filter((link) => !link?.role || EST_CEMP),
  };

  return (
    <div style={{ width: "100%" }}>
      <EmployeeModuleCard {...propsForModuleCard} />
    </div>
  );
};

export default ESTCard;