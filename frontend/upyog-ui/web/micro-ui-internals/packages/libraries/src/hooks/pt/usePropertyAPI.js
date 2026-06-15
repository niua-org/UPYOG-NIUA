import { PTService } from "../../services/elements/PT";
import { mutationTemplate } from "../../common/mutationTemplate";

const usePropertyAPI = (tenantId, type = true, config = {}) => {
  if (type) {
    return mutationTemplate({ mutationFn: (data) => PTService.create(data, tenantId), config });
  } else {
    return mutationTemplate({ mutationFn: (data) => PTService.update(data, tenantId), config });
  }
};

export default usePropertyAPI;
