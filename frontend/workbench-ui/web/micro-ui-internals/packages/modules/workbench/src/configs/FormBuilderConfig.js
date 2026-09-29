export const Config = {
  label: "WBH_FORM_BUILDER_HEADER",
  type: "search",
  actionLabel: ["WBH_ADD_NEW_FIELD","WBH_ADD_NEW_FORM"],
  actionRole: "LOC_ADMIN",
  actionLink: "workbench/form-builder-add",
  apiDetails: {
    serviceName: "/localization/messages/v1/_search",
    requestParam: {},
    requestBody: {},
    minParametersForSearchForm: 0,
    masterName: "commonUiConfig",
    moduleName: "SearchFormBuilderConfig",
    tableFormJsonPath: "requestBody.custom",
    filterFormJsonPath: "requestBody.custom",
    searchFormJsonPath: "requestParam"
  },
  sections: {
    search: {
      uiConfig: {
        searchWrapperStyles: {
          flexDirection: "column-reverse",
          marginTop: "2rem",
          alignItems: "center",
          justifyContent: "end",
          gridColumn: "4"
        },
        headerStyle: null,
        formClassName: "",
        primaryLabel: "ES_COMMON_SEARCH",
        secondaryLabel: "ES_COMMON_CLEAR_SEARCH",
        minReqFields: 0,
        defaultValues: {},
        fields: []
      },
      label: "",
      children: {},
      show: false
    },
    searchResult: {
      label: "",
      uiConfig: {
        tableClassName: "table-fixed-last-column table",
        columns: [
          {
            label: "WBH_MODULE_NAME",
            jsonPath: "moduleName"
          },
          {
            label: "WBH_FORM_NAME",
            jsonPath: "formName"
          },
          {
            label: "WBH_ACCORDION",
            jsonPath: "accordionName"
          },
          {
            label: "CS_COMMON_ACTION",
            additionalCustomization: true
          }
        ],
        enableGlobalSearch: false,
        enableColumnSort: true,
        resultsJsonPath: "items",
        manualPagination: false
      },
      children: {},
      show: true
    }
  },
  additionalSections: {},
  customHookName: "workbench.useFormBuilderSearch"
};
