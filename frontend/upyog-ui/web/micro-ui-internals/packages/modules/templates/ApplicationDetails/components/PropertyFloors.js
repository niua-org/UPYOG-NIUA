import React from "react";
import { useTranslation } from "react-i18next";
import { CardSubHeader, StatusTable, Row, CardSectionHeader } from "@nudmcdgnpm/digit-ui-react-components";

function PropertyFloors({ floors }) {
  const { t } = useTranslation();

  return (
    <React.Fragment>
      {floors.map((floor) => (
        <div key={t(floor?.title)} style={{ marginTop: "19px" }}>
          <CardSubHeader className="mb-sm text-text-secondary text-2xl">{t(floor?.title)}</CardSubHeader>
          {floor?.values?.map((value, index) => {
            return (
              <React.Fragment key={index}>
                <CardSectionHeader className="mb-md text-text-secondary text-md" style={{ marginTop: index !== 0 ? "16px" : "revert" }}>
                  {t(value.title)}
                </CardSectionHeader>
                <StatusTable className="p-sm" style={{ position: "relative" }}>
                  <div
                    className="border border-solid border-border p-md mt-sm rounded-sm bg-grey-light" style={{ maxWidth: "100%" }}
                  >
                  {value?.values?.map((value, index) => {
                    if (value.map === true && value.value !== "N/A") {
                      return <Row key={t(value.title)} label={t(value.title)} text={<img src={t(value.value)} alt="" />} />;
                    }
                    return (
                      <Row
                        key={t(value.title)}
                        label={t(value.title)}
                        text={t(value.value) || "N/A"}
                        last={index === value?.values?.length - 1}
                        caption={value.caption}
                        className="border-none"
                      />
                    );
                  })}
                  </div>
                </StatusTable>
              </React.Fragment>
            );
          })}
        </div>
      ))}
    </React.Fragment>
  );
}

export default PropertyFloors;
