/**
 * ==============================================================================
 * useThemeConfig Hook
 * ==============================================================================
 *
 * Custom React Query hook to fetch dynamic theme configurations from MDMS API
 * (`/mdms-v2/v1/theme-config/_search`) with automatic fallback to `tailwind.theme.json`.
 *
 * ------------------------------------------------------------------------------
 * HOW TO USE IN COMPONENTS:
 * ------------------------------------------------------------------------------
 * import React from "react";
 *
 * const MyComponent = () => {
 *   // Automatically detects tenantId and themeType ("CITIZEN" / "EMPLOYEE")
 *   const { data: themeData, isLoading } = Digit.Hooks.useThemeConfig();
 *
 *   // Or provide custom parameters:
 *   // const { data } = Digit.Hooks.useThemeConfig({ tenantId: "pg.citya", themeType: "CITIZEN" });
 *
 *   return (
 *     <div className="bg-primary-main text-white p-md rounded-md">
 *       Hello World
 *     </div>
 *   );
 * };
 * ==============================================================================
 */

import { queryTemplate } from "../common/queryTemplate";
import ThemeService from "../services/elements/Theme";
import { ThemeTypes, ThemeStatus } from "../enums/Theme";

/**
 * @param {Object} [params={}] - Query parameters
 * @param {string} [params.tenantId] - Specific tenantId (defaults to active stateId)
 * @param {string} [params.themeType] - ThemeTypes.CITIZEN or ThemeTypes.EMPLOYEE (auto-detected from path)
 * @param {string} [params.status=ThemeStatus.DEFAULT] - Theme status
 * @param {Object} [config={}] - React Query configuration options
 * @returns {Object} React Query hook response object
 */
const useThemeConfig = (params = {}, config = {}) => {
  const tenantId = params?.tenantId || Digit.ULBService.getStateId();
  const themeType =
    params?.themeType ||
    (typeof window !== "undefined" && window.location.pathname.includes("employee")
      ? ThemeTypes.EMPLOYEE
      : ThemeTypes.CITIZEN);
  const status = params?.status || ThemeStatus.DEFAULT;

  return queryTemplate({
    queryKey: [tenantId, themeType, status],
    queryFn: () => ThemeService.getTheme({ tenantId, themeType, status }),
    config,
  });
};

export default useThemeConfig;

