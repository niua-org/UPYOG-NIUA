import { CardSectionHeader, CardText, Loader, PDFSvg, StatusTable } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";

function Document({ docs = [] }) {
  const { t } = useTranslation();

  return (
    <div style={{ marginTop: "19px" }}>
      <React.Fragment>
        <div className="flex" style={{ flexWrap: "wrap" }}>
          {docs.length > 0 ? (
            <div className="flex justify-start" style={{ flexWrap: "wrap" }}>
              {docs?.map((document, index) => {
                let documentLink = document?.fileURL;
                return (
                  <a
                    target="_blank"
                    rel="noreferrer"
                    href={documentLink}
                    style={{ minWidth: "80px", marginRight: "10px", maxWidth: "100px", height: "auto" }}
                    key={index}
                  >
                    <div className="flex justify-center">
                      <PDFSvg />
                    </div>
                    <p className="text-text-secondary mt-sm text-center">{`${t(document?.documentType)}`}</p>
                  </a>
                );
              })}
            </div>
          ) : (
            <CardText>{t("BPA_NO_DOCUMENTS_UPLOADED_LABEL")}</CardText>
          )}
        </div>
      </React.Fragment>
    </div>
  );
}

const OBPSDocumentsHolder = ({ documents = [] }) => {
  const { t } = useTranslation();
  const { data, isLoading, error } = Digit.Hooks.useDocumentSearch(
    documents.map((doc) => {
      return { ...doc, docCategory: doc?.additionalDetails?.category || doc?.documentType.split(".").slice(0, 2).join("_") };
    })
  );

  if (isLoading) {
    return <Loader />;
  }
  let consolidatedDocObject = data?.pdfFiles?.reduce((acc, curr) => {
    if (acc[curr.docCategory]) {
      acc[curr.docCategory].push(curr);
    } else {
      acc[curr.docCategory] = [curr];
    }
    return { ...acc };
  }, {})||{};
  return (
    <React.Fragment>
      {Object.keys(consolidatedDocObject)?.map((category, index) => (
        <div key={index}>
          <div>
            <CardSectionHeader>{`${t(category)}`}</CardSectionHeader>
            <StatusTable>
              <Document key={index} docs={consolidatedDocObject[category]} />
              <hr style={{ color: "#cccccc", backgroundColor: "#cccccc", height: "2px", marginTop: "20px", marginBottom: "20px" }} />
            </StatusTable>
          </div>
        </div>
      ))}
    </React.Fragment>
  );
};

export default OBPSDocumentsHolder;
