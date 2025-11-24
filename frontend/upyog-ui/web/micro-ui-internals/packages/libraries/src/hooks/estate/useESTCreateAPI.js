import { useQuery, useMutation } from "react-query";
import { ESTService } from"../../services/elements/EST"

// Custom hook to create or update EST entities
// This hook provides a mutation interface for creating new EST entities
// or updating existing ones based on the provided type flag.

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