import SelectName from "./components/SelectName";

const pgrCustomizations = {
  // complaintConfig,
  getComplaintDetailsTableRows: ({ id, service, role, t }) => {
    return {};
  },
};

const pgrComponents = {
  SelectName: SelectName,
};
export { pgrCustomizations, pgrComponents };
