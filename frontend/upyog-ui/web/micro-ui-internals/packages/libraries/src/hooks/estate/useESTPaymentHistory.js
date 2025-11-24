import { useQuery } from "react-query";

// Custom hook to fetch EST payment history
// This hook provides a query interface for retrieving payment history
// related to EST based on tenant ID and filters.

const useESTPaymentHistory = ({ tenantId, filters, config = {} }) => {
  return useQuery(
    ["EST_PAYMENT_HISTORY", tenantId, filters],
    () => Digit.ESTService.paymentHistory({ tenantId, filters }),
    {
      ...config,
    }
  );
};

export default useESTPaymentHistory;
