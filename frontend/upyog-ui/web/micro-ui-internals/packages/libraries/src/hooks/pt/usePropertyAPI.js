import { PTService } from "../../services/elements/PT";
import { mutationTemplate } from "../../common/mutationTemplate";

const usePropertyAPI = (tenantId, type = true) => {
  if (type) {
    return mutationTemplate({ mutationFn: (data) => PTService.create(data, tenantId) });
  } else {
    return mutationTemplate({ mutationFn: (data) => PTService.update(data, tenantId) });
  }
};

export default usePropertyAPI;
