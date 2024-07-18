  import React, { useCallback, useMemo, useEffect } from "react"
  import { useForm, Controller } from "react-hook-form";
  import { TextInput, SubmitBar, DatePicker, SearchForm, Dropdown, SearchField, Table, Card, Loader, Header } from "@nudmcdgnpm/digit-ui-react-components";
  import { Link } from "react-router-dom";
  

  const ASSETSearchApplication = ({isLoading, t, onSubmit, data, count, setShowToast }) => {
      const isMobile = window.Digit.Utils.browser.isMobile();
      const todaydate = new Date();
      const today = todaydate.toISOString().split("T")[0];
      const { register, control, handleSubmit, setValue, getValues, reset, formState } = useForm({
          defaultValues: {
              offset: 0,
              limit: !isMobile && 10,
              sortBy: "commencementDate",
              sortOrder: "DESC",
              fromDate: today,
              toDate: today,
          }
      })
      useEffect(() => {
        register("offset", 0)
        register("limit", 10)
        register("sortBy", "commencementDate")
        register("sortOrder", "DESC")
        setValue("fromDate", today);
        setValue("toDate", today);
      },[register, setValue, today])


      const { data: actionState } = Digit.Hooks.useCustomMDMS(Digit.ULBService.getStateId(), "ASSET", [{ name: "Action" }],
      {
        select: (data) => {
            const formattedData = data?.["ASSET"]?.["Action"]
            return formattedData;
        },
    }); 
    let action = [];

    actionState && actionState.map((actionstate) => {
      action.push({i18nKey: `${actionstate.name}`, code: `${actionstate.code}`, value: `${actionstate.name}`})
    }) 


      const GetCell = (value) => <span className="cell-text">{value}</span>;
      const columns = useMemo( () => ([
          {
              Header: t("ES_ASSET_RESPONSE_CREATE_LABEL"),
              accessor: "applicationNo",
              disableSortBy: true,
              Cell: ({ row }) => {
                return (
                  <div>
                    <span className="link">
                      <Link to={`/digit-ui/employee/asset/assetservice/applicationsearch/application-details/${row.original?.["applicationNo"]}`}>
                        {row.original?.["applicationNo"]}
                      </Link>
                    </span>
                  </div>
                );
              },
            },
          

            {
              Header: t("AST_ASSET_CATEGORY_LABEL"),
              Cell: ( row ) => {
                return GetCell(`${row?.row?.original?.["assetClassification"]}`)
              },
              disableSortBy: true,
            },
            {
              Header: t("AST_PARENT_CATEGORY_LABEL"),
              Cell: ({ row }) => {
                return GetCell(`${row?.original?.["assetParentCategory"]}`)
              },
              disableSortBy: true,
            
            },
            {
              Header: t("AST_NAME_LABEL"),
              Cell: ({ row }) => {
                return GetCell(`${row?.original?.["assetName"]}`)
              },
              disableSortBy: true,
            },
            {
              Header: t("AST_DEPARTMENT_LABEL"),
              Cell: ({ row }) => {
                return GetCell(`${row?.original?.["department"]}`)
              },
              disableSortBy: true,
            },
        ]), [] )

      const onSort = useCallback((args) => {
          if (args.length === 0) return
          setValue("sortBy", args.id)
          setValue("sortOrder", args.desc ? "DESC" : "ASC")
      }, [setValue])

      function onPageSizeChange(e){
          setValue("limit",Number(e.target.value))
          handleSubmit(onSubmit)()
      }

      function nextPage () {
          setValue("offset", getValues("offset") + getValues("limit"))
          handleSubmit(onSubmit)()
      }
      function previousPage () {
          setValue("offset", getValues("offset") - getValues("limit") )
          handleSubmit(onSubmit)()
      }
  

      return <React.Fragment>
                  
                  <div>
                  <Header>{t("ASSET_APPLICATIONS")}</Header>
                  < Card className={"card-search-heading"}>
                      <span style={{color:"#505A5F"}}>{t("Provide at least one parameter to search for an application")}</span>
                  </Card>
                  <SearchForm onSubmit={onSubmit} handleSubmit={handleSubmit}>
                  <SearchField>
                      <label>{t("AST_STATUS")}</label>
                      <Controller
                              control={control}
                              name="status"
                              render={(props) => (
                                  <Dropdown
                                  selected={props.value}
                                  select={props.onChange}
                                  onBlur={props.onBlur}
                                  option={action}
                                  optionKey="i18nKey"
                                  t={t}
                                  disable={false}
                                  />
                              )}
                              />
                  </SearchField>
                  <SearchField>
                      <label>{t("AST_APPLICATION_ID")}</label>
                      <TextInput name="applicationNo" inputRef={register({})} />
                  </SearchField>
                 
                  <SearchField>
                      <label>{t("AST_FROM_DATE")}</label>
                      <Controller
                          render={(props) => <DatePicker date={props.value} disabled={false} onChange={props.onChange} />}
                          name="fromDate"
                          control={control}
                          />
                  </SearchField>
                  <SearchField>
                      <label>{t("AST_TO_DATE")}</label>
                      <Controller
                          render={(props) => <DatePicker date={props.value} disabled={false} onChange={props.onChange} />}
                          name="toDate"
                          control={control}
                          />
                  </SearchField>
                  <SearchField className="submit">
                      <SubmitBar label={t("ES_COMMON_SEARCH")} submit />
                      <p style={{marginTop:"10px"}}
                      onClick={() => {
                          reset({ 
                              applicationNo: "", 
                              fromDate: today, 
                              toDate: today,
                              status: "",
                              offset: 0,
                              limit: 10,
                              sortBy: "commencementDate",
                              sortOrder: "DESC"
                          });
                          setShowToast(null);
                          previousPage();
                      }}>{t(`ES_COMMON_CLEAR_ALL`)}</p>
                  </SearchField>
              </SearchForm>
              {!isLoading && data?.display ? <Card style={{ marginTop: 20 }}>
                  {
                  t(data.display)
                      .split("\\n")
                      .map((text, index) => (
                      <p key={index} style={{ textAlign: "center" }}>
                          {text}
                      </p>
                      ))
                  }
              </Card>
              :(!isLoading && data !== ""? <Table
                  t={t}
                  data={data}
                  totalRecords={count}
                  columns={columns}
                  getCellProps={(cellInfo) => {
                  return {
                      style: {
                      minWidth: cellInfo.column.Header === t("ASSET_INBOX_APPLICATION_NO") ? "240px" : "",
                      padding: "20px 18px",
                      fontSize: "16px"
                    },
                  };
                  }}
                  onPageSizeChange={onPageSizeChange}
                  currentPage={getValues("offset")/getValues("limit")}
                  onNextPage={nextPage}
                  onPrevPage={previousPage}
                  pageSizeLimit={getValues("limit")}
                  onSort={onSort}
                  disableSort={false}
                  sortParams={[{id: getValues("sortBy"), desc: getValues("sortOrder") === "DESC" ? true : false}]}
              />: data !== "" || isLoading && <Loader/>)}
              </div>
          </React.Fragment>
  }

  export default ASSETSearchApplication