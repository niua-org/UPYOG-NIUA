import { Header, Loader, Dropdown, Card, CardLabel, Button } from "@upyog/workbench-ui-react-components";
import React, { useState, useEffect, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { Config as Configg } from "../../configs/searchMDMSConfig";
import _ from "lodash";

function sortByKey(arr, key) {
  return arr.slice().sort((a, b) => {
    if (a[key] < b[key]) return -1;
    if (a[key] > b[key]) return 1;
    return 0;
  });
}

const MDMSManageMaster = () => {
  let Config = _.clone(Configg);
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();

  let { masterName: modulee, moduleName: master, tenantId } =
    Digit.Hooks.useQueryParams();

  tenantId = tenantId || Digit.ULBService.getCurrentTenantId();

  const [masterName,    setMasterName]    = useState(null);
  const [moduleName,    setModuleName]    = useState(null);
  const [masterOptions, setMasterOptions] = useState([]);
  const [moduleOptions, setModuleOptions] = useState([]);
  const [updatedConfig, setUpdatedConfig] = useState(null);
  const [isLoading,     setIsLoading]     = useState(false);
  const [schemaData,    setSchemaData]    = useState(null);

  const toDropdownObj = (master = "", mod = "") => ({
    name: mod || master,
    code: Digit.Utils.locale.getTransformedLocale(
      mod ? `WBH_MDMS_${master}_${mod}` : `WBH_MDMS_MASTER_${master}`
    ),
    translatedValue: t(
      Digit.Utils.locale.getTransformedLocale(
        mod ? `WBH_MDMS_${master}_${mod}` : `WBH_MDMS_MASTER_${master}`
      )
    ),
  });

  const callSchemaAPI = async (searchText) => {
    setIsLoading(true);
    try {
      const response = await Digit.CustomService.getResponse({
        url: `/${Digit.Hooks.workbench.getMDMSContextPath()}/schema/v1/_search`,
        useCache: false,
        method: "POST",
        userService: false,
        body: {
          SchemaDefCriteria: {
            tenantId,
            limit: 200,
            moduleName: searchText,
          },
        },
      });

      const schemas = response?.SchemaDefinitions || [];

      function onlyUnique(v, i, arr) { return arr.indexOf(v) === i; }

      const obj = { mastersAvailable: [], schemas };

      schemas.forEach((schema) => {
        const [mas, mod] = schema.code.split(".");
        obj[mas] = obj[mas]?.length > 0
          ? [...obj[mas], toDropdownObj(mas, mod)]
          : [toDropdownObj(mas, mod)];
        obj.mastersAvailable.push(mas);
      });

      obj.mastersAvailable = obj.mastersAvailable
        .filter(onlyUnique)
        .map((mas) => toDropdownObj(mas));
      obj.mastersAvailable = sortByKey(obj.mastersAvailable, "translatedValue");

      setSchemaData(obj);
      setMasterOptions(obj.mastersAvailable);

    } catch (err) {
      console.error("Schema API Error:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const debouncedSearch = useCallback(
    _.debounce((text) => {
      if (text && text.trim().length >= 3) {
        callSchemaAPI(text.trim());
      } else {
        setSchemaData(null);
        setMasterOptions([]);
        setModuleOptions([]);
        setMasterName(null);
        setModuleName(null);
      }
    }, 500),
    [tenantId]
  );

  const handleModuleSearch = (e) => {
    if (e.target.tagName !== "INPUT") return;
    const value = e.target.value || "";
    debouncedSearch(value);
  };

  const handleClear = () => {
  setMasterName(null);
  setModuleName(null);
  setUpdatedConfig(null);
  setModuleOptions([]);
};


  useEffect(() => {
    if (masterName?.name && schemaData?.[masterName?.name]?.length > 0) {
      setModuleOptions(sortByKey(schemaData[masterName.name], "translatedValue"));
    } else {
      setModuleOptions([]);
    }
  }, [masterName, schemaData]);

  useEffect(() => {
    if (masterName?.name && moduleName?.name) {
      navigate(
        `/${window?.contextPath}/employee/workbench/mdms-search-v2?moduleName=${masterName.name}&masterName=${moduleName.name}`
      );
    }
  }, [moduleName]);

  return (
    <React.Fragment>
      <Header className="works-header-search">{t(Config?.label)}</Header>

      <div className="jk-header-btn-wrapper">
        <Card className="manage-master-wrapper" style={{ alignItems: "end", gap: "1rem" }}>
          <div style={{ width: '100%' }} onKeyUp={handleModuleSearch}>
            <CardLabel style={{ margin: 0 }}>{t("WBH_MODULE_NAME")}</CardLabel>
            <Dropdown
              style={{ width: "100%", margin: 0, flexShrink: 0}}
              option={masterOptions}
              className={"form-field"}
              optionKey="code"
              selected={master && modulee ? toDropdownObj(master) : masterName}
              select={(e) => {
                setMasterName(e);
                setModuleName(null);
                setUpdatedConfig(null);
              }}
              t={t}
              placeholder={t("ASSET_SEARCH_ENTER_MIN_3_CHARS")}
              disable={master ? true : false}
            />
          </div>

          <div style={{ width: '100%' }}>
            <CardLabel style={{ margin: 0 }}>{t("WBH_MASTER_NAME")}</CardLabel>
            <Dropdown
              style={{ width: "100%", margin: 0}}
              option={moduleOptions}
              className={"form-field"}
              optionKey="code"
              selected={master && modulee ? toDropdownObj(master, modulee) : moduleName}
              select={(e) => {
                setModuleName(e);
                setUpdatedConfig(null);
              }}
              t={t}
              placeholder={t("WBH_MASTER_NAME")}
              disable={masterName ? false : true}
            />
          </div>
          <Button style={{ width: '100%', maxWidth: '100px' }} label={t("CLEAR")} variation="secondary" onButtonClick={handleClear} type="button"/>
        </Card>
      </div>
    </React.Fragment>
  );
};

export default MDMSManageMaster;