import { useMutation } from "react-query";
import { ESTService } from "../../services/elements/EST";

/**** This hook is used to create an  new asset. ****/

// Custom hook to create or update EST entities
// This hook provides a mutation interface for creating new EST entities
// or updating existing ones based on the provided type flag.

export const useESTCreateAPI = (tenantId, type = true) => {
  return useMutation((data) => ESTService.create(data, tenantId));
};

export default useESTCreateAPI;
