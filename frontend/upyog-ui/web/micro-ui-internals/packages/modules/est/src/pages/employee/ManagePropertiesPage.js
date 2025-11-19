import React from "react";
import { useTranslation } from "react-i18next";
import ESTManageProperties from "../../PageComponents/ESTManageProperties";

const ManagePropertiesPage = () => {
  const { t } = useTranslation();

  return <ESTManageProperties t={t} />;
};

export default ManagePropertiesPage;
