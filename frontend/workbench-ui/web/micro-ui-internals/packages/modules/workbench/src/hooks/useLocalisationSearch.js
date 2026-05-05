import { useQueryClient} from "../../../../libraries/src/common/queryClientTemplate";
import { queryTemplate } from "../../../../libraries/src/common/queryTemplate";
import { LocalisationSearch } from "../utils/LocalisationSearch";

const useLocalisationSearch = ({url, params, body, config = {}, plainAccessRequest,changeQueryName="Random",state }) => {
  const client = useQueryClient();
  const CustomService = Digit.CustomService
  const { isLoading, data, isFetching, refetch, error } = queryTemplate({
    queryKey: [url, changeQueryName].filter((e) => e),
    queryFn: () => LocalisationSearch.fetchResults({ url, params, body, plainAccessRequest, state }),
    config: {
      cacheTime: 0,
      ...config,
    },
  });

  return {
    isLoading,
    isFetching,
    data,
    refetch,
    revalidate: () => {
      data && client.invalidateQueries({ queryKey: [url].filter((e) => e) });
    },
    error
  };
};



export default useLocalisationSearch;