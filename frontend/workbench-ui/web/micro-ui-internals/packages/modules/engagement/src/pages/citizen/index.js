import React from "react";
import { useTranslation } from "react-i18next";
import { Routes, useLocation, Route } from "react-router-dom";
import { BackButton } from "@upyog/workbench-ui-react-components";
import DocumentCategories from "./Documents/DocumentCategories";
import ViewDocument from "./Documents/ViewDocument";
import Response from "./CitizenSurvey/Response";

const CitizenApp = ({ path, url, userType, tenants }) => {
  const location = useLocation();
  const { t } = useTranslation();
  const NotificationsOrWhatsNew = Digit.ComponentRegistryService.getComponent("NotificationsAndWhatsNew");
  const Events = Digit.ComponentRegistryService.getComponent("EventsListOnGround");
  const EventDetails = Digit.ComponentRegistryService.getComponent("EventDetails");
  const Documents = Digit.ComponentRegistryService.getComponent("DocumentList");
  const SurveyList = Digit.ComponentRegistryService.getComponent("SurveyList");
  const FillSurvey = Digit.ComponentRegistryService.getComponent("FillSurvey");
  const ShowSurvey = Digit.ComponentRegistryService.getComponent("ShowSurvey");
  return (
    <React.Fragment>
      <div className="engagement-citizen-wrapper">
        {!location.pathname.includes("response") && <BackButton>{t("CS_COMMON_BACK")}</BackButton>}
        <Routes>
          <Route path={`${path}/notifications`} element={<NotificationsOrWhatsNew variant="notifications" parentRoute={path} />} />
          <Route path={`${path}/whats-new`} element={<NotificationsOrWhatsNew variant="whats-new" parentRoute={path} />} />
          <Route path={`${path}/events`} element={<Events variant="events" parentRoute={path} />} />
          <Route path={`${path}/events/details/:id`} element={<EventDetails parentRoute={path} />} />
          <Route path={`${path}/docs`} element={<DocumentCategories t={t} {...{ path }} />} />
          <Route path={`${path}/documents/viewDocument`} element={<ViewDocument t={t} {...{ path }} />} />
          <Route path={`${path}/documents/list/:category/:count`} element={<Documents />} />
          <Route path={`${path}/surveys/list`} element={<SurveyList />} />
          <Route path={`${path}/surveys/fill-survey`} element={<FillSurvey />} />
          <Route path={`${path}/surveys/submit-response`} element={<Response />} />
          <Route path={`${path}/surveys/show-survey`} element={<ShowSurvey />} />
        </Routes>
      </div>
    </React.Fragment>
  );
};
export default CitizenApp;
