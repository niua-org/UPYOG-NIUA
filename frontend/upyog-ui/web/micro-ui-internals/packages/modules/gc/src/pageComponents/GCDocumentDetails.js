import React, { useEffect, useState, useMemo } from "react";
import {
  CardLabel,
  Dropdown,
  UploadFile,
  Toast,
  Loader,
  FormStep,
  LabelFieldPair,
  CardSubHeader,
  CardLabelDesc,
} from "@nudmcdgnpm/digit-ui-react-components";

/**
 * GCDocumentDetails Component
 * 
 * Step 3 in the GC Application Wizard as defined in UPYOG Garbage Service HLD Section 2.3:
 * Multi-Card Document Upload:
 * - Card 1: Owner Documents (Owner Name & Mobile)
 *   - Identity Proof (Aadhaar / PAN / Passport / Voter ID)
 *   - Address Proof (Electricity Bill / Water Bill / Property Tax Receipt)
 *   - Special Category Proof (if Freedom Fighter / BPL / Senior Citizen / Defence selected)
 *   - Death Certificate (if inheritance transfer selected)
 * - Card 2..N: Tenant Documents (one card per added tenant with Tenant Name & Mobile)
 *   - Tenant Identity Proof (Aadhaar / Voter ID / PAN / Passport)
 *   - Rent Agreement / Occupancy Proof
 */
