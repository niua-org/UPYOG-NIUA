import { GCServices } from "../../elements/GC";

const ApplicationUpdateActionGC = async (applicationData, tenantId) => {
  try {
    const payload = applicationData?.garbageApplications
      ? applicationData
      : { garbageApplications: [applicationData] };
    return await GCServices.update(payload, tenantId);
  } catch (error) {
    throw new Error(error?.response?.data?.Errors?.[0]?.message || error?.message);
  }
};

export default ApplicationUpdateActionGC;
