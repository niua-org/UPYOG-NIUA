import React from "react";
import { CitizenSideBar } from "./CitizenSideBar";
import EmployeeSideBar from "./EmployeeSideBar";

const SideBar = ({ t, CITIZEN, isSidebarOpen, toggleSidebar, handleLogout, mobileView, userDetails, modules, linkData, islinkDataLoading, isSideBarScroll,setSideBarScrollTop, citizenLoginPath }) => {
  if (CITIZEN)
    return (
      <CitizenSideBar
        isOpen={isSidebarOpen}
        isSideBarScroll={isSideBarScroll}
        setSideBarScrollTop={setSideBarScrollTop}
        isMobile={true}
        toggleSidebar={toggleSidebar}
        onLogout={handleLogout}
        linkData={linkData}
        islinkDataLoading={islinkDataLoading}
        citizenLoginPath={citizenLoginPath}
      />
    );
  else {
    if (!mobileView && userDetails?.access_token) return <EmployeeSideBar {...{ mobileView, userDetails, modules }} />;
    else return <CitizenSideBar isOpen={isSidebarOpen} isMobile={true} toggleSidebar={toggleSidebar} onLogout={handleLogout} isEmployee={true} />;
  }
};

export default SideBar;
