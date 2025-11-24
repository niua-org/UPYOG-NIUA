import { useMutation } from "react-query";
import { ESTService } from "../../services/elements/EST";

const useESTAllotAsset = (tenantId) => {
  return useMutation((data) => ESTService.allotment(data, tenantId));
};

export default useESTAllotAsset;  // for allotment of the Asset to Citizen  
