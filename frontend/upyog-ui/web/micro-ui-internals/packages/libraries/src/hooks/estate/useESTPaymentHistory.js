import { useQuery } from "react-query";

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
