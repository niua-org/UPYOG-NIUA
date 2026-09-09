import React, { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { Card, CardHeader } from "..";

const ULBHomeCard = (props) => {
  const { t } = useTranslation();
  const state = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const stateId = Digit.ULBService.getStateId();
  const navigate = Digit.Hooks.useCustomNavigate();

  return (
    <React.Fragment>
      <Card className="fsm" style={{ backgroundColor: "transparent", boxShadow: "none", paddingTop: "0" }}>
        <CardHeader> {t(props.title)} </CardHeader>
        <div className="justify-between" style={{ display: "grid", gridTemplateColumns: "30% 30% 30%", textAlign: "-webkit-center" }}>
          {props.module.map((i) => {
            return (
              <Card
                className="cursor-pointer" style={{ minWidth: "100px" }}
                onClick={() => (i.hyperlink ? location.assign(i.link) : navigate(i.link))}
                children={
                  <>
                    {" "}
                    {i.icon} <p> {t(i.name)} </p>{" "}
                  </>
                }
              ></Card>
            );
          })}
        </div>
      </Card>
    </React.Fragment>
  );
};

export default ULBHomeCard;
