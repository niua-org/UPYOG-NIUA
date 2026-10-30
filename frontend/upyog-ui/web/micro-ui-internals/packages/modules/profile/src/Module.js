import React from "react";
import ProfileDetails from "./components/ProfileDetails";
import CitizenApp from "./index";
import EditProfile from "./components/EditProfile";
import EditProfileForm from "./components/EditProfileForm";
import EditAddress from "./components/EditAddress";
import EditDocuments from "./components/EditDocuments";




// Main module function to get to the routes file of citizen or employee module
export const PROFILEModule = ({ stateCode, userType, tenants }) => {
  const { path, url } = Digit.Hooks.useModuleBasePath();
  Digit.SessionStorage.set("PROFILE_TENANTS", tenants);
    // Displaying employee module if userType is 'employee', otherwise displaying citizen module
  if (userType === "employee") {
    return <EmployeeApp path={path} url={url} userType={userType} />;
  } else return <CitizenApp />;
};

// Registering all components to be used in the module
const componentsToRegister = {
PROFILEModule,
ProfileDetails,
EditProfile,
EditProfileForm,
EditAddress,
EditDocuments
};

export const initPROFILEComponents = () => {
    Object.entries(componentsToRegister).forEach(([key, value]) => {
        Digit.ComponentRegistryService.setComponent(key, value);
    });
};