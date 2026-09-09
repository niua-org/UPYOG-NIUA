import React from "react";
import { useTranslation } from "react-i18next";
import { CardSubHeader, PDFSvg } from "@nudmcdgnpm/digit-ui-react-components";

function DocumentsPreview({ documents, svgStyles = {}, isSendBackFlow = false, isHrLine = false, titleStyles }) {
    const { t } = useTranslation();
    const isStakeholderApplication = window.location.href.includes("stakeholder");

    return (
        <div style={{ marginTop: "19px" }}>
            {!isStakeholderApplication && documents?.map((document, index) => (
                <React.Fragment key={index}>
                    {document?.title ? <CardSubHeader style={titleStyles ? titleStyles : { marginTop: "32px", marginBottom: "8px", color: "var(--text-secondary)", fontSize: "24px" }}>{t(document?.title)}</CardSubHeader> : null}
                    <div className="flex justify-start" style={{ flexWrap: "wrap" }}>
                        {document?.values && document?.values.length > 0 ? document?.values?.map((value, index) => (
                            <a target="_" href={value?.url} style={{ marginRight: "10px", maxWidth: "100px", height: "auto", minWidth: "100px" }} key={index}>
                                <div className="flex justify-center">
                                    <PDFSvg />
                                </div>
                                <p className="text-text-secondary mt-sm font-bold text-center">{t(value?.title)}</p>
                                {isSendBackFlow ? value?.documentType?.includes("NOC") ? <p className="text-center">{t(value?.documentType.split(".")[1])}</p> : <p className="text-center">{t(value?.documentType)}</p> : ""}
                            </a>
                        )) : !(window.location.href.includes("citizen")) && <div><p>{t("BPA_NO_DOCUMENTS_UPLOADED_LABEL")}</p></div>}
                    </div>
                    {isHrLine && documents?.length != index + 1 ? <hr className="text-border bg-border" style={{ height: "2px", marginTop: "20px", marginBottom: "20px" }} /> : null}
                </React.Fragment>
            ))}
            {isStakeholderApplication && documents?.map((document, index) => (
                <React.Fragment key={index}>
                    {document?.title ? <CardSubHeader className="mt-xl mb-sm text-text-secondary text-2xl">{t(document?.title)}</CardSubHeader> : null}
                    <div>
                        {document?.values && document?.values.length > 0 ? document?.values?.map((value, index) => (
                            <a target="_" href={value?.url} style={{ minWidth: svgStyles?.minWidth ? svgStyles?.minWidth : "160px", marginRight: "20px" }} key={index}>
                                <div className="p-sm rounded-sm border border-solid border-border bg-grey-light" style={{ maxWidth: "940px" }}>
                                    <p className="mt-sm font-bold" style={{ marginBottom: "10px" }}>{t(value?.title)}</p>
                                    {value?.docInfo ? <div className="text-xs text-text-secondary font-regular" style={{ lineHeight: "15px", marginBottom: "10px" }}>{`${t(value?.docInfo)}`}</div> : null}
                                    <PDFSvg />
                                    <p className="text-text-secondary mt-sm text-md font-regular text-center" style={{ lineHeight: "19px" }}>{`${t(value?.title)}`}</p>
                                </div>
                            </a>
                        )) : !(window.location.href.includes("citizen")) && <div><p>{t("BPA_NO_DOCUMENTS_UPLOADED_LABEL")}</p></div>}
                    </div>
                </React.Fragment>
            ))}
        </div>
    );
}

export default DocumentsPreview;
