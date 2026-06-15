import { queryTemplate } from "../../common/queryTemplate";

const useESTBillSearch = ({ tenantId, consumerCode, businessService = "est-services", config = {} }) => {
  return queryTemplate({
    queryKey: ["EST_BILL_SEARCH", tenantId, consumerCode, businessService],
    queryFn: () =>
      Digit.ESTService.fetchBill({ tenantId, consumerCode, businessService }),
    enabled: !!(tenantId && consumerCode),
    config,
  });
};

export default useESTBillSearch;