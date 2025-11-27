import { useQuery } from "react-query";

// Custom hook to search EST assets
// This hook provides a query interface for searching EST assets
// based on various filters and tenant ID in the Estate module.

const useESTAssetSearch = ({ tenantId, filters = {}, config = {} }) => {
  return useQuery(
    ["EST_ASSET_SEARCH", tenantId, filters],
    () => Digit.ESTService.assetSearch({ tenantId, filters }),
    config
  );
};

export default useESTAssetSearch;