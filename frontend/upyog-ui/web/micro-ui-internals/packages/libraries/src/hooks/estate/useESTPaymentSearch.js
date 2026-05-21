import { queryTemplate } from "../../common/queryTemplate";

const useESTPaymentSearch = ({ tenantId, filters, auth, searchedFrom = "" }, config = {}) => {
  const args = tenantId ? { tenantId, filters, auth } : { filters, auth };

  const defaultSelect = (data) => {
    if (data?.Payments?.length > 0) {
      data.Payments[0].paymentId = data.Payments[0].paymentId || [];
    }
    return data;
  };

  return queryTemplate({
    queryKey: ["estPaymentSearchList", tenantId, filters, auth],
    queryFn: () => Digit.ESTService.paymentSearch(args),
    select: defaultSelect,
    config,
  });
};

export default useESTPaymentSearch;