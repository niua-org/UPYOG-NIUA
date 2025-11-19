import React from "react";
import { useTranslation } from "react-i18next";
import AllProperties from "../../components/AllProperties";

const AllPropertiesPage = () => {
  const { t } = useTranslation();

  return <AllProperties t={t} />;
};

export default AllPropertiesPage;
