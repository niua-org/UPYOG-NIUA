import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { EmployeeModuleCard, PropertyHouse } from "@nudmcdgnpm/digit-ui-react-components";

// EST Card Component
// This component renders a module card for Estate Management with navigation links to various EST functionalities.

const ESTCard = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const EST_CEMP = Digit.UserService.hasAccess(["EST_CEMP"]) || false;
  if (!EST_CEMP) return null;

  const links = [
    
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
    {
    label: t("EST_ACTIONS"),   
    link: `/upyog-ui/employee/est/actions`,
  },
  ];

  const propsForModuleCard = {
    Icon: <PropertyHouse />,
    moduleName: (
      <div style={{ 
        width: isMobile ? "150px" : "200px", 
        wordWrap: "break-word",
        fontSize: isMobile ? "14px" : "16px"
      }}>
        {t("ESTATE_MANAGEMENT")}
      </div>
    ),
    kpis: [],
    links: links.filter((link) => !link?.role || EST_CEMP),
  };

  return (
    <div style={{ 
      width: "100%",
      padding: isMobile ? "10px" : "20px"
    }}>
      <EmployeeModuleCard {...propsForModuleCard} />
    </div>
  );
};

export default ESTCard;