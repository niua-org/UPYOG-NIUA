import { queryTemplate } from "../../common/queryTemplate";
import { MdmsService } from "../../services/elements/MDMS";

const useESTDoc = (tenantId, moduleCode, type, config = {}) => {
  const isDocument = type === "Documents";

  return queryTemplate({
    queryKey: isDocument
      ? ["EST_DOCUMENT_REQ_SCREEN", tenantId, moduleCode]
      : ["EST_MULTIPLE_TYPES", tenantId, moduleCode, type],

    queryFn: () =>
      isDocument
        ? MdmsService.getESTDocuments(tenantId, moduleCode)
        : MdmsService.getMultipleTypes(tenantId, moduleCode, type),

    config,
  });
};

export default useESTDoc;