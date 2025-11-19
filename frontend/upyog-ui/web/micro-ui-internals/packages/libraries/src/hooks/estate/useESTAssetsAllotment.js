import { useQuery, useMutation } from "react-query";
import { ESTService } from"../../services/elements/EST"

export const useESTAssetsAllotment = (tenantId, type = true) => {
  if (type) {
    return useMutation((data) => ESTService.allotmentcreate(data, tenantId));
  } 
  else {
    return useMutation((data) => ESTService.update(data, tenantId));
  }
};

export default useESTAssetsAllotment;
