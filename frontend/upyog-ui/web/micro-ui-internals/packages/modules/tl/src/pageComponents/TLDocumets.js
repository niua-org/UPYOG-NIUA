import { Loader } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { pdfDownloadLink } from "../utils";

const PDFSvg = ({
  width = 20,
  height = 20,
  style
}) => <svg style={style} xmlns="http://www.w3.org/2000/svg" width={width} height={height} viewBox="0 0 20 20" fill="gray">
    <path d="M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8.5 7.5c0 .83-.67 1.5-1.5 1.5H9v2H7.5V7H10c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-2.5V7H15c.83 0 1.5.67 1.5 1.5v3zm4-3H19v1h1.5V11H19v2h-1.5V7h3v1.5zM9 9.5h1v-1H9v1zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm10 5.5h1v-3h-1v3z" />
  </svg>;

const fileId = (doc) => doc?.fileStoreId || doc?.filestoreId || doc?.documentuuid || "";

const collectDocuments = (value = {}) => {
  const documents = [];
  const seen = new Set();
  const push = (doc, fallbackType) => {
    if (!doc) return;
    const id = typeof doc === "string" ? doc : fileId(doc);
    if (!id || seen.has(id)) return;
    seen.add(id);
    documents.push(
      typeof doc === "string"
        ? { fileStoreId: doc, documentType: fallbackType || "" }
        : { ...doc, fileStoreId: id, documentType: doc.documentType || fallbackType || "" }
    );
  };

  const applicationDocs = value?.tradeLicenseDetail?.applicationDocuments;
  if (Array.isArray(applicationDocs)) applicationDocs.forEach((doc) => push(doc));
  if (Array.isArray(value?.workflowDocs)) value.workflowDocs.forEach((doc) => push(doc));

  const sessionDocs = value?.owners?.documents;
  if (sessionDocs && typeof sessionDocs === "object" && !Array.isArray(sessionDocs)) {
    push(sessionDocs.ProofOfIdentity, "OWNERIDPROOF");
    push(sessionDocs.ProofOfOwnership, "OWNERSHIPPROOF");
    push(sessionDocs.OwnerPhotoProof, "OWNERPHOTO");
  }

  const owners = value?.tradeLicenseDetail?.owners;
  if (Array.isArray(owners)) {
    owners.forEach((owner) => {
      if (Array.isArray(owner?.documents)) owner.documents.forEach((doc) => push(doc));
    });
  }

  return documents;
};

const documentLabel = (document, t, isWorkflow) => {
  const type = document?.documentType || "";
  const keys = isWorkflow
    ? [type, `TL_NEW_${type}`, `TL_${type}_LABEL`]
    : [`TL_${type}_LABEL`, `TL_NEW_${type}`, `TL_${type}`];
  for (const key of keys) {
    if (!key) continue;
    const translated = t(key);
    if (translated && translated !== key) return translated;
  }
  return document?.fileName || document?.name || type || t("TL_COMMON_DOCS");
};

function TLDocument({
  value = {}
}) {
  const {
    t
  } = useTranslation();
  const {
    isLoading,
    data
  } = Digit.Hooks.tl.useTLDocumentSearch({
    value
  }, {
    value
  });
  const documents = collectDocuments(value);
  if (isLoading) {
    return <Loader />;
  }
  return <div className="tl-auto-91">
      <React.Fragment>
        <div className="tl-auto-92">
          {documents.map((document, index) => {
          const documentLink = pdfDownloadLink(data?.pdfFiles, document?.fileStoreId);
          const label = documentLabel(document, t, Boolean(value?.workflowDocs));
          return <a target="_blank" rel="noreferrer" href={documentLink || undefined} key={document.fileStoreId || index} className="tl-auto-93">
                <PDFSvg width={85} height={100} className="tl-auto-94" />
                <p className="tl-auto-95">{label}</p>
              </a>;
        })}
        </div>
      </React.Fragment>
    </div>;
}
export default TLDocument;
