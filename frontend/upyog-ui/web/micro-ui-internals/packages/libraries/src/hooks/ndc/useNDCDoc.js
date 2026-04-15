import { MdmsService } from "../../services/elements/MDMS";
import { useQuery } from "react-query";
// hook to fetch documents from MDMS 
const useNDCDoc = (tenantId, moduleCode, type, config = {}) => {
  
  const useNDCDocumentsRequiredScreen = () => {
    return useQuery("NDC_DOCUMENT_REQ_SCREEN", () => MdmsService.getNDCDocuments(tenantId, moduleCode), config);
  };
  const _default = () => {
    return useQuery([tenantId, moduleCode, type], () => MdmsService.getMultipleTypes(tenantId, moduleCode, type), config);
  };

  switch (type) {
    
    case "Documents":
      return useNDCDocumentsRequiredScreen();
    
    default:
      return _default();
  }
};

export default useNDCDoc;
