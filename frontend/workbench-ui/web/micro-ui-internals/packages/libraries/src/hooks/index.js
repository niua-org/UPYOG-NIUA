import { useClearNotifications, useEvents, useNotificationCount } from "./events";
import useCreateEvent from "./events/useCreateEvent";
import useUpdateEvent from "./events/useUpdateEvent";
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

import useDocCreate from "./engagement/useCreate";
import useDocDelete from "./engagement/useDelete";
import { useEngagementMDMS } from "./engagement/useMdms";
import useDocSearch from "./engagement/useSearch";
import useDocUpdate from "./engagement/useUpdate";
import useEventDetails from "./events/useEventDetails";
import useEventInbox from "./events/useEventInbox";

import useSurveyCreate from "./surveys/useCreate";
import useSurveyDelete from "./surveys/useDelete";
import useSurveySearch from "./surveys/useSearch";
import useSurveyShowResults from "./surveys/useShowResults";
import useSurveySubmitResponse from "./surveys/useSubmitResponse";
import useSurveyInbox from "./surveys/useSurveyInbox";
import useSurveyUpdate from "./surveys/useUpdate";



import useGetFAQsJSON from "./useGetFAQsJSON";
import useGetHowItWorksJSON from "./useHowItWorksJSON";
import { usePrivacyContext } from "./usePrivacyContext";
import useStaticData from "./useStaticData";
import useCustomNavigate from "./useCustomNavigate";
import useUpdateSurvey from "./surveys/useUpdate";
import useServeyCreateDef from "./surveys/useCreateSurvey";
import useGenderMDMS from "./useGenderMDMS";

const events = {
  useInbox: useEventInbox,
  useCreateEvent,
  useEventDetails,
  useUpdateEvent,
};

const engagement = {
  useMDMS: useEngagementMDMS,
  useDocCreate,
  useDocSearch,
  useDocDelete,
  useDocUpdate,
};

const survey = {
  useCreate: useSurveyCreate,
  useUpdate: useSurveyUpdate,
  useDelete: useSurveyDelete,
  useSearch: useSurveySearch,
  useSubmitResponse: useSurveySubmitResponse,
  useShowResults: useSurveyShowResults,
  useSurveyInbox,
  useUpdateSurvey: useUpdateSurvey,
  useServeyCreateDef: useServeyCreateDef
};

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
  useEvents,
  useClearNotifications,
  useNotificationCount,
  useStore,
  useDocumentSearch,
  useTenants,
  useAccessControl,
  usePrivacyContext,
  events,
  engagement,
  survey,
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
