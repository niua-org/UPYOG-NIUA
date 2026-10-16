/**
 * useFormWizard.js
 *
 * Shared MDMS / local-JSON wizard router state for any module form flow.
 * Session, step flattening, goNext, and optional check/ack helpers.
 *
 * Module hooks (useTlWizard / useEstWizard) should stay thin wrappers over this.
 *
 * @see formWizardUtils
 * @see FormFlowRoutes
 */

import { useCallback, useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useLocation } from "react-router-dom";
import { mergeSessionStepWithRouteConfig } from "./checkPageUtils";
import {
  buildWizardSteps,
  createWizardGoNext,
  getWizardBasePath,
} from "./formWizardUtils";

/**
 * @param {object} options
 * @param {Array|null} options.mdmsData - Resolved master array
 * @param {boolean} options.isLoading
 * @param {string} options.indexRoute
 * @param {string} options.sessionKey
 * @param {string[]} options.terminalSegments
 * @param {boolean} [options.multiStepNavigation=true]
 * @param {string} [options.invalidateQueryKey]
 * @param {Function} [options.transformSession]
 * @param {Function} [options.onAfterSelect] - Side effect after session merge (e.g. TL flag)
 * @param {"citizen"|"employee"} [options.audience="employee"]
 * @param {Set<string>|string[]} [options.skipComponents]
 * @param {string} [options.docsComponent]
 * @param {string} [options.formComponent]
 * @param {string} [options.sessionStepKey]
 * @param {*} [options.checkFlow] - Passed through for EST check pages
 * @param {Function} [options.buildSuccessAckState] - (response, params) => ack state
 * @returns {object}
 */
const useFormWizard = ({
  mdmsData,
  isLoading,
  indexRoute,
  sessionKey,
  terminalSegments,
  multiStepNavigation = true,
  invalidateQueryKey,
  transformSession,
  onAfterSelect,
  audience = "employee",
  skipComponents,
  docsComponent,
  formComponent,
  sessionStepKey,
  checkFlow,
  buildSuccessAckState,
}) => {
  const location = useLocation();
  const { pathname } = location;
  const navigate = Digit.Hooks.useCustomNavigate();
  const match = Digit.Hooks.useModuleBasePath();
  const queryClient = useQueryClient();

  const [params, setParams, clearParams] = Digit.Hooks.useSessionStorage(
    sessionKey,
    {}
  );

  const config = useMemo(
    () =>
      buildWizardSteps(mdmsData, {
        indexRoute,
        audience,
        skipComponents,
        docsComponent,
        formComponent,
        sessionKey: sessionStepKey,
      }),
    [
      mdmsData,
      indexRoute,
      audience,
      skipComponents,
      docsComponent,
      formComponent,
      sessionStepKey,
    ]
  );

  const getBasePath = useCallback(
    () => getWizardBasePath(pathname, match?.pathnameBase, terminalSegments),
    [pathname, match, terminalSegments]
  );

  const goNext = useCallback(
    createWizardGoNext({
      pathname,
      config,
      navigate,
      multiStep: multiStepNavigation,
    }),
    [pathname, config, navigate, multiStepNavigation]
  );

  const handleSelect = useCallback(
    (key, data, skipStep, index, isAddMultiple = false) => {
      setParams((prev) => {
        const merged = mergeSessionStepWithRouteConfig(prev, key, data);
        return typeof transformSession === "function"
          ? transformSession(merged)
          : merged;
      });
      if (typeof onAfterSelect === "function") {
        try {
          onAfterSelect(key, data);
        } catch (e) {
          /* ignore module side-effect errors */
        }
      }
      goNext(skipStep, index, isAddMultiple, key);
    },
    [setParams, goNext, transformSession, onAfterSelect]
  );

  const onAckSuccess = useCallback(() => {
    clearParams();
    if (invalidateQueryKey) queryClient.invalidateQueries(invalidateQueryKey);
  }, [clearParams, queryClient, invalidateQueryKey]);

  const onUpdateSuccess = useCallback(() => {
    clearParams();
    if (invalidateQueryKey) queryClient.invalidateQueries(invalidateQueryKey);
  }, [clearParams, queryClient, invalidateQueryKey]);

  const onCheckSuccess = useCallback(
    (response) => {
      let ackState = { data: response, isSuccess: true };
      try {
        if (buildSuccessAckState) {
          ackState = buildSuccessAckState(response, params);
          if (!ackState || typeof ackState !== "object") {
            ackState = { data: response, isSuccess: true };
          }
          ackState = { ...ackState, isSuccess: true };
        }
      } catch (err) {
        console.error("Form wizard ack state build failed after successful submit:", err);
        ackState = {
          data: response,
          isSuccess: true,
          ackBuildError: String(err?.message || err),
        };
      }

      clearParams();
      if (invalidateQueryKey) queryClient.invalidateQueries(invalidateQueryKey);
      navigate(`${getBasePath()}/acknowledgement`, { state: ackState });
    },
    [
      clearParams,
      queryClient,
      invalidateQueryKey,
      buildSuccessAckState,
      params,
      navigate,
      getBasePath,
    ]
  );

  const onCheckError = useCallback(
    (error) => {
      const status = error?.response?.status;
      const body = error?.response?.data;
      if (status >= 200 && status < 300) {
        console.warn(
          "Form wizard submit treated error with 2xx status as success",
          status,
          error
        );
        const ackState = buildSuccessAckState
          ? (() => {
              try {
                return { ...buildSuccessAckState(body, params), isSuccess: true };
              } catch {
                return { data: body, isSuccess: true };
              }
            })()
          : { data: body, isSuccess: true };
        clearParams();
        navigate(`${getBasePath()}/acknowledgement`, { state: ackState });
        return;
      }

      navigate(`${getBasePath()}/acknowledgement`, {
        state: {
          data: null,
          isSuccess: false,
          error: {
            message: error?.message || "COMMON_APPLICATION_FAILED",
            status,
          },
        },
      });
    },
    [navigate, getBasePath, buildSuccessAckState, params, clearParams]
  );

  const isReady =
    !isLoading &&
    (Array.isArray(mdmsData) ? mdmsData.length > 0 : Boolean(mdmsData)) &&
    config.length > 0;

  return {
    config,
    params,
    setParams,
    clearParams,
    location,
    match,
    getBasePath,
    goNext,
    handleSelect,
    onAckSuccess,
    onUpdateSuccess,
    onCheckSuccess,
    onCheckError,
    checkFlow,
    isReady,
  };
};

export default useFormWizard;
