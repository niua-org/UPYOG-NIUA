import { queryTemplate } from "../../common/queryTemplate";
import { DSSService } from "../../services/elements/DSS";

const useGetCustomFilterValues = (filterConfigs, config = {}) => {
  const queryKey = [`DSS_CUSTOM_FILTER_CONFIG_${JSON.stringify(filterConfigs)}`];
  const queryFn = () => DSSService.getFiltersConfigData(filterConfigs);

  return queryTemplate({
    queryKey,
    queryFn,
    config,
  });
};

export default useGetCustomFilterValues;
