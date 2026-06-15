import { AppContainer, BackButton, PrivateRoute } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import { Route, Routes } from "react-router-dom";
import { useTranslation } from "react-i18next";


const App = () => {
  const { path, url, ...match } = Digit.Hooks.useModuleBasePath();
  const { t } = useTranslation();
  

  const MapView = Digit?.ComponentRegistryService?.getComponent("MapView");
  const ServiceType = Digit?.ComponentRegistryService?.getComponent("ServiceTypes");
 
  return (
    <span className={"chb-citizen"} style={{ width: "100%" }}>
      <AppContainer>
        <Routes>
          <Route path="/map/*" element={<PrivateRoute><ServiceType /></PrivateRoute>} />
          <Route path="/mapview/*" element={<PrivateRoute><MapView /></PrivateRoute>} />
        </Routes>
      </AppContainer>
    </span>
  );
};

export default App;