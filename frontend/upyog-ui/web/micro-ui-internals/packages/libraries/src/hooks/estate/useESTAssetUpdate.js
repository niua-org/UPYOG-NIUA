import { mutationTemplate } from "../../common/mutationTemplate";
import { ESTService } from "../../services/elements/EST";

export const useESTAssetUpdate = () => {
  return mutationTemplate({
    mutationFn: (data) => ESTService.updateAsset(data),
  });
};