import { AppContainer, BackButton, PrivateRoute } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import { Route, Routes } from "react-router-dom";


const App = () => {
  const { path, url } = Digit.Hooks.useModuleBasePath();
  const ProfileDetails = Digit?.ComponentRegistryService?.getComponent("ProfileDetails");
  const EditProfile = Digit?.ComponentRegistryService?.getComponent("EditProfile");
  const JoinExistingFamily = Digit?.ComponentRegistryService?.getComponent("JoinExistingFamily");
  const FamilyJoinedResponse = Digit?.ComponentRegistryService?.getComponent("FamilyJoinedResponse");
  const FoundFamily = Digit?.ComponentRegistryService?.getComponent("FoundFamily");
  const VerifyProfile = Digit?.ComponentRegistryService?.getComponent("VerifyProfile");



 
  return (
    <span className={`${"profile-citizen"}`}>
      <AppContainer>
        {<BackButton>Back</BackButton>}
        <Routes>
          <Route path="profile-details" element={<PrivateRoute><ProfileDetails /></PrivateRoute>} />
          <Route path="edit-details" element={<PrivateRoute><EditProfile /></PrivateRoute>} />
          <Route path="join-family" element={<PrivateRoute><JoinExistingFamily /></PrivateRoute>} />
          <Route path="family-joined" element={<PrivateRoute><FamilyJoinedResponse /></PrivateRoute>} />
          <Route path="found-family" element={<PrivateRoute><FoundFamily /></PrivateRoute>} />
          <Route path="verify-profile" element={<PrivateRoute><VerifyProfile /></PrivateRoute>} />
        </Routes>
      </AppContainer>
    </span>
  );
};

export default App;