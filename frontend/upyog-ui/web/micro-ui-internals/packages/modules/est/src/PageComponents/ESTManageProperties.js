import React from "react";
import { Link } from "react-router-dom";
import { Header, Card } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
const ESTManageProperties = () => {
  const { t } = useTranslation();
  const history = useHistory();

  return (
    <React.Fragment>
      <div>
         <Header>{t("EST_MANAGE_PROPERTIES")}</Header>
      {/* Card Section */}
      <Card style={{ padding: "16px 24px" }}>
        <div
          style={{
            display: "flex",
            gap: "40px", // space between links
            alignItems: "center",
          }}
        >
          {/* Manage Property Link */}
          <span className="link">
            <Link
              to="/upyog-ui/employee/est/manage-properties-table"
              style={{
                textDecoration: "none",
                color: "#8B0000",
                fontWeight: "500",
                fontSize: "16px",
              }}
            >
              {t("EST_MANAGE_PROPERTY")}
            </Link>
          </span>
           {/* Manage Property Link */}
          <span className="link">
            <Link
              to="/upyog-ui/employee/est/all-properties"
              style={{
                textDecoration: "none",
                color: "#8B0000",
                fontWeight: "500",
                fontSize: "16px",
              }}
            >
              {t("EST_ALL_PROPERTIES")}
            </Link>
          </span>

          {/* Assign Asset Link */}
          <span className="link">
            <Link
             to="/upyog-ui/employee/est/assignassets/info"
              style={{
                textDecoration: "none",
                color: "#8B0000",
                fontWeight: "500",
                fontSize: "16px",
              }}
            >
              {t("EST_ASSIGN_ASSETS")}
            </Link>
          </span>

          {/* Allotte Asset Link */}
          <span className="link">
            <Link
              to="/upyog-ui/employee/est/property-allottee-details"
              style={{
                textDecoration: "none",
                color: "#8B0000",
                fontWeight: "500",
                fontSize: "16px",
              }}
            >
              {t("EST_ALLOTTEE_DETAILS")}
            </Link>
          </span>
        </div>
      </Card>
        </div>
        </React.Fragment>
  );
};

export default ESTManageProperties;







