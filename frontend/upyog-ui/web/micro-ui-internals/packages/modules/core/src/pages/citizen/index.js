import {
  BackButton,
  WhatsappIcon,
  Card,
  CitizenHomeCard,
  CitizenInfoLabel,
  PrivateRoute,
  AdvertisementModuleCard,
} from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { Navigate, Route, Routes, Link, useLocation } from "react-router-dom";
import ErrorBoundary from "../../components/ErrorBoundaries";
import { AppHome, processLinkData } from "../../components/Home";
import TopBarSideBar from "../../components/TopBarSideBar";
import StaticCitizenSideBar from "../../components/TopBarSideBar/SideBar/StaticCitizenSideBar";
import CitizenHome from "./Home";
import LanguageSelection from "./Home/LanguageSelection";
import LocationSelection from "./Home/LocationSelection";
import Login from "./Login";
import UserProfile from "./Home/UserProfile";
import ErrorComponent from "../../components/ErrorComponent";
import FAQsSection from "./FAQs/FAQs";
import HowItWorks from "./HowItWorks/howItWorks";
import StaticDynamicCard from "./StaticDynamicComponent/StaticDynamicCard";
import AcknowledgementCF from "../../components/AcknowledgementCF";
import CitizenFeedback from "../../components/CitizenFeedback";
import Search from "./SearchApp";
import QRCode from "./QRCode";
import VSearchCertificate from "./CMSearchCertificate";
import AssetsQRCode from "./AssetsQRCode";
import ChallanQRCode from "./ChallanQRCode";
import { newConfig as newConfigEDCR } from "../../config/edcrConfig";
import CreateAnonymousEDCR from "./Home/EDCR";
import EDCRAcknowledgement from "./Home/EDCR/EDCRAcknowledgement";

const sidebarHiddenFor = [
  "upyog-ui/citizen/register/name",
  "/upyog-ui/citizen/select-language",
  "/upyog-ui/citizen/select-location",
  "/upyog-ui/citizen/login",
  "/upyog-ui/citizen/register/otp",
  "/upyog-ui/citizen/v2/login",
  "/upyog-ui/citizen/v2/register",
];

const headerHiddenFor = ["/upyog-ui/citizen/v2/login", "/upyog-ui/citizen/v2/register"];

import { APPLICATION_PATH } from "./Home/EDCR/utils";
import OnboardingLayout from "../onboarding/OnboardingLayout";
import { CitizenLoginV2, CitizenOtpV2, CitizenRegisterV2 } from "./V2OnboardingFlow";
import { CITIZEN_V2_ONBOARDING_STORAGE_KEY, getCitizenOnboardingPaths } from "./onboardingRoutes";

// Preserve OAuth query parameters when the existing DigiLocker callback lands
// on a disabled V1 login URL and must be handed to CitizenLoginV2.
const OnboardingEntryRedirect = ({ to }) => {
  const location = useLocation();
  return <Navigate to={`${to}${location.search}${location.hash}`} replace />;
};

const getTenants = (codes, tenants) => {
  return tenants.filter((tenant) => codes.map((item) => item.code).includes(tenant.code));
};

