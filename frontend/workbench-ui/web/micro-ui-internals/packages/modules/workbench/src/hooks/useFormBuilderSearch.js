export const FORM_BUILDER_DATA = [
  {
    id: "1",
    moduleName: "Trade Licence",
    formName: "TL_APPLY",
    accordionName: "Applicant Details (Owner, Contact)"
  },
  {
    id: "2",
    moduleName: "Trade Licence",
    formName: "TL_RENEW",
    accordionName: "Business Information & Premises"
  },
  {
    id: "3",
    moduleName: "Trade Licence",
    formName: "TL_AMENDMENT",
    accordionName: "Documentation & KYC Uploads"
  },
  {
    id: "4",
    moduleName: "Trade Licence",
    formName: "TL_CLOSURE",
    accordionName: "Declaration & Fee Assessment"
  },
  {
    id: "5",
    moduleName: "Property Tax",
    formName: "PT_ASSESSMENT",
    accordionName: "Property & Owner Verification"
  }
];

const useFormBuilderSearch = () => {
  return {
    isLoading: false,
    isFetching: false,
    data: {
      items: FORM_BUILDER_DATA,
    },
    refetch: () => {},
    revalidate: () => {},
    error: null,
  };
};

export default useFormBuilderSearch;
