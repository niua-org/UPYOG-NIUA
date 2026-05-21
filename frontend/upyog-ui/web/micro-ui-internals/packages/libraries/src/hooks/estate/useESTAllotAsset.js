import { mutationTemplate } from "../../common/mutationTemplate";
import { ESTService } from "../../services/elements/EST";

const useESTAllotAsset = (tenantId) => {
  return mutationTemplate({
    mutationFn: (data) => ESTService.allotment(data, tenantId),
  });
};

export default useESTAllotAsset;