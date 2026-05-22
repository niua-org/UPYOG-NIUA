import { useInitStore } from "./store";
import useAccessControl from "./useAccessControl";
import { useApplicationsForBusinessServiceSearch } from "./useApplicationForBillSearch";
import useClickOutside from "./useClickOutside";
import useCustomMDMS from "./useCustomMDMS";
import useDocumentSearch from "./useDocumentSearch";
import useDynamicData from "./useDynamicData";
import useLocation from "./useLocation";

import useInboxGeneral from "./useInboxGeneral/useInboxGeneral";
import useNewInboxGeneral from "./useInboxGeneral/useNewInbox";
import useBoundaryLocalities from "./useLocalities";
import useCommonMDMS from "./useMDMS";
import useWorkflowDetailsV2 from "./useWorkflowDetailsV2";
import useModuleTenants from "./useModuleTenants";
import useQueryParams from "./useQueryParams";
import useRouteSubscription from "./useRouteSubscription";
import { useUserSearch } from "./userSearch";
import useSessionStorage from "./useSessionStorage";
import useApplicationStatusGeneral from "./useStatusGeneral";
import useStore from "./useStore";
import { useTenants } from "./useTenants";
import useWorkflowDetails from "./workflow";
import useCustomAPIHook from "./useCustomAPIHook";
import useCustomAPIMutationHook from "./useCustomAPIMutationHook";
import useUpdateCustom from "./useUpdateCustom";


import useEmployeeSearch from "./useEmployeeSearch";




import useGetFAQsJSON from "./useGetFAQsJSON";
import useGetHowItWorksJSON from "./useHowItWorksJSON";
import { usePrivacyContext } from "./usePrivacyContext";
import useStaticData from "./useStaticData";
import useCustomNavigate from "./useCustomNavigate";
import useGenderMDMS from "./useGenderMDMS";


const Hooks = {
  useSessionStorage,
  useQueryParams,
  useWorkflowDetails,
  useInitStore,
  useClickOutside,
  useUserSearch,
  useApplicationsForBusinessServiceSearch,
  useInboxGeneral,
  useEmployeeSearch,
  useBoundaryLocalities,
  useCommonMDMS,
  useApplicationStatusGeneral,
  useModuleTenants,
  useNewInboxGeneral,
  useStore,
  useDocumentSearch,
  useTenants,
  useAccessControl,
  usePrivacyContext,
  useGenderMDMS,
  useRouteSubscription,
  useCustomAPIHook,
  useCustomAPIMutationHook,
  useWorkflowDetailsV2,
  useUpdateCustom,
  useCustomMDMS,
  useGetHowItWorksJSON,
  useGetFAQsJSON,
  useStaticData,
  useDynamicData,
  useLocation,
  useCustomNavigate
};

export default Hooks;
