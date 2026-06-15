import { queryTemplate } from "../../common/queryTemplate";
import { InboxGeneral } from "../../services/elements/InboxService";
import { Search } from "../../services/molecules/OBPS/Search";

const useArchitectInbox = ({ tenantId, filters, withEDCRData = true, isTotalCount = false, config = {} }) => {
  const queryKey = [
    "OBPS_ARCHITECT_INBOX",
    tenantId,
    JSON.stringify(filters),
    withEDCRData,
    isTotalCount,
  ];

  const queryFn = async () => {
    const data = await InboxGeneral.Search({ inbox: { ...filters } });

    if (withEDCRData && data?.items) {
      try {
        const edcrData = await Promise.all(
          data.items.map((app) =>
            Search.scrutinyDetails("pb.amritsar", {
              edcrNumber: app?.businessObject?.edcrNumber,
            })
          )
        );

        data.items = data.items.map((app) => ({
          ...app,
          edcr:
            edcrData.find(
              (e) => e?.edcrNumber === app?.businessObject?.edcrNumber
            ) || {},
        }));
      } catch {}
    }

    return data;
  };

  const select = (data) => ({
    statuses: data.statusMap,
    table: data.items.map((app) => ({
      applicationId: app.businessObject.applicationNo,
      date: app.businessObject.applicationDate,
      status: app.ProcessInstance?.state?.state,
      owner: app.ProcessInstance?.assigner?.name,
      edcr: app.edcr,
    })),
    totalCount: data.totalCount,
  });

  return queryTemplate({
    queryKey,
    queryFn,
    select,
    config,
  });
};

export default useArchitectInbox;