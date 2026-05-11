import { mutationTemplate } from "../../common/mutationTemplate";
import { ASSETService } from "../../services/elements/ASSET"



export const useEditUpdateAPI = (tenantId, type = true) => {
  const mutationFn = (data) =>
    type
      ? ASSETService.update(data, tenantId)
      : ASSETService.create(data, tenantId);

  return mutationTemplate({ mutationFn });
};

export default useEditUpdateAPI;
