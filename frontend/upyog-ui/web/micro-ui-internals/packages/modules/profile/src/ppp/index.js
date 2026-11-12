/**
 * FamilyCreate Component
 * 
 * This component handles the creation of Family Process
 * and rendering different steps of the application process.
 * 
 * Features:
 * - Uses React hooks for state management, routing, and API calls.
 * - Fetches common fields configuration and application data from the MDMS service.
 * - Supports navigation between different steps of the form.
 * - Caches form data using session storage to persist user inputs.
 * - Renders loading state until data is fetched.
 * - Dynamically renders routes based on configuration.
 * - Handles multi-step form submissions and navigates to acknowledgment or summary pages.
 */

import { Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
// import { PetDataConvert } from "../../../utils";
import { Route, useLocation, useSearchParams, Routes, Navigate } from "react-router-dom";
import { createfamilyconfig } from "../config/createfamilyconfig";
import { PROFILE_ROUTES } from "../utils/constants";
import { stageVerifiedMember } from "../utils";


const FamilyCreate = ({ parentRoute }) => {
  const queryClient = useQueryClient();
  const { t } = useTranslation();
  const { pathname, search } = useLocation();
  const [searchParams] = useSearchParams();
  const navigate = Digit.Hooks.useCustomNavigate();

  let config = [];
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();; 
  const mutation = Digit.Hooks.ptr.usePTRCreateAPI(tenantId);
  // Step data is kept per family member: Family_Details = { [memberId]: { profileDetails, income, vehicles } }.
  // memberId comes from `?member=<userUuid>` (defaults to the logged-in applicant), so filling in another
  // member never overwrites the details already stored for someone else.
  const loggedInUuid = Digit.UserService.getUser()?.info?.uuid;
  const memberId = searchParams.get("member") || loggedInUuid;
  const [allParams, setAllParams, clearParams] = Digit.Hooks.useSessionStorage("Family_Details", {});
  const params = allParams?.[memberId] || {};

  // Fetches common field configurations from MDMS
  ///TODO - Need to remove it later / add Profile Details
  let { data: commonFields, isLoading } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "PetService", [{ name: "CommonFieldsConfig" }],
    {
      select: (data) => {
        const formattedData = data?.["PetService"]?.["CommonFieldsConfigEmp"]
        return formattedData;
      },
    });



  const goNext = (skipStep, index, isAddMultiple, key) => {
    let currentPath = pathname.split("/").pop(),
      lastchar = currentPath.charAt(currentPath.length - 1),
      isMultiple = false,
      nextPage;

      // Handles multi-step form navigation
    if (Number(parseInt(currentPath)) || currentPath == "0" || currentPath == "-1") {
      if (currentPath == "-1" || currentPath == "-2") {
        currentPath = pathname.slice(0, -3);
        currentPath = currentPath.split("/").pop();
        isMultiple = true;
      } else {
        currentPath = pathname.slice(0, -2);
        currentPath = currentPath.split("/").pop();
        isMultiple = true;
      }
    } else {
      isMultiple = false;
    }
    if (!isNaN(lastchar)) {
      isMultiple = true;
    }
    const currentRoute =
      config.find((routeObj) => routeObj.route === (currentPath || "0")) ||
      config.find((routeObj) => routeObj.key === key);
    if (!currentRoute) {
      console.error(`Unable to find profile step configuration for route "${currentPath}" or key "${key}".`);
      return;
    }
    let nextStep = currentRoute.nextStep;

    // Keep `?member=` on step-to-step navigation (absolute routes such as the Family Members page are left as is)
    const withMember = (to) => (to.startsWith("/") ? to : `${to}${search}`);
    let redirectWithHistory = (to, state) => navigate(withMember(to), state != null ? { state } : undefined);
    if (skipStep) {
      redirectWithHistory = (to, state) => navigate(withMember(to), state != null ? { replace: true, state } : { replace: true });
    }
    if (isAddMultiple) {
      nextStep = key;
    }
    // Last step: the Family Members page (FamilyMemberAdded) is the summary of this flow
    if (nextStep === null) {
      // A member added via Aadhaar joins the family list only once all their steps are done
      if (memberId !== loggedInUuid) stageVerifiedMember(memberId, params.profileDetails?.relationshipWithHof);
      return redirectWithHistory(PROFILE_ROUTES.FAMILY_MEMBER_ADDED);
    }
    if (typeof nextStep !== "string") {
      console.error(`Profile step "${currentRoute.route}" does not have a valid nextStep configured.`);
      return;
    }
    if (!isNaN(nextStep.split("/").pop())) {
      nextPage = `${nextStep}`;
    }
    else {
      nextPage = isMultiple && nextStep !== "map" ? `${nextStep}/${index}` : `${nextStep}`;
    }

    redirectWithHistory(nextPage);
  };

 
 // Clears parameters on back navigation
  if(params && Object.keys(params).length>0 && window.location.href.includes("/info") && sessionStorage.getItem("docReqScreenByBack") !== "true")
    {
      clearParams();
      queryClient.invalidateQueries("Family_Details");

    }
 // Navigates to the acknowledgment page
  const ptrcreate = async () => {
    navigate(`acknowledgement`);
  };

  // clear session after successfull Submit
 const clearSession = () => {
    clearParams();
    queryClient.invalidateQueries("Family_Details");
  };

//  const handleSubmit = () => {
//   const formdata = PetDataConvert(params);
//   formdata.PetRegistrationApplications[0].tenantId = tenantId;
//   mutation.mutate(formdata, {
//     onSuccess: (response) => {
//       clearSession();
//       navigate("acknowledgement", {
//         state: {
//           data: response,
//           isSuccess: true,
//         },
//       });
//     },

//     onError: (error) => {
//       console.log("error");
//       navigate("acknowledgement", {
//         state: {
//           data: null,
//           isSuccess: false,
//            error:error
//         },
//       });
//     },
//   });
// };

  function handleSelect(key, data, skipStep, index, isAddMultiple = false) {
    setAllParams({ ...allParams, [memberId]: { ...params, [key]: { ...params[key], ...data } } });
    goNext(skipStep, index, isAddMultiple, key);
  }

  const handleSkip = () => { };
  const handleMultiple = () => { };

  // Clears params and cache on success
  if (isLoading) {
    return <Loader />;
  }

// Merges common fields configuration with citizen configuration
  commonFields = createfamilyconfig;
  commonFields.forEach((obj) => {
    config = config.concat(obj.body.filter((a) => !a.hideInCitizen));
  });

  config.indexRoute = "profileDetails";




  return (
    <Routes>
      {config.map((routeObj, index) => {
        const { component, texts, inputs, key } = routeObj;
        const Component = typeof component === "string" ? Digit.ComponentRegistryService.getComponent(component) : component;
        return (
          <Route
            path={`${routeObj.route}`}
            key={index}
            element={
              <Component config={{ texts, inputs, key }} onSelect={handleSelect} onSkip={handleSkip} t={t} formData={params} onAdd={handleMultiple} />
            }
          />
        );
      })}
      <Route path="*" element={<Navigate to={`${config.indexRoute}`} replace />} />
    </Routes>
  );
};

export default FamilyCreate;