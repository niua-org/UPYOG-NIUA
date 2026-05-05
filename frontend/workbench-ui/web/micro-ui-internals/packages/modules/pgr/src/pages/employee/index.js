import React, { useState } from "react";
import { Routes, Route, useLocation } from "react-router-dom";
import { ActionBar, Menu, SubmitBar, BreadCrumb } from "@upyog/workbench-ui-react-components";
import { useTranslation } from "react-i18next";
// import { ComplaintDetails } from "./ComplaintDetails";
// import { CreateComplaint } from "./CreateComplaint";
// import Inbox from "./Inbox";
import { Employee } from "../../constants/Routes";
// import Response from "./Response";

const Complaint = () => {
  const [displayMenu, setDisplayMenu] = useState(false);
  const [popup, setPopup] = useState(false);
  const location = useLocation();
  const { t } = useTranslation();

  // CHANGE 3: Replace of match.url with location.pathname to make it dynamic and not break when the url changes
  const matchUrl = location.pathname.split('/').slice(0, 4).join('/');

  const breadcrumConfig = {
    home: {
      content: t("CS_COMMON_HOME"),
      path: Employee.Home,
    },
    inbox: {
      content: t("CS_COMMON_INBOX"),
      path: matchUrl + Employee.Inbox,
    },
    createComplaint: {
      content: t("CS_PGR_CREATE_COMPLAINT"),
      path: matchUrl + Employee.CreateComplaint,
    },
    complaintDetails: {
      content: t("CS_PGR_COMPLAINT_DETAILS"),
      path: matchUrl + Employee.ComplaintDetails + ":id",
    },
    response: {
      content: t("CS_PGR_RESPONSE"),
      path: matchUrl + Employee.Response,
    },
  };

  function popupCall(option) {
    setDisplayMenu(false);
    setPopup(true);
  }

  const CreateComplaint = Digit?.ComponentRegistryService?.getComponent('PGRCreateComplaintEmp');
  const ComplaintDetails = Digit?.ComponentRegistryService?.getComponent('PGRComplaintDetails');
  const Inbox = Digit?.ComponentRegistryService?.getComponent('PGRInbox');
  const Response = Digit?.ComponentRegistryService?.getComponent('PGRResponseEmp');

  return (
    <React.Fragment>
      <div className="ground-container">
        {!location.pathname.includes(Employee.Response) && ( //CHANGE 4
          <Routes> {/* CHANGE 5: Switch → Routes */}
            <Route
              path={matchUrl + Employee.CreateComplaint}
              element={<BreadCrumb crumbs={[breadcrumConfig.home, breadcrumConfig.createComplaint]}></BreadCrumb>} // CHANGE 6
            />
            <Route
              path={matchUrl + Employee.ComplaintDetails + ":id"}
              element={<BreadCrumb crumbs={[breadcrumConfig.home, breadcrumConfig.inbox, breadcrumConfig.complaintDetails]}></BreadCrumb>}
            />
            <Route
              path={matchUrl + Employee.Inbox}
              element={<BreadCrumb crumbs={[breadcrumConfig.home, breadcrumConfig.inbox]}></BreadCrumb>}
            />
            <Route
              path={matchUrl + Employee.Response}
              element={<BreadCrumb crumbs={[breadcrumConfig.home, breadcrumConfig.response]}></BreadCrumb>}
            />
          </Routes>
        )}
        <Routes> {/*CHANGE 5: Switch → Routes */}
          <Route path={matchUrl + Employee.CreateComplaint} element={<CreateComplaint parentUrl={matchUrl} />} /> {/* CHANGE 6 */}
          <Route path={matchUrl + Employee.ComplaintDetails + ":id*"} element={<ComplaintDetails />} />
          <Route path={matchUrl + Employee.Inbox} element={<Inbox />} />
          <Route path={matchUrl + Employee.Response} element={<Response />} />
        </Routes>
      </div>
    </React.Fragment>
  );
};

export default Complaint;
