import { useQuery, useQueryClient } from "react-query";

const useChallanGenerationCount = (tenantId, config = {}) => {
  return useQuery(["ChallanGeneration_COUNT", tenantId], () => Digit.ChallanGenerationService.count(tenantId), config);
};

/**
 * Fetches challan generation count for a given tenant using React Query.
 *
 * - Calls Digit.ChallanGenerationService.count API.
 * - Uses tenantId in query key for caching and refetching.
 *
 * @param {string} tenantId - Tenant identifier
 * @param {Object} config - Optional React Query config
 *
 * @returns {Object} Query result (data, isLoading, error, etc.)
 */

export default useChallanGenerationCount;
