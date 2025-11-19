import { useQuery } from "react-query";

const useESTAssetSearch = ({ tenantId, filters = {}, config = {} }) => {
  return useQuery(
    ["EST_ASSET_SEARCH", tenantId, filters],
    () => Digit.ESTService.assetSearch({ tenantId, filters }),
    {
      ...config,
      onSuccess: (data) => {
        console.log("EST Asset Search Response:", data);
      },
      onError: (error) => {
        console.error("EST Asset Search Error:", error);
      }
    }
  );
};

export default useESTAssetSearch;