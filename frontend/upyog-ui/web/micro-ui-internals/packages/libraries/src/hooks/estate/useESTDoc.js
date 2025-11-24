import { MdmsService } from "../../services/elements/MDMS";
import { useQuery } from "react-query";

// Custom hook to fetch EST documents or multiple types from MDMS
// This hook provides a query interface for retrieving EST-related documents
// or other types of data based on the provided type parameter.

const useESTDoc = (tenantId, moduleCode, type, config = {}) => {
  
  const useESTDocumentsRequiredScreen = () => {
    return useQuery("EST_DOCUMENT_REQ_SCREEN", () => MdmsService.getESTDocuments(tenantId, moduleCode), config);
  };
  const _default = () => {
    return useQuery([tenantId, moduleCode, type], () => MdmsService.getMultipleTypes(tenantId, moduleCode, type), config);
  };

  switch (type) {
    
    case "Documents":
      return useESTDocumentsRequiredScreen();
    
    default:
      return _default();
  }
};

export default useESTDoc;
