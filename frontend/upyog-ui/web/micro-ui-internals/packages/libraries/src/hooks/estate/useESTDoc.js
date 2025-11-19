import { MdmsService } from "../../services/elements/MDMS";
import { useQuery } from "react-query";

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
