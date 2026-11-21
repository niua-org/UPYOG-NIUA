import { useMemo } from "react";

/**
 * Custom inbox hook for Family / Profile module.
 * Wraps Digit.Hooks.useInbox to query Municipal Services Inbox (/inbox/v1/_search)
 * with family-service businessService, filter handling, and response transformation.
 *
 * @param {Object} params - Hook parameters.
 * @param {string} params.tenantId - Tenant identifier.
 * @param {Object} [params.filters={}] - Filter criteria.
 * @param {string} [params.filters.familyId] - Family ID filter.
 * @param {string} [params.filters.mobileNumber] - Mobile number filter.
 * @param {Array<string>} [params.filters.applicationStatus] - Application status array.
 * @param {number} [params.filters.limit=10] - Number of records per page.
 * @param {number} [params.filters.offset=0] - Pagination offset.
 * @param {string} [params.filters.sortBy="createdTime"] - Sort field.
 * @param {string} [params.filters.sortOrder="DESC"] - Sort direction.
 * @param {Object} [params.config={}] - React Query configuration options.
 * @returns {Object} React Query result containing transformed inbox data:
 *   - data.searchData: Array of family application records.
 *   - data.workflowData: Workflow process instance mapping.
 *   - data.statusMap: Status distribution array with record counts.
 *   - data.totalCount: Total count of matching applications.
 *   - data.nearingSlaCount: Applications nearing SLA limit.
 *   - data.dataEmpty: Boolean flag indicating if result is empty.
 */
const useInbox = ({ tenantId, filters = {}, config = {} }) => {
  const {
    familyId,
    mobileNumber,
    applicationStatus,
    limit = 10,
    offset = 0,
    sortBy = "createdTime",
    sortOrder = "DESC",
  } = filters;

  const processSearchCriteria = useMemo(() => {
    const criteria = {
      businessService: ["family-service"],
      moduleName: "family-service",
    };
    return criteria;
  }, []);

  const moduleSearchCriteria = useMemo(() => {
    const criteria = { sortBy, sortOrder };
    if (familyId && familyId.trim()) criteria.familyId = familyId.trim();
    if (mobileNumber && mobileNumber.trim()) criteria.mobileNumber = mobileNumber.trim();
    if (applicationStatus && applicationStatus.length > 0) {
      criteria.applicationStatus = applicationStatus;
    }
    return criteria;
  }, [familyId, mobileNumber, applicationStatus, sortBy, sortOrder]);

  const inboxFilters = useMemo(
    () => ({
      tenantId,
      processSearchCriteria,
      moduleSearchCriteria,
      limit,
      offset,
    }),
    [tenantId, processSearchCriteria, moduleSearchCriteria, limit, offset]
  );

  const query = Digit.Hooks.useInbox({
    tenantId,
    filters: inboxFilters,
    config: {
      select: (data) => {
        const items = data?.items || [];
        const totalCount = data?.totalCount || 0;
        const nearingSlaCount = data?.nearingSlaCount || 0;
        const statusMap = data?.statusMap || [];

        if (items.length) {
          return items.map((obj) => ({
            searchData: obj.businessObject,
            workflowData: obj.ProcessInstance,
            statusMap,
            totalCount,
            nearingSlaCount,
          }));
        }
        return [{ statusMap, totalCount, nearingSlaCount, dataEmpty: true }];
      },
      ...config,
      ...(config?.enabled !== undefined
        ? {
            enabled:
              typeof config.enabled === "function"
                ? config.enabled
                : Boolean(config.enabled),
          }
        : {}),
    },
  });

  return query;
};

export default useInbox;

