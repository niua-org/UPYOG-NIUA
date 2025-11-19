import React from "react";
import { useTranslation } from "react-i18next";
import { CitizenHomeCard, PropertyHouse } from "@upyog/digit-ui-react-components";

const ESTCitizenCard = () => {
  const { t } = useTranslation();

  const citizenLinks = [
    {
      i18nKey: t("EST_MY_APPLICATIONS"),
      link: `/upyog-ui/citizen/est/myApplications`,
    },
    {
      i18nKey: t("EST_PAYMENT_HISTORY"),
      link: `/upyog-ui/citizen/est/payment-history`,
    },
  ];

  return (
    <CitizenHomeCard
      header={t("ESTATE_MANAGEMENT")}
      links={citizenLinks}
      Icon={() => <PropertyHouse />}
    />
  );
};

export default ESTCitizenCard;