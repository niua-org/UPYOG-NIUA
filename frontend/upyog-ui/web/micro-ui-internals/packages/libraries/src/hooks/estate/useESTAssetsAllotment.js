import { useQuery, useMutation } from "react-query";
import { ESTService } from"../../services/elements/EST"

// Custom hook to manage EST assets allotment
// This hook provides a mutation interface for creating or updating
// asset allotments in the Estate module.

export const useESTAssetsAllotment = (tenantId) => {
  return useMutation((data) => ESTService.allotmentcreate(data, tenantId));
};

export default useESTAssetsAllotment;
