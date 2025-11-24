import { useQuery } from "react-query";

// Custom hook to fetch EST application details
// This hook provides a query interface for retrieving detailed information about a specific
//  EST application using the application number and tenant ID in the Estate module.

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