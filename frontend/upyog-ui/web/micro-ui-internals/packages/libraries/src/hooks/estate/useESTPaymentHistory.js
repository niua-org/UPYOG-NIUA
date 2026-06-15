import { queryTemplate } from "../../common/queryTemplate";

const useESTPaymentHistory = ({ tenantId, filters, config = {} }) => {
  return queryTemplate({
    queryKey: ["EST_PAYMENT_HISTORY", tenantId, filters],
    queryFn: () =>
      Digit.ESTService.paymentHistory({ tenantId, filters }),
    config,
  });
};

export default useESTPaymentHistory;