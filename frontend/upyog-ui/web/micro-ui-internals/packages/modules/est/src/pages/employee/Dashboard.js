import React from "react";
import { useTranslation } from "react-i18next";
import ESTDashboard from "../../PageComponents/ESTDashboard";

const Dashboard = () => {
 const { t } = useTranslation();
  return (
    <div>
      <ESTDashboard t={t} />
    </div>
  );
};

export default Dashboard;
