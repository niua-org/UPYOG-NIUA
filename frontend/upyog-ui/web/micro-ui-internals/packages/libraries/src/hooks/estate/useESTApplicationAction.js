import { mutationTemplate } from "../../common/mutationTemplate";

const useESTApplicationAction = (tenantId) => {
  return mutationTemplate({
    mutationFn: (applicationData) =>
      Digit.ESTService.applicationAction(applicationData, tenantId),
  });
};

export default useESTApplicationAction;