import { useQuery } from "react-query";
import { MdmsService } from "../../services/elements/MDMS";

/**
 * Fetches MDMS data for challan generation based on type.
 *
 * - Supports fetching:
 *   • BusinessService (billing services)
 *   • applicationStatus
 * - Calls respective MdmsService APIs using React Query.
 * - Uses type to determine which query to execute.
 *
 * @param {string} tenantId - Tenant identifier
 * @param {string} moduleCode - MDMS module name
 * @param {string} type - Data type (e.g., "BusinessService", "applicationStatus")
 * @param {Object} filter - MDMS query filters
 * @param {Object} config - Optional React Query config
 *
 * @returns {Object|null} Query result or null if type is unsupported
 */

const useChallanGenerationMDMS = (tenantId, moduleCode, type, filter, config = {}) => {
  const useChallanGenerationBillingService = () => {
    return useQuery("CHALLANGENERATION_BILLING_SERVICE", () => MdmsService.getChallanGenerationBillingService(tenantId, moduleCode, type, filter), config);
  };
  const useChallanGenerationApplcationStatus = () => {
    return useQuery("CHALLANGENERATION_APPLICATION_STATUS", () => MdmsService.getChallanGenerationApplcationStatus(tenantId, moduleCode, type, filter), config);
  };

  switch (type) {
    case "BusinessService":
      return useChallanGenerationBillingService();
    case "applicationStatus":
      return useChallanGenerationApplcationStatus();
    default:
      return null;
  }
};

export default useChallanGenerationMDMS;
