import React from "react";
import { CitizenHomeCard, MCollectIcon } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";

export const ChallanCitizenLinks = ({ matchPath, userType }) => {
  const { t } = useTranslation();

  const links = [
    {
      link: `/upyog-ui/citizen/challangeneration/my-payments`,
      i18nKey: t("CHALLAN_MY_PAYMENTS") || "My Payments",
    },
  ];

  return (
    <CitizenHomeCard
      header={t("MODULE_CHALLANGENERATION") || "Challan Generation"}
      links={links}
      Icon={() => <MCollectIcon className="fill-path-primary-main" />}
    />
  );
};

export default ChallanCitizenLinks;
