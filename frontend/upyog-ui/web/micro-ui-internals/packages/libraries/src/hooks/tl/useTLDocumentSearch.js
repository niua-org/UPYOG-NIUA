import { queryTemplate } from "../../common/queryTemplate";
import { useQueryClient } from "../../common/queryClientTemplate";

const fileId = (doc) => {
  if (doc == null || doc === "") return "";
  if (typeof doc === "string") return doc;
  return doc.fileStoreId || doc.filestoreId || doc.documentuuid || "";
};

const collectFileIds = (value = {}) => {
  const ids = [];
  const push = (doc) => {
    const id = fileId(doc);
    if (id && !ids.includes(id)) ids.push(id);
  };

  const applicationDocs = value?.tradeLicenseDetail?.applicationDocuments;
  if (Array.isArray(applicationDocs)) applicationDocs.forEach(push);

  if (Array.isArray(value?.workflowDocs)) value.workflowDocs.forEach(push);

  const sessionDocs = value?.owners?.documents;
  if (sessionDocs && typeof sessionDocs === "object") {
    Object.values(sessionDocs).forEach(push);
  }

  const owners = value?.tradeLicenseDetail?.owners;
  if (Array.isArray(owners)) {
    owners.forEach((owner) => {
      if (Array.isArray(owner?.documents)) owner.documents.forEach(push);
    });
  }

  return ids;
};

const useTLDocumentSearch = (data1 = {}) => {
  const client = useQueryClient();
  const tenant = Digit.ULBService.getStateId();
  const filesArray = collectFileIds(data1?.value);

  const { isLoading, error, data } = queryTemplate({
    queryKey: [`tlDocuments-${1}`, filesArray],
    queryFn: () => Digit.UploadServices.Filefetch(filesArray, tenant),
    enabled: filesArray.length > 0,
  });
  return {
    isLoading,
    error,
    data: { pdfFiles: data?.data },
    revalidate: () => client.invalidateQueries({ queryKey: [`tlDocuments-${1}`, filesArray] }),
  };
};

export default useTLDocumentSearch;
