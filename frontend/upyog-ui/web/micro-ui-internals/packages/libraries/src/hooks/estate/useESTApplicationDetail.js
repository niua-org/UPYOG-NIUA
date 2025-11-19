import { useQuery } from "react-query";

const useESTApplicationDetail = ({ tenantId, applicationNumber, config = {} }) => {
  return useQuery(
    ["EST_APPLICATION_DETAIL", tenantId, applicationNumber],
    () => Digit.ESTService.applicationDetail({ tenantId, applicationNumber }),
    {
      ...config,
    }
  );
};

export default useESTApplicationDetail;
