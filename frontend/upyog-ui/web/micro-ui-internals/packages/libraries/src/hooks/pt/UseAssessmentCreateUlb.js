import { PTService } from "../../services/elements/PT";
import { mutationTemplate } from "../../common/mutationTemplate";

const UseAssessmentCreateUlb = (tenantId, config = {}) => {
  console.log("Using AssessmentCreateUlb",tenantId)
  const mutationFn = (data) => PTService.assessmentCreateUlb(data, tenantId);
  return mutationTemplate({ mutationFn });
};

export default UseAssessmentCreateUlb;
