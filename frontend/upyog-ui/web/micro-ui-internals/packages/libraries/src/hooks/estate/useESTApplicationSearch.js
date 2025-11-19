import { useQuery } from "react-query";
import { ESTService } from "../../services/elements/EST";

const useESTApplicationSearch = ({ tenantId, filters, auth, config = {} }) => {
  return useQuery(
    ["EST_APPLICATION_SEARCH", tenantId, filters],
    () => ESTService.applicationSearch({ tenantId, filters, auth }),
    config
  );
};

export default useESTApplicationSearch;
