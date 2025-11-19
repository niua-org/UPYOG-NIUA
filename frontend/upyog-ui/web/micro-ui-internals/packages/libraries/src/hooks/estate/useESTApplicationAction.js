import { useMutation } from "react-query";

const useESTApplicationAction = () => {
  return useMutation((data) => Digit.ESTService.applicationAction(data));
};

export default useESTApplicationAction;
