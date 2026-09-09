import { Surveys } from "../../services/elements/Surveys";
import { mutationTemplate } from "../../common/mutationTemplate";

const useUpdateSurvey = (filters, config) => {
  console.log("updateSurvey");
  return mutationTemplate({ mutationFn: (filters) => Surveys.updateSurvey(filters) });
};

export default useUpdateSurvey;
