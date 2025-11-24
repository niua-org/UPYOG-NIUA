import { useQuery } from "react-query";

// Hook to search for EST bills based on tenantId and consumerCode
// Returns loading state, error, data, and a refetch function

const useESTBillSearch = ({ tenantId, consumerCode, businessService = "est-services", config = {} }) => {
  const { isLoading, error, data, refetch } = useQuery(
    ["EST_BILL_SEARCH", tenantId, consumerCode, businessService],
    () => Digit.ESTService.fetchBill({ tenantId, consumerCode, businessService }),
    {
      ...config,
      enabled: !!(tenantId && consumerCode),
    }
  );

  return { isLoading, error, data, refetch };
};

export default useESTBillSearch;
