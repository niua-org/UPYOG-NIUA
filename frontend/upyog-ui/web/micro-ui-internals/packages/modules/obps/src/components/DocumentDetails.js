import React, { Fragment, useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { PDFSvg, Row } from "@nudmcdgnpm/digit-ui-react-components";

const DocumentDetails = ({ documents }) => {
  const { t } = useTranslation();
  const [filesArray, setFilesArray] = useState(() => []);
  const [pdfFiles, setPdfFiles] = useState({});

  if (documents?.length == 0) {
    return <div style={{padding: "10px 0px"}}><p>{t("BPA_NO_DOCUMENTS_UPLOADED_LABEL")}</p></div>
  }
  useEffect(() => {
    let acc = [];
    documents?.forEach((element, index, array) => {
      acc = [...acc, element];
    });
    setFilesArray(acc?.map((value) => value?.filestoreIdArray.map((val) => val)));
  }, [documents]);

  useEffect(() => {
    if (filesArray?.length) {
      Digit.UploadServices.Filefetch(filesArray, Digit.ULBService.getStateId()).then((res) => {
        setPdfFiles(res?.data);
      });
    }
  }, [filesArray]);

  return (
    <Fragment>
      {documents?.map((document, docIndex) => (
        <Fragment>
          <Row className="border-none" labelStyle={{ paddingTop: "10px", width: "100%" }} label={t(document?.title?.split('_')?.slice(0, 2).join('_'))} />
          <div className="flex" style={{ flexWrap: "wrap" }}>
            {document?.filestoreIdArray && document?.filestoreIdArray.map((filestoreId, index) =>
              <div className="flex justify-start" style={{ flexWrap: "wrap", alignContent: "center" }}>
                <a target="_blank" href={pdfFiles[filestoreId]?.split(",")[0]} style={{ minWidth: "100px", marginRight: "10px", maxWidth: "100px", height: "auto" }} key={index}>
                  <div className="flex justify-center">
                    <PDFSvg />
                  </div>
                  <p className="mt-sm text-center text-text-secondary font-regular text-md" style={{ lineHeight: "19px" }}>{t(document?.title)}</p>
                </a>
              </div>
            )}
          </div>
          {documents?.length != docIndex + 1 ? <hr className="text-border bg-border" style={{ height: "2px", marginTop: "20px", marginBottom: "20px" }} /> : null}
        </Fragment>
      ))}
    </Fragment>
  );
}

export default DocumentDetails;