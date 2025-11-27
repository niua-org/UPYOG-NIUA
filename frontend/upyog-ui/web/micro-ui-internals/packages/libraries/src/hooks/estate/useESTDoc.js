import { useQuery, useQueryClient } from "react-query";
import { MdmsService } from "../../services/elements/MDMS";

/**
 * useESTDoc Hook
 * 
 * This hook is used to fetch EST-related document configurations from MDMS.
 * It supports two types of fetch:
 * 
 * 1. `Documents` → Fetches required document screen data.
 * 2. Default     → Fetches multiple types from MDMS.
 * 
 * Parameters:
 * - tenantId: The ULB / tenant code.
 * - moduleCode: MDMS module name.
 * - type: "Documents" OR any other type that needs to fetch multiple types.
 * - config: Optional react-query config (staleTime, cacheTime, etc.).
 * 
 * Returns:
 * - { isLoading, error, data, revalidate }
 *   Revalidate will re-fetch query using react-query client.invalidateQueries()
 */

// Custom hook to fetch EST documents or multiple types from MDMS
// This hook provides a query interface for retrieving EST-related documents
// or other types of data based on the provided type parameter.

const useESTDoc = (tenantId, moduleCode, type, config = {}) => {
  const client = useQueryClient();

  // ---------- API FUNCTIONS ----------
  const fetchDocumentsRequiredScreen = () =>
    MdmsService.getESTDocuments(tenantId, moduleCode);

  const fetchMultipleTypes = () =>
    MdmsService.getMultipleTypes(tenantId, moduleCode, type);

  // ---------- QUERY KEYS ----------
  const keyDocuments = ["EST_DOCUMENT_REQ_SCREEN", tenantId, moduleCode];
  const keyMultipleTypes = ["EST_MULTIPLE_TYPES", tenantId, moduleCode, type];

  // ---------- QUERY ----------
  const { isLoading, error, data } =
    type === "Documents"
      ? useQuery(keyDocuments, fetchDocumentsRequiredScreen, config)
      : useQuery(keyMultipleTypes, fetchMultipleTypes, config);

  // ---------- REVALIDATE ----------
  const revalidate = () => {
    const key = type === "Documents" ? keyDocuments : keyMultipleTypes;
    client.invalidateQueries(key);
  };

  return { isLoading, error, data, revalidate };
};

export default useESTDoc;
