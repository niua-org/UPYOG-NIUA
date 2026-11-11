import React from "react";
import ProfileDetails from "./components/ProfileDetails";
import CitizenApp from "./index";
import EditProfile from "./components/EditProfile";
import EditProfileForm from "./components/EditProfileForm";
import EditAddress from "./components/EditAddress";
import EditDocuments from "./components/EditDocuments";
import AadhaarVerification from "./components/AadhaarVerification";
import JoinExistingFamily from "./familyFeatureComponents/JoinExistingFamily";
import FamilyJoinedResponse from "./familyFeatureComponents/FamilyJoinedResponse";
import FoundFamily from "./familyFeatureComponents/FoundFamily";
import VerifyProfile from "./verifications/ProfileVerify";
import AddFamilyMember from "./familyFeatureComponents/AddFamilyMember";
import FamilyMemberVerified from "./familyFeatureComponents/FamilyMemberVerified";
import FamilyMemberAdded from "./familyFeatureComponents/FamilyMemberAdded";
import FamilyCreatedResponse from "./familyFeatureComponents/FamilyCreatedResponse";
import FamilyDashboard from "./familyFeatureComponents/FamilyDashboard";
import FamilyCreate from "./ppp";
import CreateProfile from "./ppp/CreateProfile";




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
EditDocuments,
AadhaarVerification,
JoinExistingFamily,
FamilyJoinedResponse,
FoundFamily,
VerifyProfile,
AddFamilyMember,
FamilyMemberVerified,
FamilyMemberAdded,
FamilyCreatedResponse,
FamilyDashboard,
FamilyCreate,
CreateProfile
};

export const initPROFILEComponents = () => {
    Object.entries(componentsToRegister).forEach(([key, value]) => {
        Digit.ComponentRegistryService.setComponent(key, value);
    });
};