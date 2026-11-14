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
 * - Dynamically renders routes based on configuration.
 * - Handles multi-step form submissions and navigates to acknowledgment or summary pages.
 */

import { Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { Route, useLocation, useSearchParams, Routes, Navigate } from "react-router-dom";
import { createfamilyconfig } from "../config/createfamilyconfig";
import { PROFILE_ROUTES } from "../utils/constants";
import { stageVerifiedMember, getFamilyMembers, setFamilyMembers } from "../utils";


const FamilyCreate = ({ parentRoute }) => {
  const queryClient = useQueryClient();
  const { t } = useTranslation();
  const { pathname, search } = useLocation();
  const [searchParams] = useSearchParams();
  const navigate = Digit.Hooks.useCustomNavigate();
  let config = [];
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();; 

  /**
   * Step data is kept per family member: Family_Details = { [memberId]: { profileDetails, income, vehicles } }.
   * memberId comes from `?member=<userUuid>` (defaults to the logged-in applicant), so filling in another
   * member never overwrites the details already stored for someone else.
  */
  const loggedInUuid = Digit.UserService.getUser()?.info?.uuid;
  const memberId = searchParams.get("member") || loggedInUuid;
  const isViewMode = searchParams.get("mode") === "view";
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
    const currentPath = pathname.split("/").pop();

    // Find current step by route or by key as fallback
    const currentRoute =
      config.find((r) => r.route === currentPath) ||
      config.find((r) => r.key === key);

    if (!currentRoute) {
      console.error(`[FamilyCreate] No config found for route "${currentPath}" or key "${key}".`);
      return;
    }

    let nextStep = isAddMultiple ? key : currentRoute.nextStep;

    // Preserve ?member= query param on relative navigations
    const withMember = (to) => (to.startsWith("/") ? to : `${to}${search}`);

    const redirect = (to, state) => navigate(withMember(to),skipStep ? { replace: true, ...(state != null && { state }) } : state != null ? { state } : undefined);

    // Last step — stage member if added via Aadhaar, then go to summary
    if (nextStep === null) {
      if (!isViewMode) {
        const rel = params.profileDetails?.relationshipWithHof;
        const relCode = String(rel?.code || rel?.name || rel || "").toUpperCase().replace(/\s+/g, "_");
        if (memberId !== loggedInUuid) {
          stageVerifiedMember(memberId, rel);
        } else {
          const staged = getFamilyMembers();
          if (staged.some((m) => m.userUuid === memberId)) {
            setFamilyMembers(staged.map((m) => (m.userUuid === memberId ? { ...m, relationshipWithHead: relCode || m.relationshipWithHead || "SELF" } : m)));
          }
        }
      }
      return redirect(PROFILE_ROUTES.FAMILY_MEMBER_ADDED);
    }

    if (typeof nextStep !== "string") {
      console.error(`[FamilyCreate] Step "${currentRoute.route}" has invalid nextStep.`);
      return;
    }

    redirect(nextStep);
  };

 
 // Clears parameters on back navigation
  if(params && Object.keys(params).length>0 && window.location.href.includes("/info") && sessionStorage.getItem("docReqScreenByBack") !== "true")
    {
      clearParams();
      queryClient.invalidateQueries("Family_Details");

    }

  // clear session after successfull Submit
 const clearSession = () => {
    clearParams();
    queryClient.invalidateQueries("Family_Details");
  };


  function handleSelect(key, data, skipStep, index, isAddMultiple = false) {
    if (!isViewMode) {
      setAllParams({ ...allParams, [memberId]: { ...params, [key]: { ...params[key], ...data } } });
    }
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
              <Component config={{ texts, inputs, key, isViewMode }} onSelect={handleSelect} onSkip={handleSkip} t={t} formData={params} onAdd={handleMultiple} />
            }
          />
        );
      })}
      <Route path="*" element={<Navigate to={`${config.indexRoute}`} replace />} />
    </Routes>
  );
};

export default FamilyCreate;