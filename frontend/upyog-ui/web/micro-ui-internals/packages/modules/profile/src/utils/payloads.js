// All request payload builders used by the Edit Profile screen

// helper to convert all DOBs to epoch
const toEpoch = (dob) => {
  if (!dob) return undefined;
  if (typeof dob === "number") return dob;
  return new Date(dob).getTime();
};

const getPropertyOwnershipType = (ownershipType) => {
  const value = ownershipType?.code || ownershipType;
  if (value === "INSTITUTION") return "ORGANISATION";
  return value || "INDIVIDUAL";
};

// Body for Digit.UserService.updateUser
export const buildUpdateUserPayload = ({ userInfo, form, photoId, locale }) => ({
  ...userInfo,
  name: form.name,
  dob: form.dob ? form.dob.split("-").reverse().join("/") : "", // yyyy-mm-dd -> dd/mm/yyyy
  gender: form.gender?.value,
  emailId: form.emailId,
  altContactNumber: form.altContactNumber,
  photo: photoId,
  locale,
  fatherName: form?.fatherName,
  motherName: form?.motherName,
  spouseName: form?.spouseName,
  guardianName: form?.guardianName,
});

// Updated session user (stored in localStorage) after a successful update
export const buildSessionUser = ({ sessionUser, form, photoThumb }) => ({
  ...sessionUser,
  info: {
    ...sessionUser.info,
    name: form.name,
    mobileNumber: form.mobileNumber,
    altContactNumber: form.altContactNumber,
    emailId: form.emailId,
    permanentCity: form.city,
    photo: photoThumb,
    fatherName: form?.fatherName,
    motherName: form?.motherName,
    spouseName: form?.spouseName,
    guardianName: form?.guardianName,
  },
});

// Body for Digit.UserService.changePassword
// NOTE: change-password UI was commented out in the old component, so this is not wired yet
export const buildChangePasswordPayload = ({ currentPassword, newPassword, confirmPassword, tenantId, userName }) => ({
  existingPassword: currentPassword,
  newPassword,
  confirmPassword,
  tenantId,
  type: "EMPLOYEE",
  username: userName,
});

// Body for Digit.UserService.updateUserDocument (`document` is singular)
export const buildUpdateDocumentPayload = ({ existingDoc, tenantId, filestoreId, fileName }) => ({
  id: existingDoc.id,
  tenantId,
  documentType: existingDoc.documentType,
  documentUid: existingDoc.documentUid || "",
  filestoreId,
  fileName: fileName || existingDoc.fileName || "",
  isVerified: existingDoc.isVerified ?? true,
  additionalDetails: existingDoc.additionalDetails || {},
});

// Body item for Digit.UserService.createUserDocument (`documents` is an array)
export const buildCreateDocumentPayload = ({ userUuid, tenantId, documentType, filestoreId, fileName }) => ({
  userUuid,
  tenantId,
  documentType,
  filestoreId,
  ...(fileName && { fileName }),
  active: true,
});

/**
 * Builds the Family Create API payload from familyDetails session data + staged members list
 * Property fields are read only from portfolio entries; noProperty suppresses the array.
 * TODO - Need to Enhance this payload as lots of info is un-necessary
 * */
