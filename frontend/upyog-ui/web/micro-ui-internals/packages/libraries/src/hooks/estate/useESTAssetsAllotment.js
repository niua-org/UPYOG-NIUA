import { useMutation } from "react-query";
import { ESTService } from "../../services/elements/EST";

/*** This Hooks is used to create an allotment for an asset. ***/

// Custom hook to manage EST assets allotment
// This hook provides a mutation interface for creating or updating
// asset allotments in the Estate module.

export const useESTAssetsAllotment = (tenantId) => {
  return useMutation((data) => ESTService.allotmentcreate(data, tenantId));
};

export default useESTAssetsAllotment;
