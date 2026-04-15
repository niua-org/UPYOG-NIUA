import { useQuery, useQueryClient } from "react-query";

/**
 * Fetches challan bill search results using React Query.
 *
 * - Calls Digit.ChallanGenerationService.search_bill API.
 * - Uses tenantId and filters for query key (caching & refetching).
 * - Provides revalidate function to manually refresh data.
 *
 * @param {Object} input
 * @param {string} input.tenantId - Tenant identifier
 * @param {Object} input.filters - Search filters
 * @param {Object} config - Optional React Query config
 *
 * @returns {Object} { isLoading, error, data, revalidate }
 */

const useChallanGenerationSearchBill = ({ tenantId, filters }, config = {}) => {
  const client = useQueryClient();
  const args = tenantId ? { tenantId, filters } : { filters };
  const { isLoading, error, data } = useQuery(["billSearchList", tenantId, filters], () => Digit.ChallanGenerationService.search_bill(args), config);
  return { isLoading, error, data, revalidate: () => client.invalidateQueries(["billSearchList", tenantId, filters]) };
};

export default useChallanGenerationSearchBill;
