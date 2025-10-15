import React from "react";
import { useTranslation } from "react-i18next";
import ManageProperties from "../../components/ManageProperties";

const ManagePropertiesPage = () => {
  const { t } = useTranslation();

  return <ManageProperties t={t} />;
};

export default ManagePropertiesPage;