export const buildCreateFamilyPayload = ({ membersList, tenantId, selectedHeadUuid, familyDetails = {} }) => {
  const members = membersList || [];
  const finalHeadUuid = selectedHeadUuid || members[0]?.userUuid;
  const headMember = members.find((m) => m.userUuid === finalHeadUuid);
  const headMemberData = familyDetails[finalHeadUuid] || {};
  const headProfile = headMemberData.profileDetails || {};
  const headPropertyPortfolio = headMemberData.property?.propertiesPortfolio || [];

  // Derive family-level fields from head's session data
  const familyAddressPropertyId =
    headPropertyPortfolio.find((property) => property.localityType === "URBAN" && property.isPrimaryAddress)?.propertyId ||
    undefined;
  const category = headProfile.caste?.code || undefined;

  const finalMembers = members.map((member) => {
    const memberData = familyDetails[member.userUuid] || {};
    const profileDetails = memberData.profileDetails || {};
    const incomeDetails = memberData.income || {};
    const vehicleDetails = memberData.vehicles || {};
    const propertyDetails = memberData.property || {};

    // --- Citizen Documents ---
    const citizenDocuments = [
      profileDetails.identityProofFile?.fileStoreId && {
        documentType: profileDetails.identityProofType?.code,
        filestoreId: profileDetails.identityProofFile.fileStoreId,
        fileName: profileDetails.identityProofFile.name,
        referenceType: "CITIZEN_PROFILE",
        active: true,
      },
      profileDetails.birthProofFile?.fileStoreId && {
        documentType: profileDetails.birthProofType?.code,
        filestoreId: profileDetails.birthProofFile.fileStoreId,
        fileName: profileDetails.birthProofFile.name,
        referenceType: "CITIZEN_PROFILE",
        active: true,
      },
      profileDetails.casteProof?.fileStoreId && {
        documentType: profileDetails.casteProofType?.code,
        filestoreId: profileDetails.casteProof.fileStoreId,
        fileName: profileDetails.casteProof.name,
        referenceType: "CITIZEN_PROFILE",
        active: true,
      },
      profileDetails.disabilityProof?.fileStoreId && {
        documentType: profileDetails.disabilityProofType?.code,
        filestoreId: profileDetails.disabilityProof.fileStoreId,
        fileName: profileDetails.disabilityProof.name,
        referenceType: "CITIZEN_PROFILE",
        active: true,
      },
    ].filter(Boolean);

    // --- Citizen Details ---
    const citizenDetails = {
      uuid: member.userUuid,
      userName: profileDetails.mobileNumber || member.mobileNumber,
      name: profileDetails.name || member.name,
      gender: profileDetails.gender?.code || profileDetails.gender,
      dob: toEpoch(profileDetails.dob),
      mobileNumber: profileDetails.mobileNumber || member.mobileNumber,
      alternateMobileNumber: profileDetails.altContactNumber,
      emailId: profileDetails.emailId,
      fatherName: profileDetails.fatherName,
      motherName: profileDetails.motherName,
      spouseName: profileDetails.spouseName,
      guardian: profileDetails.guardianName,
      guardianRelation: profileDetails.guardianRelation?.code || profileDetails.guardianRelation,
      caste: profileDetails.caste?.code,
      isSpeciallyAbled: profileDetails.speciallyAbled?.code === "Yes",
      isIdentityVerified: profileDetails.isIdentityVerified ?? member.isVerified ?? false,
      relationshipWithHead:
        String(profileDetails.relationshipWithHof?.code || profileDetails.relationshipWithHof || "")
          .toUpperCase()
          .replace(/\s+/g, "_") || undefined,
      photo: profileDetails.photo || undefined,
      ...(citizenDocuments.length > 0 && { documents: citizenDocuments }),
    };

    // --- Income Details ---
    const income = incomeDetails.annualIncome
      ? {
          occupation: incomeDetails.occupation?.code || incomeDetails.occupation,
          annualIncome: Number(incomeDetails.annualIncome),
          identifierType: incomeDetails.taxIdType?.code,
          identifierValue: incomeDetails.taxIdNumber,
          isVerified: incomeDetails.taxIdValidated || false,
          verificationSource: incomeDetails.verificationSource || undefined,
          otpVerified: incomeDetails.otpVerified || false,
          ...(incomeDetails.uploadedIncomeFile && {
            salaryProofDocument: {
              documentType: incomeDetails.incomeProofType?.code || "SALARY_PROOF",
              filestoreId: incomeDetails.uploadedIncomeFile,
              referenceType: "INCOME",
              fileName: incomeDetails.incomeProofType?.code,
              active: true,
            },
          }),
          ...(incomeDetails.uploadedTaxProof && {
            taxProofDocument: {
              documentType: `${incomeDetails.taxIdType?.code || "TAX"}_PROOF`,
              filestoreId: incomeDetails.uploadedTaxProof,
              referenceType: "INCOME",
              active: true,
            },
          }),
          ...(incomeDetails.employerName && {
            additionalDetails: {
              employerName: incomeDetails.employerName,
              designation: incomeDetails.designation,
            },
          }),
        }
      : undefined;

    // --- Vehicle Details ---
    let vehicles;
    if (vehicleDetails?.hasVehicle === "Yes") {
      const list =
        Array.isArray(vehicleDetails.vehicleList) && vehicleDetails.vehicleList.length > 0
          ? vehicleDetails.vehicleList
          : vehicleDetails.vehicleNo
            ? [vehicleDetails]
            : [];

      vehicles = list.map((veh) => ({
        vehicleNumber: veh.vehicleNo,
        purchaseDate: veh.purchaseYear || undefined,
        vehicleType: (veh.vehicleType?.code || veh.vehicleType?.name || veh.vehicleType || "").toUpperCase().replace(/\s+/g, "_"),
        makerModel: veh.makerModel || undefined,
        category: veh.vehicleCategory?.code || veh.vehicleCategory || undefined,
        fuelType: veh.fuelType?.code || veh.fuelType || undefined,
        isVerified: veh.vehicleNoValidated || true,
        validationSource: veh.validationSource || "VAHAN",
        ...(veh.uploadedRCFile && {
          rcDocument: {
            documentType: "RC_CERTIFICATE",
            filestoreId: veh.uploadedRCFile,
            fileName: "RC_CERTIFICATE",
            referenceType: "VEHICLE",
            active: true,
          },
        }),
      }));
    }

    // --- Property Details ---
    let properties;
    const portfolio = propertyDetails.propertiesPortfolio;

    if (Array.isArray(portfolio) && portfolio.length > 0) {
      properties = portfolio.map((entry) => {
        const propertyOwners = [{ ownerType: "INDIVIDUAL", userUuid: member.userUuid, active: true }];
        if (entry.ownershipType === "JOINT" && entry.ownerName) {
          propertyOwners.push({ ownerType: "INDIVIDUAL", ownerName: entry.ownerName, ownerMobile: entry.ownerMobile, active: true });
        }

        if (entry.localityType === "RURAL") {
          return {
            localityType: "RURAL",
            ownershipType: getPropertyOwnershipType(entry.ownershipType),
            declarationConfirmed: true,
            ruralPropertyDetail: {
              gramPanchayat: entry.gramPanchayat?.name || entry.gramPanchayat,
              villageName: entry.village?.name || entry.village,
              wardNumber: entry.ward?.name || entry.ward,
              propertyAddress: entry.propertyAddress,
              propertyType: entry.propertyType?.code || entry.propertyType,
              propertyUsage: entry.propertyUsage?.code || entry.propertyUsage,
              surveyNumber: entry.surveyNumber,
            },
            owners: propertyOwners,
            ...(entry.propertyDocument?.fileStoreId && {
              document: {
                documentType: entry.documentType?.code || "PROPERTY_ADDRESS_PROOF",
                filestoreId: entry.propertyDocument.fileStoreId,
                fileName: entry.propertyDocument.name,
                referenceType: "PROPERTY",
                active: true,
              },
            }),
          };
        }

        // URBAN
        return {
          localityType: "URBAN",
          ownershipType: "INDIVIDUAL",
          declarationConfirmed: true,
          urbanPropertyDetail: {
            upyogPropertyId: entry.propertyId,
            propertyTaxAmount: entry.propertyTaxAmount || 0,
            urbanAddress: entry.propertyAddress,
          },
          owners: [{ ownerType: "INDIVIDUAL", userUuid: member.userUuid, active: true }],
        };
      });
    }

    // --- Member-level additionalDetails ---
    const memberAdditionalDetails =
      profileDetails.educationLevel || profileDetails.maritalStatus
        ? {
            educationLevel: profileDetails.educationLevel?.code || profileDetails.educationLevel,
            maritalStatus: profileDetails.maritalStatus?.code || profileDetails.maritalStatus,
            disabilityPercentage: profileDetails.disabilityPercentage ?? 0,
          }
        : undefined;

    return {
      userUuid: member.userUuid,
      name: member.name || profileDetails.name,
      mobileNumber: member.mobileNumber || profileDetails.mobileNumber,
      gender: profileDetails.gender?.code || profileDetails.gender || member.gender,
      dob: toEpoch(profileDetails.dob),
      photo: profileDetails.photo || undefined,
      relationshipWithHead:
        String(member.relationshipWithHead || profileDetails.relationshipWithHof?.code || "")
          .toUpperCase()
          .replace(/\s+/g, "_") || undefined,
      isHead: member.userUuid === finalHeadUuid,
      isVerified: member.isVerified ?? true,
      active: true,
      consentToken: member.consentToken || profileDetails.consentToken || undefined,
      consentDate: member.consentDate || profileDetails.consentDate || undefined,
      effectiveFrom: member.effectiveFrom || profileDetails.effectiveFrom || undefined,
      citizenDetails,
      ...(memberAdditionalDetails && { additionalDetails: memberAdditionalDetails }),
      ...(income && { income }),
      ...(vehicles && { vehicles }),
      ...(properties && { properties }),
    };
  });

  return {
    Family: {
      tenantId,
      headUserUuid: finalHeadUuid,
      headName: headMember?.name || headProfile.name,
      category,
      familySource: "CITIZEN_PORTAL",
      familyAddressPropertyId,
      members: finalMembers,
      workflow: {
        action: "APPLY",
        businessService: "family-service",
        comment: "",
        assignees: [],
      },
    },
  };
};

