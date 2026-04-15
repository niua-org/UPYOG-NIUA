import { useQuery, useQueryClient } from "react-query";

/**
 * Fetches challan generation search results based on filters.
 *
 * - Formats filter values (status, businessService) into comma-separated strings.
 * - Calls Digit.ChallanGenerationService.search API.
 * - Uses React Query for caching, fetching, and state management.
 * - Provides revalidate function to manually refresh cached data.
 *
 * @param {Object} input
 * @param {string} input.tenantId - Tenant identifier
 * @param {Object} input.filters - Search filters
 * @param {boolean} input.isChallanGenerationAppChanged - Trigger for refetch
 * @param {Object} config - Optional React Query config
 *
 * @returns {Object} { isLoading, error, data, revalidate }
 */

const useChallanGenerationSearch = ({ tenantId, filters, isChallanGenerationAppChanged }, config = {}) => {
  if (filters.status && filters.status.length > 0) {
    filters.status = filters.status.toString();
  } else if (filters.status && filters.status.length === 0) {
    delete filters.status;
  }

  if (filters.businessService && filters.businessService.length > 0) {
    filters.businessService = filters.businessService.toString();
  } else if (filters.businessService && filters.businessService.length === 0) {
    delete filters.businessService;
  }

  const client = useQueryClient();
  const args = tenantId ? { tenantId, filters } : { filters };
  const { isLoading, error, data } = useQuery(
    ["challanGenerationSearchList", tenantId, filters, isChallanGenerationAppChanged],
    () => Digit.ChallanGenerationService.search(args),
    config
  );
  return { isLoading, error, data, revalidate: () => client.invalidateQueries(["propertySearchList", tenantId, filters]) };
};

export default useChallanGenerationSearch;
