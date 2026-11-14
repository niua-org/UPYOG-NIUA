// All request payload builders used by the Edit Profile screen

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
  guardianName: form?.guardianName
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
    guardianName: form?.guardianName
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
export const buildCreateDocumentPayload = ({ uuid, tenantId, documentType, filestoreId, fileName }) => ({
  uuid,
  tenantId,
  documentType,
  filestoreId,
  ...(fileName && { fileName }),
  active: true,
});


// Builds the Family Create API payload from familyDetails session data + staged members list
export const buildCreateFamilyPayload = ({ membersList, tenantId, selectedHeadUuid, familyDetails = {} }) => {
  const members = membersList || [];
  const finalHeadUuid = selectedHeadUuid || members[0]?.userUuid;

  const finalMembers = members.map((member) => {
    const memberData = familyDetails[member.userUuid] || {};
    const profileDetails = memberData.profileDetails || {};
    const incomeDetails = memberData.income || {};
    const vehicleDetails = memberData.vehicles || {};
    const propertyDetails = memberData.property || {};

    // --- Citizen Documents (identity, DOB, caste, disability proofs) ---
    const citizenDocuments = [
      profileDetails.identityProofFile?.fileStoreId && {
        documentType: profileDetails.identityProofType?.code || "IDENTITY_PROOF",
        filestoreId: profileDetails.identityProofFile.fileStoreId,
        fileName: profileDetails.identityProofFile.name,
        referenceType: "CITIZEN_PROFILE",
        active: true,
      },
      profileDetails.birthProofFile?.fileStoreId && {
        documentType: profileDetails.birthProofType?.code || "DOB_PROOF",
        filestoreId: profileDetails.birthProofFile.fileStoreId,
        fileName: profileDetails.birthProofFile.name,
        referenceType: "CITIZEN_PROFILE",
        active: true,
      },
      profileDetails.casteProof?.fileStoreId && {
        documentType: profileDetails.casteProofType?.code || "CASTE_CERTIFICATE",
        filestoreId: profileDetails.casteProof.fileStoreId,
        fileName: profileDetails.casteProof.name,
        referenceType: "CITIZEN_PROFILE",
        active: true,
      },
      profileDetails.disabilityProof?.fileStoreId && {
        documentType: profileDetails.disabilityProofType?.code || "DISABILITY_CERTIFICATE",
        filestoreId: profileDetails.disabilityProof.fileStoreId,
        fileName: profileDetails.disabilityProof.name,
        referenceType: "CITIZEN_PROFILE",
        active: true,
      },
    ].filter(Boolean);

    // --- Citizen Details ---
    const citizenDetails = {
      name: profileDetails.name,
      gender: profileDetails.gender?.code || profileDetails.gender,
      dob: profileDetails.dob || undefined,
      mobileNumber: profileDetails.mobileNumber,
      alternateMobileNumber: profileDetails.altContactNumber,
      emailId: profileDetails.emailId,
      fatherName: profileDetails.fatherName,
      motherName: profileDetails.motherName,
      spouseName: profileDetails.spouseName,
      guardian: profileDetails.guardianName,
      caste: profileDetails.caste?.code,
      isSpeciallyAbled: profileDetails.speciallyAbled?.code === "Yes",
      relationshipWithHead: String(profileDetails.relationshipWithHof?.code || profileDetails.relationshipWithHof || "").toUpperCase().replace(/\s+/g, "_") || undefined,
      ...(citizenDocuments.length > 0 && { documents: citizenDocuments }),
    };

    // --- Income Details ---
    const income = incomeDetails.annualIncome ? {
      occupation: incomeDetails.occupation?.code || incomeDetails.occupation,
      annualIncome: Number(incomeDetails.annualIncome),
      identifierType: incomeDetails.taxIdType?.code,
      identifierValue: incomeDetails.taxIdNumber,
      isVerified: incomeDetails.taxIdValidated || false,
      ...(incomeDetails.uploadedIncomeFile && {
        salaryProofDocument: {
          documentType: "SALARY_PROOF",
          filestoreId: incomeDetails.uploadedIncomeFile,
          referenceType: "INCOME",
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
    } : undefined;

    // --- Vehicle Details ---
    const vehicles = vehicleDetails.vehicleNo ? [{
      vehicleNumber: vehicleDetails.vehicleNo,
      purchaseDate: vehicleDetails.purchaseYear,
      vehicleType: vehicleDetails.vehicleType?.code,
      vehicleCategory: vehicleDetails.vehicleCategory?.code,
      fuelType: vehicleDetails.fuelType?.code,
      isVerified: vehicleDetails.vehicleNoValidated || false,
      ...(vehicleDetails.uploadedRCFile && {
        rcDocument: {
          documentType: "VEHICLE_RC",
          filestoreId: vehicleDetails.uploadedRCFile,
          referenceType: "VEHICLE",
          active: true,
        },
      }),
    }] : undefined;

    // --- Property Details (URBAN or RURAL) ---
    let properties;
    if (propertyDetails.localityType === "RURAL") {
      const propertyOwners = [{ ownerType: "INDIVIDUAL", userUuid: member.userUuid, active: true }];
      if (propertyDetails.ownershipType === "JOINT" && propertyDetails.ownerName) {
        propertyOwners.push({
          ownerType: "INDIVIDUAL",
          ownerName: propertyDetails.ownerName,
          ownerMobile: propertyDetails.ownerMobile,
          active: true,
        });
      }
      properties = [{
        localityType: "RURAL",
        ownershipType: propertyDetails.ownershipType || "INDIVIDUAL",
        declarationConfirmed: true,
        ruralPropertyDetail: {
          gramPanchayat: propertyDetails.gramPanchayat?.name || propertyDetails.gramPanchayat,
          villageName: propertyDetails.village?.name || propertyDetails.village,
          wardNumber: propertyDetails.ward?.name || propertyDetails.ward,
          propertyAddress: propertyDetails.propertyAddress,
          propertyType: propertyDetails.propertyType?.code || propertyDetails.propertyType,
          propertyUsage: propertyDetails.propertyUsage?.code || propertyDetails.propertyUsage,
          surveyNumber: propertyDetails.surveyNumber,
        },
        owners: propertyOwners,
        ...(propertyDetails.propertyDocument?.fileStoreId && {
          document: {
            documentType: propertyDetails.documentType?.code || "PROPERTY_ADDRESS_PROOF",
            filestoreId: propertyDetails.propertyDocument.fileStoreId,
            fileName: propertyDetails.propertyDocument.name,
            referenceType: "PROPERTY",
            active: true,
          },
        }),
      }];
    } else if (propertyDetails.localityType === "URBAN" && propertyDetails.primaryPropertyId) {
      properties = [{
        localityType: "URBAN",
        ownershipType: "INDIVIDUAL",
        declarationConfirmed: true,
        urbanPropertyDetail: {
          upyogPropertyId: propertyDetails.primaryPropertyId || propertyDetails.propertyId,
          propertyTaxAmount: propertyDetails.propertyTaxAmount || 0,
          urbanAddress: propertyDetails.propertyAddress,
        },
        owners: [{ ownerType: "INDIVIDUAL", userUuid: member.userUuid, active: true }],
      }];
    }

    return {
      userUuid: member.userUuid,
      name: member.name || profileDetails.name,
      mobileNumber: member.mobileNumber || profileDetails.mobileNumber,
      gender: profileDetails.gender?.code || profileDetails.gender || member.gender,
      dob: profileDetails.dob || undefined,
      photo: profileDetails.photo || undefined,
      relationshipWithHead: String(member.relationshipWithHead || profileDetails.relationshipWithHof?.code || "").toUpperCase().replace(/\s+/g, "_") || undefined,
      isHead: member.userUuid === finalHeadUuid,
      isVerified: member.isVerified ?? true,
      active: true,
      citizenDetails,
      ...(income && { income }),
      ...(vehicles && { vehicles }),
      ...(properties && { properties }),
    };
  });

  return {
    Family: {
      tenantId,
      headUserUuid: finalHeadUuid,
      members: finalMembers,
      workflow: {
        action: "APPLY",
        businessService: "FAMILY_REGISTRATION",
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