const GCDocumentDetails = ({ t, config, onSelect, userType, formData, renewApplication }) => {
  const user = Digit.UserService.getUser().info;
  const stateId = Digit.ULBService.getStateId();

  // MDMS Documents Fetch using useEnabledMDMS
  const { data: documentsMDMS } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "Garbage",
    [{ name: "Documents" }],
    {
      select: (data) => {
        const formattedData = data?.["Garbage"]?.["Documents"];
        return formattedData;
      },
    }
  );

  // Retrieve Owner & Tenant context from previous steps
  const propertyLoc = formData?.gcpropertylocdetails || {};
  const ownerInfo = propertyLoc?.ownerInfo || {};
  const ownerName = ownerInfo?.name || user?.name || "Owner";
  const ownerMobile = ownerInfo?.mobileNumber || user?.mobileNumber || "";

  const specifications = formData?.gcspecifications || {};
  const tenants = specifications?.tenants || [];
  const specialCategoryCode = specifications?.specialCategory?.code || specifications?.specialCategory || "NONE";
  const hasSpecialCategory = specialCategoryCode && specialCategoryCode !== "NONE";
  const isInheritance = specifications?.isInheritance === true || specifications?.isInheritance === "true";

  // Pre-fill existing documents if editing/navigating back
  const savedGcdocuments = formData?.[config?.key] || formData?.gcdocuments || {};
  const [ownerDocs, setOwnerDocs] = useState(savedGcdocuments?.ownerDocuments || []);
  const [tenantDocs, setTenantDocs] = useState(savedGcdocuments?.tenantDocuments || {});
  const [error, setError] = useState(null);

  // Define Owner Document Schema using MDMS Documents
  const ownerDocRequirements = useMemo(() => {
    const identityMDMS = documentsMDMS?.find((doc) => doc.documentType === "APPLICANT.IDENTITYPROOF");
    const addressMDMS = documentsMDMS?.find((doc) => doc.documentType === "APPLICANT.ADDRESSPROOF");

    const identityDropdown = identityMDMS?.dropdownData
      ?.filter((d) => d.active !== false)
      ?.map((d) => ({
        code: d.code,
        i18nKey: d.code,
        name: t(d.code),
      })) || [
        { code: "APPLICANT.IDENTITYPROOF.AADHAAR", i18nKey: "GC_DOC_AADHAAR", name: "Aadhaar Card" },
        { code: "APPLICANT.IDENTITYPROOF.PAN", i18nKey: "GC_DOC_PAN", name: "PAN Card" },
        { code: "APPLICANT.IDENTITYPROOF.PASSPORT", i18nKey: "GC_DOC_PASSPORT", name: "Passport" },
        { code: "APPLICANT.IDENTITYPROOF.VOTERID", i18nKey: "GC_DOC_VOTERID", name: "Voter ID" },
      ];

    const addressDropdown = addressMDMS?.dropdownData
      ?.filter((d) => d.active !== false)
      ?.map((d) => ({
        code: d.code,
        i18nKey: d.code,
        name: t(d.code),
      })) || [
        { code: "APPLICANT.ADDRESSPROOF.ELECTRICITYBILL", i18nKey: "GC_DOC_ELECTRICITY", name: "Electricity Bill" },
        { code: "APPLICANT.ADDRESSPROOF.WATERBILL", i18nKey: "GC_DOC_WATER", name: "Water Bill" },
        { code: "APPLICANT.ADDRESSPROOF.RENTAGREEMENT", i18nKey: "GC_DOC_RENT_AGREEMENT", name: "Rent Agreement" },
      ];

    const list = [
      {
        code: "OWNER.IDENTITYPROOF",
        label: t("GC_OWNER_IDENTITY_PROOF") || "Owner Identity Proof",
        required: true,
        dropdownData: identityDropdown,
      },
      {
        code: "OWNER.ADDRESSPROOF",
        label: t("GC_OWNER_ADDRESS_PROOF") || "Owner Address Proof / Electricity Bill",
        required: true,
        dropdownData: addressDropdown,
      },
    ];

    if (hasSpecialCategory) {
      list.push({
        code: "OWNER.SPECIALCATEGORYPROOF",
        label: `${t("GC_SPECIAL_CATEGORY_PROOF") || "Special Category Proof"} (${specialCategoryCode})`,
        required: true,
        dropdownData: [
          { code: "OWNER.SPECIALCATEGORYPROOF.CERTIFICATE", i18nKey: "GC_DOC_CERTIFICATE", name: "Category Certificate / ID Card" },
          { code: "OWNER.SPECIALCATEGORYPROOF.BPL_CARD", i18nKey: "GC_DOC_BPL_CARD", name: "BPL / Ration Card" },
        ],
      });
    }

    if (isInheritance) {
      list.push({
        code: "OWNER.DEATHCERTIFICATE",
        label: t("GC_DEATH_CERTIFICATE") || "Death Certificate (Inheritance Transfer)",
        required: true,
        dropdownData: [
          { code: "OWNER.DEATHCERTIFICATE.MUNICIPAL", i18nKey: "GC_DOC_DEATH_CERT", name: "Municipal Death Certificate" },
        ],
      });
    }

    return list;
  }, [documentsMDMS, hasSpecialCategory, specialCategoryCode, isInheritance, t]);

  // Define Tenant Document Schema using MDMS Documents
  const tenantDocRequirements = useMemo(() => {
    const identityMDMS = documentsMDMS?.find((doc) => doc.documentType === "APPLICANT.IDENTITYPROOF");
    const addressMDMS = documentsMDMS?.find((doc) => doc.documentType === "APPLICANT.ADDRESSPROOF");

    const identityDropdown = identityMDMS?.dropdownData
      ?.filter((d) => d.active !== false)
      ?.map((d) => ({
        code: d.code,
        i18nKey: d.code,
        name: t(d.code),
      })) || [
        { code: "APPLICANT.IDENTITYPROOF.AADHAAR", i18nKey: "GC_DOC_AADHAAR", name: "Aadhaar Card" },
        { code: "APPLICANT.IDENTITYPROOF.PAN", i18nKey: "GC_DOC_PAN", name: "PAN Card" },
        { code: "APPLICANT.IDENTITYPROOF.PASSPORT", i18nKey: "GC_DOC_PASSPORT", name: "Passport" },
        { code: "APPLICANT.IDENTITYPROOF.VOTERID", i18nKey: "GC_DOC_VOTERID", name: "Voter ID" },
      ];

    const rentDropdown = addressMDMS?.dropdownData
      ?.filter((d) => d.active !== false)
      ?.map((d) => ({
        code: d.code,
        i18nKey: d.code,
        name: t(d.code),
      })) || [
        { code: "TENANT.RENTPROOF.RENTAGREEMENT", i18nKey: "GC_DOC_RENT_AGREEMENT", name: "Registered Rent Agreement" },
        { code: "TENANT.RENTPROOF.LEASEDEED", i18nKey: "GC_DOC_LEASE_DEED", name: "Lease Deed / Occupancy Letter" },
      ];

    return [
      {
        code: "TENANT.IDENTITYPROOF",
        label: t("GC_TENANT_IDENTITY_PROOF") || "Tenant Identity Proof",
        required: true,
        dropdownData: identityDropdown,
      },
      {
        code: "TENANT.RENTPROOF",
        label: t("GC_TENANT_RENT_PROOF") || "Rent Agreement / Occupancy Proof",
        required: true,
        dropdownData: rentDropdown,
      },
    ];
  }, [documentsMDMS, t]);

  // Document validation check
  const isAllRequiredUploaded = useMemo(() => {
    // 1. Check Owner docs
    const ownerValid = ownerDocRequirements.every((req) => {
      if (!req.required) return true;
      return ownerDocs.some((doc) => doc?.documentType?.startsWith(req.code) && doc?.fileStoreId);
    });
    if (!ownerValid) return false;

    // 2. Check each tenant's docs
    for (const tenant of tenants) {
      const tDocs = tenantDocs[tenant.id] || [];
      const tenantValid = tenantDocRequirements.every((req) => {
        if (!req.required) return true;
        return tDocs.some((doc) => doc?.documentType?.startsWith(req.code) && doc?.fileStoreId);
      });
      if (!tenantValid) return false;
    }

    return true;
  }, [ownerDocRequirements, tenantDocRequirements, ownerDocs, tenantDocs, tenants]);

  const handleSubmit = () => {
    const flatDocs = [
      ...ownerDocs,
      ...Object.values(tenantDocs).flat(),
    ];

    const documentStep = {
      ...savedGcdocuments,
      ownerDocuments: ownerDocs,
      tenantDocuments: tenantDocs,
      documents: flatDocs,
    };

    onSelect(config.key, documentStep, false);
  };

  const onSkip = () => onSelect();

  return (
    <div>
      <FormStep
        t={t}
        config={config}
        onSelect={handleSubmit}
        onSkip={onSkip}
        // isDisabled={!isAllRequiredUploaded}
      >
        <div>
          <CardSubHeader>{t("GC_PROOF_OF_DOCUMENTS") || "Document Upload"}</CardSubHeader>
          <CardLabelDesc>{t("GC_UPLOAD_RESTRICTIONS_TYPES") || "Supported formats: PDF, JPEG, JPG, PNG"}</CardLabelDesc>
          <CardLabelDesc>{t("GC_UPLOAD_RESTRICTIONS_SIZE") || "Maximum file size: 5 MB"}</CardLabelDesc>

          {/* Card 1: Owner Documents Card */}
          <div
            className="gc-doc-card-owner"
            style={{
              border: "1px solid #1d70b8",
              backgroundColor: "#f3f8fc",
              padding: "16px",
              borderRadius: "4px",
              marginTop: "20px",
              marginBottom: "24px",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                borderBottom: "1px solid #cbe0f1",
                paddingBottom: "8px",
                marginBottom: "16px",
              }}
            >
              <span style={{ fontWeight: "bold", fontSize: "16px", color: "#1d70b8" }}>
                1. {t("GC_OWNER_DOCUMENTS_TITLE") || "Card 1: Owner Documents"} ({ownerName} — {ownerMobile || "Registered Mobile"})
              </span>
              <span
                style={{
                  backgroundColor: "#1d70b8",
                  color: "#ffffff",
                  padding: "2px 8px",
                  borderRadius: "10px",
                  fontSize: "12px",
                }}
              >
                {t("GC_OWNER") || "Owner"}
              </span>
            </div>

            {ownerDocRequirements.map((req, idx) => (
              <SingleDocumentUploadField
                key={`owner-${idx}`}
                requirement={req}
                t={t}
                setError={setError}
                documents={ownerDocs}
                setDocuments={setOwnerDocs}
                userType={userType}
              />
            ))}
          </div>

          {/* Cards 2..N: Tenant Document Cards (One distinct card per added tenant) */}
          {tenants.map((tenant, tIdx) => {
            const currentTenantDocs = tenantDocs[tenant.id] || [];
            const setThisTenantDocs = (updater) => {
              setTenantDocs((prev) => {
                const existing = prev[tenant.id] || [];
                const updated = typeof updater === "function" ? updater(existing) : updater;
                return { ...prev, [tenant.id]: updated };
              });
            };

            return (
              <div
                key={tenant.id || tIdx}
                className="gc-doc-card-tenant"
                style={{
                  border: "1px solid #28a197",
                  backgroundColor: "#f2f9f8",
                  padding: "16px",
                  borderRadius: "4px",
                  marginBottom: "24px",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    borderBottom: "1px solid #ccebe8",
                    paddingBottom: "8px",
                    marginBottom: "16px",
                  }}
                >
                  <span style={{ fontWeight: "bold", fontSize: "16px", color: "#005a54" }}>
                    {t("GC_TENANT_CARD") || `Card ${tIdx + 2}: Tenant #${tIdx + 1} Documents`} ({tenant.name || "Tenant"} — {tenant.mobileNumber || "Mobile"} | Floor: {tenant.floorNo})
                  </span>
                  <span
                    style={{
                      backgroundColor: "#28a197",
                      color: "#ffffff",
                      padding: "2px 8px",
                      borderRadius: "10px",
                      fontSize: "12px",
                    }}
                  >
                    {t("GC_TENANT") || "Tenant"}
                  </span>
                </div>

                {tenantDocRequirements.map((req, reqIdx) => (
                  <SingleDocumentUploadField
                    key={`tenant-${tenant.id}-${reqIdx}`}
                    requirement={req}
                    t={t}
                    setError={setError}
                    documents={currentTenantDocs}
                    setDocuments={setThisTenantDocs}
                    userType={userType}
                  />
                ))}
              </div>
            );
          })}

          {error && <Toast label={error} onClose={() => setError(null)} error />}
        </div>
      </FormStep>
    </div>
  );
};

