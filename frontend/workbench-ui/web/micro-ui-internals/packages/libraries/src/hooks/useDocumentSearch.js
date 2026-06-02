import { queryTemplate } from "../common/queryTemplate";
import { useQueryClient } from "../common/queryClientTemplate";
import PropTypes from "prop-types";

const useDocumentSearch = (documents = [], config = {}) => {
  const client = useQueryClient();
  const tenant = Digit.ULBService.getStateId();
  const filesArray = documents?.map((value) => value?.fileStoreId);

  const { isLoading, error, data } = queryTemplate({           
    queryKey: [filesArray.join('')],
    queryFn: () => Digit.UploadServices.Filefetch(filesArray, tenant),
    config: {
      enabled: filesArray && filesArray.length > 0,
      select: (data) => {
        return documents.map((document) => {
          return {
            ...document,
            fileURL: data?.data?.[document?.fileStoreId] && Digit.Utils.getFileUrl(data.data[document?.fileStoreId]),
            url: data?.data?.[document?.fileStoreId] && Digit.Utils.getFileUrl(data.data[document?.fileStoreId]),
            fileResponse: data?.data?.[document?.fileStoreId] || "",
          };
        });
      },
      ...config,
    },
  });

  return {
    isLoading,
    error,
    data: { pdfFiles: data },
    revalidate: () => client.invalidateQueries({               
      queryKey: [filesArray.join('')]
    }),
  };
};

export default useDocumentSearch;

useDocumentSearch.propTypes = {
  documents: PropTypes.array,
  config: PropTypes.object,
};

useDocumentSearch.defaultProps = {
  documents: [],
  config: {},
};