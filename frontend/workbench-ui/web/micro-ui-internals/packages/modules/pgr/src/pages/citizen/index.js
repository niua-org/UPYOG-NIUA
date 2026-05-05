import React from "react";
import { ReopenComplaint } from "./ReopenComplaint/index";
import SelectRating from "./Rating/SelectRating";
import { PgrRoutes, getRoute } from "../../constants/Routes";
import { Routes, Route, useLocation, useResolvedPath } from "react-router-dom";
import { AppContainer, BackButton, PrivateRoute } from "@upyog/workbench-ui-react-components";

//  Remove Dublicate imports
import { useTranslation } from "react-i18next";

const App = () => {
  const { t } = useTranslation();
  const { pathname: path } = useResolvedPath(".");  // useRouteMatch → useResolvedPath

  const location = useLocation();

  const CreateComplaint = Digit?.ComponentRegistryService?.getComponent("PGRCreateComplaintCitizen");
  const ComplaintsList = Digit?.ComponentRegistryService?.getComponent("PGRComplaintsList");
  const ComplaintDetailsPage = Digit?.ComponentRegistryService?.getComponent("PGRComplaintDetailsPage");
  const SelectRating = Digit?.ComponentRegistryService?.getComponent("PGRSelectRating");
  const Response = Digit?.ComponentRegistryService?.getComponent("PGRResponseCitzen");

  return (
    <React.Fragment>
      <div className="pgr-citizen-wrapper">
        {!location.pathname.includes("/response") && <BackButton>{t("CS_COMMON_BACK")}</BackButton>}
        
         <Routes>                                                    //  Switch → Routes 
          <Route
            path="create-complaint"                                 //  relative path
            element={<PrivateRoute element={<CreateComplaint />} />}
          />
          <Route
            path="complaints"                                       // exact not needed in v6
            element={<PrivateRoute element={<ComplaintsList />} />}
          />
          <Route
            path="complaints/:id/*"                                 // :id* → :id/*
            element={<PrivateRoute element={<ComplaintDetailsPage />} />}
          />
          <Route
            path="reopen"
            element={
              <PrivateRoute
                element={
                  // match/url props removed — use useLocation/useParams inside component
                  <ReopenComplaint parentRoute={path} />
                }
              />
            }
          />
          <Route
            path="rate/:id/*"                                       // :id* → :id/*
            element={<PrivateRoute element={<SelectRating parentRoute={path} />} />}
          />
          <Route
            path="response"
            element={
              // match props removed
              <PrivateRoute element={<Response />} />
            }
          />

        </Routes>
      </div>
    </React.Fragment>
  );
};

export default App;
