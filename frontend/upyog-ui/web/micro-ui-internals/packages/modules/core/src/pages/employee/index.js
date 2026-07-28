import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AppModules } from "../../components/AppModules";
import ErrorBoundary from "../../components/ErrorBoundaries";
import TopBarSideBar from "../../components/TopBarSideBar";
import ChangePassword from "./ChangePassword";
import ForgotPassword from "./ForgotPassword";
import LanguageSelection from "./LanguageSelection";
import EmployeeLogin from "./Login";
import UserProfile from "../citizen/Home/UserProfile";
import ErrorComponent from "../../components/ErrorComponent";
import { PrivateRoute } from "@nudmcdgnpm/digit-ui-react-components";
import EmployeeDashboard from "../../components/EmployeeDashboard";
import OnboardingLayout from "../onboarding/OnboardingLayout";
import {
  EmployeeChangePasswordV2,
  EmployeeForgotPasswordV2,
  EmployeeLoginV2,
} from "./EmployeeV2AuthFlow";
import { EMPLOYEE_V2_ONBOARDING_STORAGE_KEY, getEmployeeAuthPaths } from "./employeeAuthRoutes";

const userScreensExempted = ["user/profile", "user/error"];

// Preserve query parameters and router state when a bookmarked V1 auth URL is
// redirected into the active V2 Employee experience.
const EmployeeAuthRedirect = ({ to }) => {
  const location = useLocation();
  return <Navigate to={`${to}${location.search}`} state={location.state} replace />;
};

