import { Surveys } from "../../services/elements/Surveys";
import { mutationTemplate } from "../../common/mutationTemplate";

const useCreateSurveys = (filters, config) => {
  return mutationTemplate({
    mutationFn: (filters) => Surveys.create(filters),
    config,
  });
};

export default useCreateSurveys;
