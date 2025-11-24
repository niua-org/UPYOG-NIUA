import { useQuery } from "react-query";
import { ESTService } from "../../services/elements/EST";

// Custom hook to search EST applications
// This hook provides a query interface for searching EST applications
// based on various filters and tenant ID.

const useESTApplicationSearch = ({ filters, config = {} }) => {
  return useQuery(
    ["EST_APPLICATION_SEARCH", filters],
    () => ESTService.allotmentSearch({ tenantId: filters?.tenantId, filters }),
    {
      enabled: !!filters,
      ...config
    }
  );
};

export default useESTApplicationSearch;
