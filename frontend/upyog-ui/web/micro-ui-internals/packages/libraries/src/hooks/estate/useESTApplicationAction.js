import { useMutation } from "react-query";

// useMutation hook to perform application actions in the EST module
// This hook provides a mutation interface for executing actions on EST applications
// such as approval, rejection, or other state changes.

const useESTApplicationAction = (tenantId) => {
  return useMutation((applicationData) => Digit.ESTService.applicationAction(applicationData, tenantId));
};

export default useESTApplicationAction;