/**
 * Reusable single document selector + uploader with type dropdown and file validation
 */
function SingleDocumentUploadField({ requirement: req, t, setError, documents, setDocuments, userType }) {
  const user = Digit.UserService.getUser().info;
  const filteredDoc = documents?.find((item) => item?.documentType?.startsWith(req.code));

  const dropDownData = useMemo(() => {
    return (
      req.dropdownData?.map((item) => ({
        ...item,
        i18nKey: item.i18nKey || "GC_" + item.code?.replaceAll(".", "_"),
      })) || []
    );
  }, [req.dropdownData]);

  const [selectedSubtype, setSelectedSubtype] = useState(() => {
    if (filteredDoc && dropDownData.length > 0) {
      const match = dropDownData.find((d) => d.code === filteredDoc.documentType);
      return match || dropDownData[0];
    }
    return dropDownData[0] || {};
  });

  const [file, setFile] = useState(null);
  const [uploadedFile, setUploadedFile] = useState(() => filteredDoc?.fileStoreId || null);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    const currentCode = selectedSubtype?.code || req.code;
    setDocuments((prevDocs = []) => {
      const withoutThis = prevDocs.filter((d) => !d?.documentType?.startsWith(req.code));
      if (!uploadedFile) {
        return withoutThis;
      }
      return [
        ...withoutThis,
        {
          uuid: user?.uuid,
          documentType: currentCode,
          fileStoreId: uploadedFile,
          documentUid: uploadedFile,
        },
      ];
    });
  }, [uploadedFile, selectedSubtype]);

  useEffect(() => {
    (async () => {
      setError(null);
      if (file) {
        const validTypes = ["application/pdf", "image/jpeg", "image/png", "image/jpg"];
        if (!validTypes.includes(file.type)) {
          setError(t("CS_INVALID_FILE_TYPE") || "Invalid file format. Upload PDF, JPEG, JPG, or PNG.");
          return;
        }
        if (file.size >= 5242880) {
          setError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED") || "File exceeds maximum size of 5 MB.");
          return;
        }

        try {
          setUploadedFile(null);
          setIsUploading(true);
          const response = await Digit.UploadServices.Filestorage("GC", file, Digit.ULBService.getStateId());
          if (response?.data?.files?.length > 0) {
            setUploadedFile(response.data.files[0]?.fileStoreId);
          } else {
            setError(t("CS_FILE_UPLOAD_ERROR") || "File upload failed.");
          }
        } catch (err) {
          setError(t("CS_FILE_UPLOAD_ERROR") || "File upload failed.");
        } finally {
          setIsUploading(false);
        }
      }
    })();
  }, [file]);

  return (
    <div style={{ marginBottom: "18px" }}>
      <LabelFieldPair>
        <CardLabel className="card-label-smaller" style={{ fontWeight: "600" }}>
          {req.label} {req.required ? <span className="check-page-link-button">*</span> : null}
        </CardLabel>
        {dropDownData.length > 0 && (
          <Dropdown
            className={`form-field ${user?.type === "EMPLOYEE" ? "field-width--employee-50" : "field-width--full"}`}
            selected={selectedSubtype}
            placeholder={`Select ${req.label}`}
            option={dropDownData}
            select={(val) => setSelectedSubtype(val)}
            optionKey="name"
            t={t}
          />
        )}
      </LabelFieldPair>

      <LabelFieldPair>
        <CardLabel className="card-label-smaller" />
        <div className="field">
          <UploadFile
            onUpload={(e) => setFile(e.target.files[0])}
            onDelete={() => {
              setUploadedFile(null);
              setFile(null);
            }}
            id={`file-upload-${req.code}`}
            message={
              isUploading ? (
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Loader inline />
                  <span>Uploading...</span>
                </div>
              ) : uploadedFile ? (
                "✓ 1 File Uploaded"
              ) : (
                "No File Uploaded"
              )
            }
            textStyles={{ width: "100%" }}
            inputStyles={{ width: "280px" }}
            accept=".pdf, .jpeg, .jpg, .png"
            buttonType="button"
            error={!uploadedFile && req.required}
          />
        </div>
      </LabelFieldPair>
    </div>
  );
}

export default GCDocumentDetails;
