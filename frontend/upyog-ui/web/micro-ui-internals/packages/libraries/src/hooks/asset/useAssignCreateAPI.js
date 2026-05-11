import { mutationTemplate } from "../../common/mutationTemplate";
import { ASSETService } from"../../services/elements/ASSET"


export const useAssignCreateAPI = (tenantId, type = true) => {
  const mutationFn = (data) =>
    type
      ? ASSETService.assign(data, tenantId)
      : ASSETService.update(data, tenantId);

  return mutationTemplate({ mutationFn });
};

export default useAssignCreateAPI;
