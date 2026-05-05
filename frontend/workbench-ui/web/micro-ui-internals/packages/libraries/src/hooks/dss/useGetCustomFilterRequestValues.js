import { queryTemplate } from "../../common/queryTemplate";
import { DSSService } from "../../services/elements/DSS";

const useGetCustomFilterRequestValues = (filterConfigs, config = {}) => {
  const queryKey = [`DSS_CUSTOM_FILTER_REQUEST_VAL_${JSON.stringify(filterConfigs)}`];
  const queryFn = () => DSSService.getCustomFiltersDynamicValues(filterConfigs);

  return queryTemplate({
    queryKey,
    queryFn,
    config,
  });
};

export default useGetCustomFilterRequestValues;