// Maps and standardizes the Aadhaar OTP verification response into a verified member object
export const buildVerifiedMemberInfo = ({ response = {}, cleanAadhaar = "" }) => {
  const demographics = response?.demographics || {};
  const mobile = demographics?.mobileNumber || response?.mobileNumber;
  return {
    ...response,
    userUuid: response?.userUuid || response?.uuid,
    uuid: response?.userUuid || response?.uuid,
    message: response?.message || "Aadhaar verified successfully",
    maskedAadhaar: response?.maskedAadhaar || (cleanAadhaar ? `XXXXXXXX${cleanAadhaar.slice(-4)}` : "NA"),
    name: demographics?.name || response?.name,
    dob: demographics?.dob || response?.dob,
    gender: demographics?.gender || response?.gender,
    mobileNumber: mobile,
    demographics: {
      ...demographics,
      name: demographics?.name || response?.name,
      dob: demographics?.dob || response?.dob,
      gender: demographics?.gender || response?.gender,
      mobileNumber: mobile,
    },
  };
};




/**
 * Builds the Join Family API payload matching the updated /family/v1/member/_join contract.
 * Standardizes applicant's personal, property, income, and vehicle details into the backend contract.
 * 
 * @param {Object} params
 * @param {string} params.familyId - The target family ID to join
 * @param {string} params.tenantId - Tenant identifier (e.g. pg.citya)
 * @param {string} params.applicantUserUuid - Applicant's user UUID
 * @param {Object} params.verifiedData - Verified data from OTP response (txnId / consentToken)
 * @param {Object} params.applicantParams - Form data from Family_Details session cache
 * @param {string} params.relationshipCode - Selected relationship with HOF (e.g. SON)
 * @param {Object} [params.user] - Logged in profile user fallback
 * @returns {Object} Final payload for joinFamily API
 */
