import { useQuery, useMutation } from "react-query";
import { ESTService } from"../../services/elements/EST"

export const useESTCreateAPI = (tenantId, type = true) => {
  if (type) {
    return useMutation((data) => ESTService.create(data, tenantId));
  } 
  else {
    // return useMutation((data) => ESTService.update(data, tenantId));
       return useMutation((data) => ESTService.allotmentcreate(data, tenantId));
  }
};

export default useESTCreateAPI;