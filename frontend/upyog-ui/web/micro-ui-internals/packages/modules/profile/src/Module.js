import React from "react";
import ProfileDetails from "./components/ProfileDetails";
import CitizenApp from "./index";
import EmployeeApp from "./pages/employee";
import PROFILECard from "./components/ProfileCard";
import ProfileInbox from "./pages/employee/Inbox";
import ProfileApplicationDetails from "./pages/employee/ApplicationDetails";
import SearchApp from "./pages/employee/SearchApp";
import FamilyDesktopInbox from "./components/FamilyDesktopInbox";
import { TableConfig as FamilyInboxTableConfig } from "./config/inbox-table-config";
import EditProfile from "./components/EditProfile";
import EditProfileForm from "./components/EditProfileForm";
import EditAddress from "./components/EditAddress";
import EditDocuments from "./components/EditDocuments";
import AadhaarVerification from "./verifications/AadhaarVerification";
import AadhaarVerificationModal from "./verifications/AadhaarVerificationModal";
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
import IncomeDetails from "./ppp/IncomeDetails";
import VehicleDetails from "./ppp/VehicleDetails";
import PropertyDetails from "./ppp/PropertyDetails/PropertyDetails";
import DocumentDetails from "./ppp/DocumentDetails";




/**
 * Profile and Family Service root entry module component.
 * Directs execution to EmployeeApp or CitizenApp based on authenticated userType.
 *
 * @param {Object} props - Module properties.
 * @param {string} props.stateCode - State / ULB code.
 * @param {string} props.userType - Authenticated user role type (employee | citizen).
 * @param {Array<Object>} props.tenants - Tenant metadata list.
 * @returns {React.ReactElement} Active app container view.
 */
export const PROFILEModule = ({ stateCode, userType, tenants }) => {
  const { path, url } = Digit.Hooks.useModuleBasePath();
  Digit.SessionStorage.set("PROFILE_TENANTS", tenants);
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
AadhaarVerificationModal,
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
CreateProfile,
IncomeDetails,
VehicleDetails,
PropertyDetails,
DocumentDetails,
EmployeeApp,
PROFILECard,
ProfileInbox,
ProfileApplicationDetails,
ProfileSearchApp: SearchApp,
FamilyDesktopInbox,
FamilyInboxTableConfig,
};

export const initPROFILEComponents = () => {
    Object.entries(componentsToRegister).forEach(([key, value]) => {
        Digit.ComponentRegistryService.setComponent(key, value);
    });
};