// Default to the legacy employee auth screens. The application bootstrap opts
// in explicitly, allowing other consumers of this module to remain on V1.
const EmployeeApp = ({
  stateInfo,
  userDetails,
  CITIZEN,
  cityDetails,
  mobileView,
  handleUserDropdownSelection,
  logoUrl,
  DSO,
  stateCode,
  modules,
  appTenants,
  sourceUrl,
  pathname,
  initData,
  isV2 = false,
}) => {
  const navigate = Digit.Hooks.useCustomNavigate();
  const { t } = useTranslation();
  const { path } = Digit.Hooks.useModuleBasePath();
  const location = useLocation();
  const showLanguageChange = location?.pathname?.includes("language-selection");
  const isUserProfile = userScreensExempted.some((url) => location?.pathname?.includes(url));
  // Employee authentication routes are version-aware so all unauthenticated
  // navigation stays consistently within either V1 or V2.
  const employeeAuthPaths = getEmployeeAuthPaths(isV2);

  useEffect(() => {
    Digit.UserService.setType("employee");
  }, []);
  sourceUrl = "https://s3.ap-south-1.amazonaws.com/egov-qa-assets";
  const pdfUrl = "https://pg-egov-assets.s3.ap-south-1.amazonaws.com/Upyog+Code+and+Copyright+License_v1.pdf";


  return (
    <div className="employee">
      <Routes>
        {/* Employee V2 authentication routes share the configuration-driven
            layout and never fall back to legacy screens when a step is absent. */}
        {isV2 && (
          <Route
            path="user/v2"
            element={
              // Employee onboarding uses its own MDMS master and persistence
              // namespace while reusing the shared layout/provider implementation.
              <OnboardingLayout
                stateCode={stateCode}
                moduleName="EMPLOYEE_ONBOARDING"
                masterName="OnboardingConfig"
                homePath={employeeAuthPaths.login}
                storageKey={EMPLOYEE_V2_ONBOARDING_STORAGE_KEY}
              />
            }
          >
            {/* Relative child routes make the active Outlet step explicit while
                the provider/layout remains mounted across the reset flow. */}
            {/* Login is independent; forgot-password stores the continuation
                context consumed and guarded by change-password. */}
            <Route path="login" element={<EmployeeLoginV2 />} />
            <Route path="forgot-password" element={<EmployeeForgotPasswordV2 />} />
            <Route path="change-password" element={<EmployeeChangePasswordV2 />} />
            <Route path="*" element={<Navigate to={employeeAuthPaths.login} replace />} />
          </Route>
        )}
        <Route
          path="user/*"
          element={
            <>
              {isUserProfile && (
                <TopBarSideBar
                  t={t}
                  stateInfo={stateInfo}
                  userDetails={userDetails}
                  CITIZEN={CITIZEN}
                  cityDetails={cityDetails}
                  mobileView={mobileView}
                  handleUserDropdownSelection={handleUserDropdownSelection}
                  logoUrl={logoUrl}
                  showSidebar={isUserProfile ? true : false}
                  showLanguageChange={!showLanguageChange}
                />
              )}
              <div
                className={isUserProfile ? "grounded-container" : "loginContainer"}
                style={
                  isUserProfile
                    ? { padding: 0, paddingTop: "80px", marginLeft: mobileView ? "" : "64px" }
                    : { "--banner-url": `url(${stateInfo?.bannerUrl})`, padding: "0px" }
                }
              >
                <div className="loginnn">
                  <div className="login-logo-wrapper">
                    <div className="logoNiua"></div>
                  </div>
                  <picture>
                    <source
                      id="backgroung-login"
                      media="(min-width: 950px)"
                      srcSet="https://nugp-assets.s3.ap-south-1.amazonaws.com/nugp+asset/Banner+UPYOG+(1920x1080).jpg"
                      style={{ position: "absolute", height: "100%", width: "100%" }}
                    />
                    <source
                      media="(min-width: 250px)"
                      srcSet="https://nugp-assets.s3.ap-south-1.amazonaws.com/nugp+asset/Banner+UPYOG+%28500x900%29.jpg"
                    />
                    <img
                      src="https://nugp-assets.s3.ap-south-1.amazonaws.com/nugp+asset/Banner+UPYOG+(1920x1080).jpg"
                      alt="imagealttext"
                      style={{
                        position: "absolute",
                        height: "100%",
                        width: "100%",
                        zIndex: "1",
                        display: window.location.href.includes("user/profile") ? "none" : "",
                      }}
                    />
                  </picture>
                  <Routes>
                    {/* V1 components remain mounted only when V1 is active.
                        Under V2, legacy auth URLs redirect to their V2 peers. */}
                    {isV2 ? (
                      <>
                        {/* Preserve old bookmarks without mounting legacy forms;
                            query parameters and route state survive the handoff. */}
                        <Route path="login" element={<EmployeeAuthRedirect to={employeeAuthPaths.login} />} />
                        <Route path="forgot-password" element={<EmployeeAuthRedirect to={employeeAuthPaths.forgotPassword} />} />
                        <Route path="change-password" element={<EmployeeAuthRedirect to={employeeAuthPaths.changePassword} />} />
                        <Route path="language-selection" element={<EmployeeAuthRedirect to={employeeAuthPaths.login} />} />
                      </>
                    ) : (
                      <>
                        <Route path="login" element={<EmployeeLogin />} />
                        <Route path="forgot-password" element={<ForgotPassword />} />
                        <Route path="change-password" element={<ChangePassword />} />
                        <Route path="language-selection" element={<LanguageSelection />} />
                      </>
                    )}
                    <Route
                      path="profile"
                      element={
                        <PrivateRoute>
                          <UserProfile stateCode={stateCode} userType={"employee"} cityDetails={cityDetails} />
                        </PrivateRoute>
                      }
                    />
                    <Route
                      path="error"
                      element={
                        <ErrorComponent
                          initData={initData}
                          goToHome={() => {
                            navigate("/upyog-ui/employee");
                          }}
                        />
                      }
                    />
                      <Route
                        path="*"
                        element={
                          // Unknown legacy auth URLs stay in the current flow:
                          // V2 goes to V2 login, while V1 keeps language selection.
                          <Navigate
                          to={isV2 ? employeeAuthPaths.login : "/upyog-ui/employee/user/language-selection"}
                          replace
                        />
                      }
                    />
                  </Routes>
                </div>
              </div>
            </>
          }
        />
        <Route
          path="*"
          element={
            <>
              <TopBarSideBar
                t={t}
                stateInfo={stateInfo}
                userDetails={userDetails}
                CITIZEN={CITIZEN}
                cityDetails={cityDetails}
                mobileView={mobileView}
                handleUserDropdownSelection={handleUserDropdownSelection}
                logoUrl={logoUrl}
                modules={modules}
              />
              <div className={`main ${DSO ? "m-auto" : ""}`}>
                <div className="employee-app-wrapper">
                  <ErrorBoundary initData={initData}>
                    <Routes>
                      <Route
                        path="dashboard"
                        element={
                          <PrivateRoute>
                            <EmployeeDashboard />
                          </PrivateRoute>
                        }
                      />
                      <Route
                        path="*"
                        element={
                          // Protected modules need the version flag so session
                          // expiry returns users to the matching employee login.
                          <AppModules
                            stateCode={stateCode}
                            userType="employee"
                            modules={modules}
                            appTenants={appTenants}
                            isV2={isV2}
                          />
                        }
                      />
                    </Routes>
                  </ErrorBoundary>
                </div>
                <div style={{ width: "100%", position: "fixed", bottom: 0, backgroundColor: "white", textAlign: "center" }}>
                  <div style={{ display: "flex", justifyContent: "center", color: "black" }}>
                    <a
                      style={{ cursor: "pointer", fontSize: window.Digit.Utils.browser.isMobile() ? "12px" : "14px", fontWeight: "400" }}
                      href="#"
                      target="_blank"
                    >
                      UPYOG License
                    </a>
                    <span
                      className="upyog-copyright-footer"
                      style={{ margin: "0 10px", fontSize: window.Digit.Utils.browser.isMobile() ? "12px" : "14px" }}
                    >
                      |
                    </span>
                    <span
                      className="upyog-copyright-footer"
                      style={{ cursor: "pointer", fontSize: window.Digit.Utils.browser.isMobile() ? "12px" : "14px", fontWeight: "400" }}
                      onClick={() => {
                        window.open("https://niua.in/", "_blank").focus();
                      }}
                    >
                      Copyright © 2022 National Institute of Urban Affairs
                    </span>
                  </div>
                  <div className="upyog-copyright-footer-web">
                    <span
                      className=""
                      style={{ cursor: "pointer", fontSize: window.Digit.Utils.browser.isMobile() ? "12px" : "14px", fontWeight: "400" }}
                      onClick={() => {
                        window.open("https://niua.in/", "_blank").focus();
                      }}
                    >
                      Copyright © 2022 National Institute of Urban Affairs
                    </span>
                  </div>
                </div>
              </div>
            </>
          }
        />
      </Routes>
    </div>
  );
};

export default EmployeeApp;
