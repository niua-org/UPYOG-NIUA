import React from "react";
import { useTranslation } from "react-i18next";
import ESTPropertyAllotteeDetails from "../../PageComponents/ESTPropertyAllotteeDetails";

const ProPertyAllotteeDetails = () => {
  const { t } = useTranslation(); 

  return (
    <div>
      <ESTPropertyAllotteeDetails t={t} />
    </div>
  );
};

export default ProPertyAllotteeDetails;