const Home = (props) => {
  const {
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
    isConfigBased,
  } = props;
  // Authentication status is checked with isConfigBased at the index route so a signed-in
  // V2 citizen reaches Home instead of being sent back to onboarding.
  const isCitizenAuthenticated = Boolean(userDetails?.access_token && userDetails?.info);

  // Derive all onboarding destinations from the single application flag. This
  // value is reused by entry, fallback, header/sidebar and form navigation.
  const citizenOnboardingPaths = getCitizenOnboardingPaths(isConfigBased);

  const {
    isLoading: islinkDataLoading,
    data: linkData,
    isFetched: isLinkDataFetched,
  } = Digit.Hooks.useCustomMDMS(
    Digit.ULBService.getStateId(),
    "ACCESSCONTROL-ACTIONS-TEST",
    [
      {
        name: "actions-test",
        filter: "[?(@.url == 'digit-ui-card')]",
      },
    ],
    {
      select: (data) => {
        const formattedData = data?.["ACCESSCONTROL-ACTIONS-TEST"]?.["actions-test"]
          ?.filter((el) => el.enabled === true)
          .reduce((a, b) => {
            a[b.parentModule] = a[b.parentModule]?.length > 0 ? [b, ...a[b.parentModule]] : [b];
            return a;
          }, {});
        return formattedData;
      },
    },
  );

  const isMobile = window.Digit.Utils.browser.isMobile();
  const classname = Digit.Hooks.useRouteSubscription(pathname);
  const { t } = useTranslation();

  // replacing useRouteMatch
  const path = "/upyog-ui/citizen";

  const navigate = Digit.Hooks.useCustomNavigate();

  const handleClickOnWhatsApp = (obj) => {
    window.open(obj);
  };

  const stateId = Digit.ULBService.getStateId();
  let { data: newConfig } = Digit.Hooks.obps.SearchMdmsTypes.getFormConfig(stateId, []);
  newConfig = newConfig?.EdcrConfig ? newConfig?.EdcrConfig : newConfigEDCR;

  const hideSidebar = sidebarHiddenFor.some((e) => window.location.href.includes(e));
  const hideHeader = headerHiddenFor.some((e) => window.location.href.includes(e));

  const appRoutes = modules.map(({ code, tenants }, index) => {
    const Module = Digit.ComponentRegistryService.getComponent(`${code}Module`);
    return Module ? (
      <Route
        key={index}
        path={`${code.toLowerCase()}/*`}
        element={<Module stateCode={stateCode} moduleCode={code} userType="citizen" tenants={getTenants(tenants, appTenants)} />}
      />
    ) : null;
  });

  const { data: advertisement } = Digit.Hooks.useCustomMDMS(Digit.ULBService.getStateId(), "Advertisement", [{ name: "Unipole_12_8" }], {
    select: (data) => {
      const formattedData = data?.["Advertisement"]?.["Unipole_12_8"].map((details) => {
        return {
          imageSrc: `${details.imageSrc}`,
          light: `${details.light}`,
          title: `${details.title}`,
          location: `${details.location}`,
          poleNo: `${details.poleNo}`,
          price: `${details.price}`,
          adtype: `${details.adtype}`,
          faceArea: `${details.faceArea}`,
        };
      });
      return formattedData;
    },
  });

  const Advertisement = advertisement || [];

  const ModuleLevelLinkHomePages = modules.map(({ code, bannerImage }, index) => {
    // Pass the version into service-link normalization so login-required cards
    // point to V2 login when the V2 route family is active.
    let mdmsDataObj = isLinkDataFetched ? processLinkData(linkData, code, t, isConfigBased) : undefined;
    mdmsDataObj?.links && mdmsDataObj?.links.sort((a, b) => a.orderNumber - b.orderNumber);

    return (
      <React.Fragment key={index}>
        <Route
          path={`${code.toLowerCase()}-home`}
          element={
            <div className="moduleLinkHomePage">
              <img
                src={
                  "https://nugp-assets.s3.ap-south-1.amazonaws.com/nugp+asset/Banner+UPYOG+%281920x500%29B+%282%29.jpg" ||
                  bannerImage ||
                  stateInfo?.bannerUrl
                }
                alt="noimagefound"
              />
              <BackButton className="moduleLinkHomePageBackButton" />
              {isMobile ? (
                <h4 style={{ top: "calc(16vw + 40px)", left: "1.5rem", position: "absolute", color: "white" }}>
                  {t("MODULE_" + code.toUpperCase())}
                </h4>
              ) : (
                <h1>{t("MODULE_" + code.toUpperCase())}</h1>
              )}
              <div className="moduleLinkHomePageModuleLinks">
                {mdmsDataObj && (
                  <CitizenHomeCard
                    header={t(mdmsDataObj?.header)}
                    links={mdmsDataObj?.links}
                    Icon={() => <span />}
                    Info={
                      code === "OBPS"
                        ? () => (
                            <CitizenInfoLabel
                              style={{ margin: "0px", padding: "10px" }}
                              info={t("CS_FILE_APPLICATION_INFO_LABEL")}
                              text={t(`BPA_CITIZEN_HOME_STAKEHOLDER_INCLUDES_INFO_LABEL`)}
                            />
                          )
                        : null
                    }
                    isInfo={code === "OBPS" ? true : false}
                  />
                )}
              </div>
              {code?.toUpperCase() === "ADS" && (
                <div style={{ display: "flex", flexWrap: "wrap", justifyContent: "space-between" }}>
                  {Advertisement.map((ad, index) => (
                    <AdvertisementModuleCard
                      key={ad?.poleNo || ad?.title || index}
                      imageSrc={ad.imageSrc}
                      poleNo={ad.poleNo}
                      light={ad.light}
                      title={ad.title}
                      location={ad.location}
                      price={ad.price}
                      path={`/upyog-ui/citizen/${code.toLowerCase()}/`}
                      adType={ad.adtype}
                      faceArea={ad.faceArea}
                    />
                  ))}
                </div>
              )}
              <StaticDynamicCard moduleCode={code?.toUpperCase()} />
            </div>
          }
        />
        <Route path={`${code.toLowerCase()}-faq`} element={<FAQsSection module={code?.toUpperCase()} />} />
        <Route path={`${code.toLowerCase()}-how-it-works`} element={<HowItWorks module={code?.toUpperCase()} />} />
      </React.Fragment>
    );
  });

  return (
    <div className={classname}>
      {!hideHeader && (
        <TopBarSideBar
          t={t}
          stateInfo={stateInfo}
          userDetails={userDetails}
          CITIZEN={CITIZEN}
          cityDetails={cityDetails}
          mobileView={mobileView}
          handleUserDropdownSelection={handleUserDropdownSelection}
          logoUrl={logoUrl}
          showSidebar={true}
          linkData={linkData}
          islinkDataLoading={islinkDataLoading}
          citizenLoginPath={citizenOnboardingPaths.login}
        />
      )}

      <div className={`center-container citizen-home-container${!hideHeader ? " main mb-25" : ""}`}>
        {hideSidebar ? null : (
          <div className="SideBarStatic">
            <StaticCitizenSideBar
              linkData={linkData}
              islinkDataLoading={islinkDataLoading}
              citizenLoginPath={citizenOnboardingPaths.login}
            />
          </div>
        )}

        <Routes>
          {/* <Route path={path} element={<CitizenHome />} />

          <Route path={`/feedback`} element={<PrivateRoute><CitizenFeedback /></PrivateRoute>} />
          <Route path={`/feedback-acknowledgement`} element={<PrivateRoute><AcknowledgementCF /></PrivateRoute>} />

          <Route path={`/select-language`} element={<LanguageSelection />} />
          <Route path={`/select-location`} element={<LocationSelection />} />

          <Route path={`/error`} element={<ErrorComponent initData={initData} />} />

          <Route path={`/all-services`} element={<AppHome />} />

          <Route path={`/login/*`} element={<Login stateCode={stateCode} />} />
          <Route path={`/register/*`} element={<Login stateCode={stateCode} isUserRegistered={false} />} />


          <Route path={`/user/profile`} element={<PrivateRoute><UserProfile /></PrivateRoute>} />

          <Route path={`/Audit`} element={<Search />} />
          <Route path={`/payment/verification`} element={<QRCode />} /> */}


          <Route path="error" element={<ErrorComponent initData={initData} goToHome={() => navigate("/upyog-ui/citizen")} />} />
          <Route
            path="all-services"
            element={
              // AppHome also creates role-based login links, so it must receive
              // the same version choice as this citizen router.
              <AppHome
                userType="citizen"
                modules={modules}
                getCitizenMenu={linkData}
                fetchedCitizen={isLinkDataFetched}
                isLoading={islinkDataLoading}
                isConfigBased={isConfigBased}
              />
            }
          />
          <Route
            path="feedback"
            element={
              <PrivateRoute>
                <CitizenFeedback />
              </PrivateRoute>
            }
          />
          <Route
            path="feedback-acknowledgement"
            element={
              <PrivateRoute>
                <AcknowledgementCF />
              </PrivateRoute>
            }
          />
          <Route
            path="user/profile"
            element={
              <PrivateRoute>
                <UserProfile stateCode={stateCode} userType={"citizen"} cityDetails={cityDetails} />
              </PrivateRoute>
            }
          />
          <Route path="verificationsearch-home" element={<VSearchCertificate />} />
          <Route path="assets/services" element={<AssetsQRCode />} />
          <Route path="challan/details" element={<ChallanQRCode />} />
          <Route path={`${APPLICATION_PATH}/citizen/core/edcr/scrutiny`} element={<CreateAnonymousEDCR />} />
          <Route path={`${APPLICATION_PATH}/citizen/core/edcr/scrutiny/acknowledgement`} element={<EDCRAcknowledgement />} />
          <Route path="Audit" element={<Search />} />
          <Route path="payment/verification" element={<QRCode />} />

          {/* Visiting the citizen base path enters V2 directly when enabled. In
              V1, CitizenHome is retained to preserve its existing location-aware behavior. */}
          {/* V2 uses login as the unauthenticated entry. After OTP, the full
              citizen reload restores the persisted user and renders the normal
              Citizen Home at /upyog-ui/citizen instead of reopening login. */}
          <Route
            index
            element={
              isConfigBased && !isCitizenAuthenticated
                ? <Navigate to={citizenOnboardingPaths.entry} replace />
                : <CitizenHome isConfigBased={isConfigBased} />
            }
          />

          {/* Render only the configuration-driven V2 onboarding routes. Explicit
              fallbacks keep invalid V2 URLs and disabled V1 deep links out of the old flow. */}
          {isConfigBased && (
            <>
              <Route
                element={
                  // OnboardingLayout loads the Citizen MDMS master, keeps
                  // non-sensitive values across its Outlet steps, and supplies
                  // the common background/card shell for login, register and OTP.
                  <OnboardingLayout
                    stateCode={stateCode}
                    moduleName="CITIZEN_ONBOARDING"
                    masterName="OnboardingConfig"
                    homePath={citizenOnboardingPaths.entry}
                    storageKey={CITIZEN_V2_ONBOARDING_STORAGE_KEY}
                  />
                }
              >
                {/* All three steps remain under the same provider/layout so
                    continuation markers survive route changes within V2. */}
                <Route path="v2/:flow/otp" element={<CitizenOtpV2 stateCode={stateCode} />} />
                <Route path="v2/login" element={<CitizenLoginV2 stateCode={stateCode} />} />
                <Route path="v2/register" element={<CitizenRegisterV2 stateCode={stateCode} />} />
              </Route>
              {/* Normalize malformed V2 URLs and every disabled legacy entry to
                  V2 login; replace prevents redirect loops in browser history. */}
              <Route path="v2/*" element={<Navigate to={citizenOnboardingPaths.entry} replace />} />
              <Route path="login/*" element={<OnboardingEntryRedirect to={citizenOnboardingPaths.entry} />} />
              <Route path="register/*" element={<Navigate to={citizenOnboardingPaths.entry} replace />} />
              <Route path="select-language" element={<Navigate to={citizenOnboardingPaths.entry} replace />} />
              <Route path="select-location" element={<Navigate to={citizenOnboardingPaths.entry} replace />} />
            </>
          )}

          {/* Preserve every legacy onboarding route only while V2 is disabled.
              A V2-shaped deep link falls back to V1 entry without forming a loop. */}
          {!isConfigBased && (
            <>
              {/* These legacy components are deliberately absent from the V2
                  branch so both onboarding implementations cannot mount together. */}
              <Route path="login/*" element={<Login stateCode={stateCode} />} />
              <Route path="register/*" element={<Login stateCode={stateCode} isUserRegistered={false} />} />
              <Route path="select-language" element={<LanguageSelection />} />
              <Route path="select-location" element={<LocationSelection />} />
              <Route path="v2/*" element={<Navigate to={citizenOnboardingPaths.entry} replace />} />
            </>
          )}

          {appRoutes}
          {ModuleLevelLinkHomePages}

          {/* Successful V2 authentication uses a stable dashboard URL because
              the citizen index itself is reserved as the onboarding entry. */}
          <Route path="home" element={<CitizenHome isConfigBased={isConfigBased} />} />

          {/* Keep non-onboarding citizen URLs on the existing home fallback; the
              version-specific patterns above win first and prevent onboarding loops. */}
          <Route path="*" element={<CitizenHome isConfigBased={isConfigBased} />} />
        </Routes>
      </div>
    </div>
  );
};

export default Home;
