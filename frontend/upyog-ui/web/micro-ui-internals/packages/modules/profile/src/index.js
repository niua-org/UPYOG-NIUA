import { AppContainer, BackButton, PrivateRoute } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import { Route, Routes } from "react-router-dom";


const App = () => {
  const { path, url } = Digit.Hooks.useModuleBasePath();
  const ProfileDetails = Digit?.ComponentRegistryService?.getComponent("ProfileDetails");
  const EditProfile = Digit?.ComponentRegistryService?.getComponent("EditProfile");



 
  return (
    <span className={`${"profile-citizen"}`}>
      <AppContainer>
        {<BackButton>Back</BackButton>}
        <Routes>
          <Route path="profile-details" element={<PrivateRoute><ProfileDetails /></PrivateRoute>} />
          <Route path="edit-details" element={<PrivateRoute><EditProfile /></PrivateRoute>} />
        </Routes>
      </AppContainer>
    </span>
  );
};

export default App;