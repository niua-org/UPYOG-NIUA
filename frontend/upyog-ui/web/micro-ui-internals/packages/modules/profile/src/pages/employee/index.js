import React from "react";
import { Route, Routes, Navigate, useLocation } from "react-router-dom";
import { AppContainer, BackButton, PrivateRoute } from "@nudmcdgnpm/digit-ui-react-components";
import Inbox from "./Inbox";
import ApplicationDetails from "./ApplicationDetails";
import SearchApp from "./SearchApp";

/**
 * EmployeeApp container routing component for the Family Service module.
 * Mounts secure private routes for Inbox, Search, and Application Details views.
 *
 * @param {Object} props - Component properties.
 * @param {string} props.path - Base route prefix (e.g. /upyog-ui/employee/profile).
 * @param {string} props.userType - Active user type (e.g. EMPLOYEE).
 * @returns {React.ReactElement} Container routing element.
 */
const EmployeeApp = ({ path, userType }) => {
  const location = useLocation();
  const isFamilyApprover = Digit?.UserService?.hasAccess(["FAMILY_APPROVER"]);

  if (!isFamilyApprover) {
    return <Navigate to="/upyog-ui/employee" replace />;
  }
  const EnhancedReport = Digit?.ComponentRegistryService?.getComponent("EnhancedReport");

  return (
    <AppContainer>
      <div className="ground-container">
        <div className="family-back-button-wrap">
          <BackButton location={location} />
        </div>
        <Routes>
          <Route
            path="inbox/*"
            element={
              <PrivateRoute>
                <Inbox parentRoute={path} />
              </PrivateRoute>
            }
          />
          <Route
            path="search/*"
            element={
              <PrivateRoute>
                <SearchApp parentRoute={path} />
              </PrivateRoute>
            }
          />
          <Route
            path="application-details/:id/*"
            element={
              <PrivateRoute>
                <ApplicationDetails parentRoute={path} />
              </PrivateRoute>
            }
          />
          <Route
            path="family-summary/:id/*"
            element={
              <PrivateRoute>
                <ApplicationDetails parentRoute={path} />
              </PrivateRoute>
            }
          />
          <Route
            path="FamilyIDCoverageReport/*"
            element={
              <PrivateRoute>
                <EnhancedReport parentRoute={path} moduleName="rainmaker-family" reportName="FamilyIDCoverageReport" />
              </PrivateRoute>
            }
          />
          <Route path="*" element={<Navigate to="inbox" replace />} />
        </Routes>
      </div>
    </AppContainer>
  );
};

export default EmployeeApp;