export const buildJoinFamilyPayload = ({
  familyId,
  tenantId,
  applicantUserUuid,
  verifiedData = {},
  applicantParams = {},
  relationshipCode,
  user,
}) => {
  const profileDetails = applicantParams.profileDetails || {};
  const propertyDetails = applicantParams.property || {};
  const incomeDetails = applicantParams.income || {};
  const vehicleDetails = applicantParams.vehicles || {};

  const effectiveUuid = applicantUserUuid || user?.uuid || Digit.UserService.getUser()?.info?.uuid;

  // 1. Citizen Documents
  const citizenDocuments = [
    profileDetails.identityProofFile?.fileStoreId && {
      documentType: profileDetails.identityProofType?.code || "AADHAAR",
      filestoreId: profileDetails.identityProofFile.fileStoreId,
      fileName: profileDetails.identityProofFile.name,
      referenceType: "CITIZEN_PROFILE",
      active: true,
    },
    profileDetails.birthProofFile?.fileStoreId && {
      documentType: profileDetails.birthProofType?.code,
      filestoreId: profileDetails.birthProofFile.fileStoreId,
      fileName: profileDetails.birthProofFile.name,
      referenceType: "CITIZEN_PROFILE",
      active: true,
    },
    profileDetails.casteProof?.fileStoreId && {
      documentType: profileDetails.casteProofType?.code,
      filestoreId: profileDetails.casteProof.fileStoreId,
      fileName: profileDetails.casteProof.name,
      referenceType: "CITIZEN_PROFILE",
      active: true,
    },
    profileDetails.disabilityProof?.fileStoreId && {
      documentType: profileDetails.disabilityProofType?.code,
      filestoreId: profileDetails.disabilityProof.fileStoreId,
      fileName: profileDetails.disabilityProof.name,
      referenceType: "CITIZEN_PROFILE",
      active: true,
    },
  ].filter(Boolean);

  // 2. Citizen Details
  const citizenDetails = {
    uuid: effectiveUuid,
    userName: profileDetails.mobileNumber || user?.mobileNumber || user?.userName,
    name: profileDetails.name || user?.name,
    mobileNumber: profileDetails.mobileNumber || user?.mobileNumber,
    gender: (profileDetails.gender?.code || profileDetails.gender?.value || profileDetails.gender || user?.gender || "").toUpperCase(),
    dob: toEpoch(profileDetails.dob || user?.dob),
    photo: profileDetails.photo || user?.photo || undefined,
    emailId: profileDetails.emailId || user?.emailId || undefined,
    alternateMobileNumber: profileDetails.altContactNumber || user?.altContactNumber || undefined,
    fatherName: profileDetails.fatherName || user?.fatherName || undefined,
    motherName: profileDetails.motherName || user?.motherName || undefined,
    spouseName: profileDetails.spouseName || user?.spouseName || undefined,
    caste: (profileDetails.caste?.code || profileDetails.caste?.name || profileDetails.caste || "").toUpperCase() || undefined,
    isSpeciallyAbled: profileDetails.speciallyAbled?.code === "Yes" || profileDetails.isSpeciallyAbled === true,
    ...(citizenDocuments.length > 0 && { documents: citizenDocuments }),
  };

  // 3. Properties Array
  let properties = [];
  const portfolio = propertyDetails.propertiesPortfolio;

  if (Array.isArray(portfolio) && portfolio.length > 0) {
    properties = portfolio.map((entry) => {
      const isRural = entry.localityType === "RURAL";
      const ownerType = entry.ownershipType === "JOINT" ? "JOINT_OWNER" : "INDIVIDUAL";

      if (isRural) {
        return {
          propertyId: entry.propertyId || undefined,
          localityType: "RURAL",
          ownershipType: entry.ownershipType || "INDIVIDUAL",
          declarationConfirmed: true,
          ruralPropertyDetail: {
            gramPanchayat: entry.gramPanchayat?.name || entry.gramPanchayat,
            villageName: entry.village?.name || entry.village,
            wardNumber: entry.ward?.name || entry.ward || undefined,
            propertyAddress: entry.propertyAddress,
            propertyType: entry.propertyType?.code || entry.propertyType,
            propertyUsage: entry.propertyUsage?.code || entry.propertyUsage,
            surveyNumber: entry.surveyNumber || entry.khasraNumber || undefined,
          },
          owners: [
            {
              userUuid: effectiveUuid,
              ownerType,
              active: true,
            },
          ],
          ...(entry.propertyDocument?.fileStoreId && {
            document: {
              documentType: entry.documentType?.code,
              filestoreId: entry.propertyDocument.fileStoreId,
              fileName: entry.propertyDocument.name,
              referenceType: "PROPERTY",
              active: true,
            },
          }),
        };
      }

      // Urban Property
      return {
        propertyId: entry.propertyId || undefined,
        localityType: "URBAN",
        ownershipType: entry.ownershipType || "INDIVIDUAL",
        declarationConfirmed: true,
        urbanPropertyDetail: {
          upyogPropertyId: entry.propertyId || entry.primaryPropertyId,
          propertyTaxAmount: entry.propertyTaxAmount || 0,
          urbanAddress: entry.propertyAddress,
        },
        owners: [
          {
            userUuid: effectiveUuid,
            ownerType: "INDIVIDUAL",
            active: true,
          },
        ],
        ...(entry.propertyDocument?.fileStoreId && {
          document: {
            documentType: entry.documentType?.code,
            filestoreId: entry.propertyDocument.fileStoreId,
            fileName: entry.propertyDocument.name,
            referenceType: "PROPERTY",
            active: true,
          },
        }),
      };
    });
  } else if (propertyDetails.localityType) {
    // Single Property Form fallback
    const isRural = propertyDetails.localityType === "RURAL";
    const ownerType = propertyDetails.ownershipType === "JOINT" ? "JOINT_OWNER" : "INDIVIDUAL";

    if (isRural) {
      properties.push({
        propertyId: propertyDetails.propertyId || undefined,
        localityType: "RURAL",
        ownershipType: propertyDetails.ownershipType || "INDIVIDUAL",
        declarationConfirmed: true,
        ruralPropertyDetail: {
          gramPanchayat: propertyDetails.gramPanchayat?.name || propertyDetails.gramPanchayat,
          villageName: propertyDetails.village?.name || propertyDetails.village,
          wardNumber: propertyDetails.ward?.name || propertyDetails.ward || undefined,
          propertyAddress: propertyDetails.propertyAddress,
          propertyType: propertyDetails.propertyType?.code || propertyDetails.propertyType,
          propertyUsage: propertyDetails.propertyUsage?.code || propertyDetails.propertyUsage,
          surveyNumber: propertyDetails.surveyNumber || propertyDetails.khasraNumber || undefined,
        },
        owners: [
          {
            userUuid: effectiveUuid,
            ownerType,
            active: true,
          },
        ],
        ...(propertyDetails.propertyDocument?.fileStoreId && {
          document: {
            documentType: propertyDetails.documentType?.code,
            filestoreId: propertyDetails.propertyDocument.fileStoreId,
            fileName: propertyDetails.propertyDocument.name,
            referenceType: "PROPERTY",
            active: true,
          },
        }),
      });
    } else {
      properties.push({
        propertyId: propertyDetails.propertyId || undefined,
        localityType: "URBAN",
        ownershipType: propertyDetails.ownershipType || "INDIVIDUAL",
        declarationConfirmed: true,
        urbanPropertyDetail: {
          upyogPropertyId: propertyDetails.primaryPropertyId || propertyDetails.propertyId,
          propertyTaxAmount: propertyDetails.propertyTaxAmount || 0,
          urbanAddress: propertyDetails.propertyAddress,
        },
        owners: [
          {
            userUuid: effectiveUuid,
            ownerType: "INDIVIDUAL",
            active: true,
          },
        ],
        ...(propertyDetails.propertyDocument?.fileStoreId && {
          document: {
            documentType: propertyDetails.documentType?.code,
            filestoreId: propertyDetails.propertyDocument.fileStoreId,
            fileName: propertyDetails.propertyDocument.name,
            referenceType: "PROPERTY",
            active: true,
          },
        }),
      });
    }
  }

  // 4. Income Object
  const income = incomeDetails.annualIncome
    ? {
        occupation: (incomeDetails.occupation?.code || incomeDetails.occupation?.name || incomeDetails.occupation || "").toUpperCase(),
        annualIncome: Number(incomeDetails.annualIncome),
        identifierType: (incomeDetails.taxIdType?.code || incomeDetails.taxIdType?.name || incomeDetails.taxIdType || "").toUpperCase(),
        identifierValue: incomeDetails.taxIdNumber || undefined,
        isVerified: incomeDetails.taxIdValidated || true,
        verificationSource: incomeDetails?.verificationSource,
        otpVerified: incomeDetails.otpVerified || true,
        ...(incomeDetails.uploadedIncomeFile && {
          salaryProofDocument: {
            documentType: incomeDetails.incomeProofType?.code,
            filestoreId: incomeDetails.uploadedIncomeFile,
            fileName: incomeDetails.incomeProofType?.code,
            referenceType: "INCOME",
            active: true,
          },
        }),
        ...(incomeDetails.uploadedTaxProof && {
          taxProofDocument: {
            documentType: `${incomeDetails.taxIdType?.code}_PROOF`,
            filestoreId: incomeDetails.uploadedTaxProof,
            fileName: `${incomeDetails.taxIdType?.code}_PROOF`,
            referenceType: "INCOME",
            active: true,
          },
        }),
        ...(incomeDetails.employerName && {
          additionalDetails: {
            employerName: incomeDetails.employerName,
            designation: incomeDetails.designation,
          },
        }),
      }
    : undefined;

  // 5. Vehicles Array
    let vehicles = [];
    if (vehicleDetails?.hasVehicle === "Yes" || vehicleDetails?.vehicleNo) {
      const list =
        Array.isArray(vehicleDetails.vehicleList) && vehicleDetails.vehicleList.length > 0
          ? vehicleDetails.vehicleList
          : vehicleDetails.vehicleNo
            ? [vehicleDetails]
            : [];

      vehicles = list.map((veh) => ({
        vehicleNumber: veh.vehicleNo,
        purchaseDate: veh.purchaseYear || undefined,
        isVerified: veh.vehicleNoValidated || true,
        validationSource: veh.validationSource || "VAHAN",
        vehicleType: (veh.vehicleType?.code || veh.vehicleType?.name || veh.vehicleType || "").toUpperCase().replace(/\s+/g, "_"),
        makerModel: veh.makerModel || undefined,
        fuelType: veh.fuelType?.code || veh.fuelType || undefined,
        category: veh.vehicleCategory?.code || veh.vehicleCategory || undefined,
        ...(veh.uploadedRCFile && {
          rcDocument: {
            documentType: "RC_CERTIFICATE",
            filestoreId: veh.uploadedRCFile,
            fileName: "RC_CERTIFICATE",
            referenceType: "VEHICLE",
            active: true,
          },
        }),
      }));
    }

  return {
    familyId,
    tenantId,
    applicantUserUuid: effectiveUuid,
    relationshipWithHead: relationshipCode,
    txnId: verifiedData?.txnId || verifiedData?.consentToken,
    citizenDetails,
    properties,
    ...(income && { income }),
    vehicles,
  };
};