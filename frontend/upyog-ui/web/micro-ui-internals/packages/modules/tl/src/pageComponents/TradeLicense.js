/**
 * Citizen documents landing for a new trade license. Lists the proofs the
 * applicant must keep ready before the application form.
 *
 * Document rows come from config/RequiredDocuments.json. Headings stay on
 * navigation.infoPage. When this list moves to MDMS TradeLicense.RequiredDocuments,
 * read that master here instead of the local file.
 */
import {
  Card,
  CardHeader,
  CardText,
  CitizenInfoLabel,
  SubmitBar,
} from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import localRequiredDocuments from "../config/RequiredDocuments.json";

const activeDocuments = (list) =>
  (Array.isArray(list) ? list : []).filter(
    (doc) => doc && doc.active !== false && doc.active !== "false"
  );

/** Use a localization hit when it exists; otherwise the text stored in JSON. */
const textFrom = (t, key, fallback) => {
  if (key && typeof t === "function") {
    const translated = t(key);
    if (translated && translated !== key) return translated;
  }
  return fallback || "";
};

/** /info — required-documents landing. Rows from config/RequiredDocuments.json. */
const TradeLicense = ({ t, config, onSelect }) => {
  const documents = activeDocuments(localRequiredDocuments?.RequiredDocuments);

  const goNext = () => onSelect(config?.key || "info", {});

  const sectionHeading = textFrom(
    t,
    config?.sectionHeading || config?.header,
    config?.sectionHeadingDefault || "Documents required"
  );
  const intro = textFrom(
    t,
    config?.text || config?.cardText,
    config?.textDefault || "Keep the following documents ready"
  );
  const nextLabel = textFrom(
    t,
    config?.nextLabel,
    config?.nextLabelDefault || "Next"
  );
  const infoLabel = textFrom(
    t,
    config?.infoLabel,
    config?.infoLabelDefault || "Info"
  );
  const infoText = textFrom(
    t,
    config?.infoText,
    config?.infoTextDefault ||
      "Accepted formats: jpg, jpeg, png, pdf. Maximum size 2 MB."
  );

  return (
    <React.Fragment>
      <Card>
        <CardHeader>{sectionHeading}</CardHeader>

        <div>
          <CardText>{intro}</CardText>

          <div>
            {documents.map((doc, index) => (
              <CardText key={doc.code || doc.i18nKey || index} className="primaryColor">
                {doc.order ?? index + 1}.{" "}
                {textFrom(t, doc.i18nKey, doc.name || doc.label || "")}
              </CardText>
            ))}
          </div>
        </div>

        <span>
          <SubmitBar label={nextLabel} onSubmit={goNext} />
        </span>
      </Card>
      <CitizenInfoLabel info={infoLabel} text={infoText} />
    </React.Fragment>
  );
};

export default TradeLicense;
