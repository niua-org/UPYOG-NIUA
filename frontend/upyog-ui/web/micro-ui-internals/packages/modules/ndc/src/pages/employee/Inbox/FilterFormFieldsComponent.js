import React, { useState, Fragment, useEffect } from "react";
import { FilterFormField, Loader, RadioButtons, Localities, RemoveableTag, Dropdown, CheckBox } from "@upyog/digit-ui-react-components";
import { Controller } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { businessServiceList } from "../../../utils";

const FilterFormFieldsComponent = ({ statuses, controlFilterForm, applicationTypesOfBPA, handleFilter }) => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getStateId();

  const [tlfilters, setTLFilters] = useState({
    applicationStatus: [],
  });

  useEffect(() => {
    if (tlfilters) {
      handleFilter(tlfilters);
    }
  }, [tlfilters]);

  const availableOptions = [
    { code: "ASSIGNED_TO_ME", name: `${t("ES_INBOX_ASSIGNED_TO_ME")}` },
    { code: "ASSIGNED_TO_ALL", name: `${t("ES_INBOX_ASSIGNED_TO_ALL")}` },
  ];

  applicationTypesOfBPA?.forEach((type) => {
    type.name = t(`WF_BPA_${type.code}`);
    type.i18nKey = t(`WF_BPA_${type.code}`);
  });

  const handleAssignmentChange = (e, type) => {
    if (e.target.checked) {
      setTLFilters({ ...tlfilters, applicationStatus: [...tlfilters.applicationStatus, { code: type.applicationstatus }] });
    } else {
      const filteredStatus = tlfilters.applicationStatus.filter((value) => {
        return value.code !== type.applicationstatus;
      });
      setTLFilters({ ...tlfilters, applicationStatus: filteredStatus });
    }
  };

  return (
    <>
      <FilterFormField>
        <Controller
          name="assignee"
          control={controlFilterForm}
          render={(props) => (
            <RadioButtons
              onSelect={(e) => {
                props.onChange(e.code);
              }}
              selectedOption={availableOptions.filter((option) => option.code === props.value)[0]}
              optionsKey="name"
              options={availableOptions}
            />
          )}
        />
      </FilterFormField>

      <FilterFormField>
        <Controller
          name="applicationStatus"
          control={controlFilterForm}
          defaultValue={[]}
          render={(props) => {
            const toggleStatus = (statusCode) => {
              console.log(statusCode, "statusCode");
              if (props.value.includes(statusCode)) {
                props.onChange(props.value.filter((code) => code !== statusCode));
              } else {
                props.onChange([...props.value, statusCode]);
              }
            };

            return (
              <>
                {statuses?.map((status, index) => (
                  <CheckBox
                    key={status.applicationstatus}
                    label={`${t(status.applicationstatus)} - ${status.count}`}
                    value={status.applicationstatus}
                    checked={props.value.includes(status.applicationstatus)}
                    onChange={() => toggleStatus(status.applicationstatus)}
                    index={index}
                  />
                ))}
              </>
            );
          }}
        />
      </FilterFormField>
    </>
  );
};

export default FilterFormFieldsComponent;
