import { queryTemplate } from "../../common/queryTemplate";

const useESTApplicationDetail = ({ tenantId, applicationNumber, config = {} }) => {
  return queryTemplate({
    queryKey: ["EST_APPLICATION_DETAIL", tenantId, applicationNumber],
    queryFn: () =>
      Digit.ESTService.applicationDetail({ tenantId, applicationNumber }),
    config,
  });
};

export default useESTApplicationDetail;