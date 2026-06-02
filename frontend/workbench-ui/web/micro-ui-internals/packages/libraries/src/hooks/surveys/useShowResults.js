import { Surveys } from "../../services/elements/Surveys";
import { mutationTemplate } from "../../common/mutationTemplate";

const useShowResults = (filters, config) => {
  return mutationTemplate({
    mutationFn: (filters) => Surveys.showResults(filters),
    config,
  });
};

export default useShowResults;
