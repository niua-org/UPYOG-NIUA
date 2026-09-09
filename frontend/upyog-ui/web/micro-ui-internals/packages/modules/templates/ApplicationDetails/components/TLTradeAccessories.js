
import React from "react";
import { useTranslation } from "react-i18next";
import { CardSubHeader, StatusTable, Row, CardSectionHeader } from "@nudmcdgnpm/digit-ui-react-components";

function TLTradeAccessories({ units }) {
  const { t } = useTranslation();
  return (
    <React.Fragment>
      {units.map((unit, index) => (
        // TODO, Later will move to classes
        <div key={t(unit?.title)} className="bg-grey-light border border-solid border-border rounded-sm p-sm" style={{ marginTop: "19px", lineHeight: "19px", maxWidth: "600px", minWidth: "280px" }}>
          <CardSubHeader className="mb-sm text-text-primary text-md" style={{ paddingBottom: "9px", lineHeight: "19px" }}>{`${t(unit?.title)} ${index + 1}`}</CardSubHeader>
          <React.Fragment key={index}>
            <StatusTable style={{ position: "relative", marginTop: "19px" }}>
              <div
                style={{
                  position: "absolute",
                  maxWidth: "640px",
                  top: 0,
                  left: 0,
                  bottom: 0,
                  right: 0,
                  width: "auto",
                }}
              ></div>
              {unit?.values?.map((value, index) => {
                if (value.map === true && value.value !== "N/A") {
                  return <Row key={t(value.title)} label={t(value.title)} text={<img src={t(value.value)} alt="" />} />;
                }
                return (
                  <Row
                    key={t(value.title)}
                    label={`${t(value.title)}:`}
                    text={t(value.value) || "N/A"}
                    last={index === value?.values?.length - 1}
                    caption={value.caption}
                    className="border-none"
                    // TODO, Later will move to classes
                    rowContainerStyle={{justifyContent: "space-between", fontSize: "16px", lineHeight: "19px", color: "var(--text-primary)"}}
                  />
                );
              })}
            </StatusTable>
          </React.Fragment>
        </div>
      ))}
    </React.Fragment>
  );
}

export default TLTradeAccessories;
