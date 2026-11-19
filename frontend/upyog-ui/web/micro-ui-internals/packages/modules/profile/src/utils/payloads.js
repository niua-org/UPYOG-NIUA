// All request payload builders used by the Edit Profile screen

// helper to convert all DOBs to epoch
const toEpoch = (dob) => {
  if (!dob) return undefined;
  if (typeof dob === "number") return dob;
  return new Date(dob).getTime();
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
 * TODO - Need to Enhance this payload as lots of info is un-necessary
 * */
export const buildCreateFamilyPayload = ({ membersList, tenantId, selectedHeadUuid, familyDetails = {} }) => {
  const members = membersList || [];
  const finalHeadUuid = selectedHeadUuid || members[0]?.userUuid;
  const headMember = members.find((m) => m.userUuid === finalHeadUuid);
  const headMemberData = familyDetails[finalHeadUuid] || {};
  const headProfile = headMemberData.profileDetails || {};
  const headProperty = headMemberData.property || {};

  // Derive family-level fields from head's session data
  const familyAddressPropertyId = headProperty.primaryPropertyId || undefined;
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
    const vehicles =
      vehicleDetails?.hasVehicle === "Yes" && vehicleDetails?.vehicleNo
        ? [
            {
              vehicleNumber: vehicleDetails.vehicleNo,
              purchaseDate: vehicleDetails.purchaseYear,
              vehicleType: vehicleDetails.vehicleType?.code,
              makerModel: vehicleDetails?.makerModel,
              category: vehicleDetails.vehicleCategory?.code,
              fuelType: vehicleDetails.fuelType?.code,
              isVerified: vehicleDetails.vehicleNoValidated || false,
              validationSource: vehicleDetails.validationSource || undefined,
              ...(vehicleDetails.uploadedRCFile && {
                rcDocument: {
                  documentType: "VEHICLE_RC",
                  filestoreId: vehicleDetails.uploadedRCFile,
                  referenceType: "VEHICLE",
                  active: true,
                },
              }),
            },
          ]
        : undefined;

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
            ownershipType: entry.ownershipType || "INDIVIDUAL",
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
export const buildVerifiedMemberInfo = ({ response = {}, cleanAadhaar = "" }) => ({
  ...response,
  userUuid: response?.userUuid || response?.uuid,
  uuid: response?.userUuid || response?.uuid,
  message: response?.message || "Aadhaar verified successfully",
  maskedAadhaar: response?.maskedAadhaar || (cleanAadhaar ? `XXXXXXXX${cleanAadhaar.slice(-4)}` : "NA"),
  name: response?.demographics?.name || response?.name,
  dob: response?.demographics?.dob || response?.dob,
  gender: response?.demographics?.gender || response?.gender,
  demographics: response?.demographics || {
    name: response?.name,
    dob: response?.dob,
    gender: response?.gender,
  },
});




/**
 * Builds the new Join Family API payload matching the /family/v1/member/_join contract.
 * Maps session form data (profileDetails, property, income, vehicles) into the backend payload.
 * 
 * @param {Object} params
 * @param {string} params.familyId - The target family ID to join
 * @param {string} params.tenantId - Tenant identifier (e.g. pg.citya)
 * @param {Object} params.verifiedData - Verified data from OTP response (contains consentToken/txnId)
 * @param {Object} params.applicantParams - Form data from Family_Details session cache
 * @param {string} params.relationshipCode - Normalized relationship code (e.g. SON)
 * @param {Object} [params.user] - Logged in profile user fallback
 * @returns {Object} Final payload for joinFamily API
 */
export const buildJoinFamilyPayload = ({
  familyId,
  tenantId,
  verifiedData = {},
  applicantParams = {},
  relationshipCode,
  user,
}) => {
  const profileDetails = applicantParams.profileDetails || {};
  const propertyDetails = applicantParams.property || {};
  const incomeDetails = applicantParams.income || {};
  const vehicleDetails = applicantParams.vehicles || {};

  // 1. Citizen Documents
  const citizenDocuments = [
    profileDetails.identityProofFile?.fileStoreId && {
      documentType: profileDetails.identityProofType?.code || "IDENTITY_PROOF",
      filestoreId: profileDetails.identityProofFile.fileStoreId,
    },
    profileDetails.birthProofFile?.fileStoreId && {
      documentType: profileDetails.birthProofType?.code || "BIRTH_PROOF",
      filestoreId: profileDetails.birthProofFile.fileStoreId,
    },
    profileDetails.casteProof?.fileStoreId && {
      documentType: profileDetails.casteProofType?.code || "CASTE_PROOF",
      filestoreId: profileDetails.casteProof.fileStoreId,
    },
    profileDetails.disabilityProof?.fileStoreId && {
      documentType: profileDetails.disabilityProofType?.code || "DISABILITY_PROOF",
      filestoreId: profileDetails.disabilityProof.fileStoreId,
    },
  ].filter(Boolean);

  // 2. Citizen Details
  const citizenDetails = {
    name: profileDetails.name || user?.name,
    mobileNumber: profileDetails.mobileNumber || user?.mobileNumber,
    gender: (profileDetails.gender?.code || profileDetails.gender?.value || profileDetails.gender || user?.gender || "").toUpperCase(),
    dob: toEpoch(profileDetails.dob || user?.dob),
    caste: (profileDetails.caste?.code || profileDetails.caste?.name || profileDetails.caste || "").toUpperCase() || undefined,
    photo: profileDetails.photo || user?.photo || undefined,
    ...(citizenDocuments.length > 0 && { documents: citizenDocuments }),
  };

  // 3. Properties Array
  let properties = [];
  const portfolio = propertyDetails.propertiesPortfolio;

  if (Array.isArray(portfolio) && portfolio.length > 0) {
    properties = portfolio.map((entry) => {
      const isRural = entry.localityType === "RURAL";
      if (isRural) {
        return {
          localityType: "RURAL",
          ownershipType: entry.ownershipType || "INDIVIDUAL",
          ruralPropertyDetail: {
            gramPanchayat: entry.gramPanchayat?.name || entry.gramPanchayat,
            villageName: entry.village?.name || entry.village,
            khasraNumber: entry.khasraNumber || entry.surveyNumber || undefined,
          },
          ...(entry.propertyDocument?.fileStoreId && {
            document: {
              documentType: entry.documentType?.code || "PROPERTY_REGISTRY",
              filestoreId: entry.propertyDocument.fileStoreId,
            },
          }),
        };
      }

      // Urban Property
      return {
        localityType: "URBAN",
        ownershipType: entry.ownershipType || "INDIVIDUAL",
        urbanPropertyDetail: {
          upyogPropertyId: entry.propertyId || entry.primaryPropertyId,
        },
        ...(entry.propertyDocument?.fileStoreId && {
          document: {
            documentType: entry.documentType?.code || "PROPERTY_REGISTRY",
            filestoreId: entry.propertyDocument.fileStoreId,
          },
        }),
      };
    });
  } else if (propertyDetails.localityType) {
    // Single Property Form fallback
    const isRural = propertyDetails.localityType === "RURAL";
    if (isRural) {
      properties.push({
        localityType: "RURAL",
        ownershipType: propertyDetails.ownershipType || "INDIVIDUAL",
        ruralPropertyDetail: {
          gramPanchayat: propertyDetails.gramPanchayat?.name || propertyDetails.gramPanchayat,
          villageName: propertyDetails.village?.name || propertyDetails.village,
          khasraNumber: propertyDetails.khasraNumber || propertyDetails.surveyNumber || undefined,
        },
        ...(propertyDetails.propertyDocument?.fileStoreId && {
          document: {
            documentType: propertyDetails.documentType?.code || "PROPERTY_REGISTRY",
            filestoreId: propertyDetails.propertyDocument.fileStoreId,
          },
        }),
      });
    } else {
      properties.push({
        localityType: "URBAN",
        ownershipType: propertyDetails.ownershipType || "INDIVIDUAL",
        urbanPropertyDetail: {
          upyogPropertyId: propertyDetails.primaryPropertyId || propertyDetails.propertyId,
        },
        ...(propertyDetails.propertyDocument?.fileStoreId && {
          document: {
            documentType: propertyDetails.documentType?.code || "PROPERTY_REGISTRY",
            filestoreId: propertyDetails.propertyDocument.fileStoreId,
          },
        }),
      });
    }
  }

  // 4. Income Object
  const income = incomeDetails.annualIncome
    ? {
        annualIncome: Number(incomeDetails.annualIncome),
        occupation: (incomeDetails.occupation?.code || incomeDetails.occupation?.name || incomeDetails.occupation || "").toUpperCase(),
        identifierType: (incomeDetails.taxIdType?.code || incomeDetails.taxIdType?.name || incomeDetails.taxIdType || "").toUpperCase(),
        identifierId: incomeDetails.taxIdNumber || undefined,
        ...(incomeDetails.uploadedIncomeFile && {
          salaryProofDocument: {
            documentType: incomeDetails.incomeProofType?.code || "SALARY_SLIP",
            filestoreId: incomeDetails.uploadedIncomeFile,
          },
        }),
      }
    : undefined;

  // 5. Vehicles Array
  const vehicles =
    (vehicleDetails?.hasVehicle === "Yes" || vehicleDetails?.vehicleNo) && vehicleDetails?.vehicleNo
      ? [
          {
            vehicleNumber: vehicleDetails.vehicleNo,
            vehicleType: (vehicleDetails.vehicleType?.code || vehicleDetails.vehicleType?.name || vehicleDetails.vehicleType || "").toUpperCase().replace(/\s+/g, "_"),
            ...(vehicleDetails.uploadedRCFile && {
              rcDocument: {
                documentType: "VEHICLE_RC",
                filestoreId: vehicleDetails.uploadedRCFile,
              },
            }),
          },
        ]
      : [];

  return {
    familyId,
    tenantId,
    consentToken: verifiedData?.consentToken || verifiedData?.txnId,
    relationshipWithHead: relationshipCode,
    citizenDetails,
    properties,
    ...(income && { income }),
    vehicles,
  };
};