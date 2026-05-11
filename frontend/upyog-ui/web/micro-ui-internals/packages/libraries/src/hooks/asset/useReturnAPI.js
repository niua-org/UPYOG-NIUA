import { mutationTemplate } from "../../common/mutationTemplate";
import { ASSETService } from "../../services/elements/ASSET";

export const useReturnAPI = (tenantId, type = true) => {
  const mutationFn = (data) => {
    if (type) {
      return ASSETService.return_asset(data, tenantId);
    }
    return ASSETService.update(data, tenantId);
  };

  return mutationTemplate({ mutationFn });
};

export default useReturnAPI;