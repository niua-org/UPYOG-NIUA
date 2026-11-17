import { queryTemplate } from "../../common/queryTemplate";

/**
 * Hook to fetch family details for QR code verification and search.
 * Searches family records using `Urls.profile.familySearch` via `UserService`.
 * Enabled only when both tenantId and familyId query parameters are present.
 * @param {Object} queryProps - Query parameters.
 * @param {string} queryProps.tenantId - ULB / state tenant identifier.
 * @param {string} queryProps.familyId - Unique family identifier (e.g. FAM-2026-000064).
 * @param {Object} [config={}] - Additional react-query configuration.
 * @returns {Object} React Query result containing family data, loading, and error states.
 */
export const useFamilyQrCode = ({ tenantId, ...params }, config = {}) => {
  return queryTemplate({
    queryKey: ["family_Qr_Search", { tenantId, params }, config],
    queryFn: () => Digit.UserService.familySearch(tenantId, params),
    config: {
      enabled: Boolean(tenantId && params?.familyId),
      refetchOnMount: false,
      ...config,
    },
  });
};

