import React from "react";
import { Link } from "react-router-dom";
import { Header, Card } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";

const ESTManageProperties = () => {
  const { t } = useTranslation();

  return (
    <React.Fragment>
      {/* Page Header */}
      <Header>{t("EST_COMMMON_MANAGE_PROPERTIES")}</Header>

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
              to="/employee/manage-properties"
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

          {/* Assign Asset Link */}
          <span className="link">
            <Link
             to="/upyog-ui/employee/est/assign-assets"
              style={{
                textDecoration: "none",
                color: "#8B0000",
                fontWeight: "500",
                fontSize: "16px",
              }}
            >
              {t("EST_COMMMON_ASSIGN_ASSETS")}
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
              {t("EST_COMMON_ALLOTTEE_DETAILS")}
            </Link>
          </span>
        </div>
      </Card>
    </React.Fragment>
  );
};

export default ESTManageProperties;
