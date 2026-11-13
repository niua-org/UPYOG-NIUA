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


// Payload to be used when Create a Family
export const buildCreateFamilyPayload = ({ membersList, tenantId, selectedHeadUuid, familyDetails = {} }) => {
  console.log("membersListmembersListmembersList", familyDetails);

  const members = membersList || [];
  const finalHeadUuid = selectedHeadUuid || members[0]?.userUuid;

  const finalMembers = members.map((member) => {
    const data = familyDetails[member.userUuid] || {};
    const p = data.profileDetails || {};
    const inc = data.income || {};
    const veh = data.vehicles || {};
    const prop = data.property || {};

    // --- citizenDetails ---
    const citizenDetails = {
      name: p.name,
      gender: p.gender?.code || p.gender,
      dob: p.dob || undefined,                          // "1995-05-15" as-is, no UTC conversion
      mobileNumber: p.mobileNumber,
      alternateMobileNumber: p.altContactNumber,
      emailId: p.emailId,
      fatherName: p.fatherName,
      motherName: p.motherName,
      spouseName: p.spouseName,
      guardian: p.guardianName,
      caste: p.caste?.code,
      isSpeciallyAbled: p.speciallyAbled?.code === "Yes",
      documents: [
        p.identityProofFile?.fileStoreId && {
          documentType: p.identityProofType?.code || "IDENTITY_PROOF",
          filestoreId: p.identityProofFile.fileStoreId,
          fileName: p.identityProofFile.name,
          referenceType: "CITIZEN_PROFILE",
        },
        p.birthProofFile?.fileStoreId && {
          documentType: p.birthProofType?.code || "DOB_PROOF",
          filestoreId: p.birthProofFile.fileStoreId,
          fileName: p.birthProofFile.name,
          referenceType: "CITIZEN_PROFILE",
        },
        p.casteProof?.fileStoreId && {
          documentType: p.casteProofType?.code || "CASTE_CERTIFICATE",
          filestoreId: p.casteProof.fileStoreId,
          fileName: p.casteProof.name,
          referenceType: "CITIZEN_PROFILE",
        },
        p.disabilityProof?.fileStoreId && {
          documentType: p.disabilityProofType?.code || "DISABILITY_CERTIFICATE",
          filestoreId: p.disabilityProof.fileStoreId,
          fileName: p.disabilityProof.name,
          referenceType: "CITIZEN_PROFILE",
        },
      ].filter(Boolean),
    };

    // --- income ---
    const income = inc.annualIncome ? {
      occupation: inc.occupation?.code || inc.occupation,
      annualIncome: Number(inc.annualIncome),
      identifierType: inc.taxIdType?.code,
      identifierValue: inc.taxIdNumber,
      isVerified: inc.taxIdValidated || false,
      ...(inc.uploadedIncomeFile && {
        salaryProofDocument: {
          documentType: "SALARY_PROOF",
          filestoreId: inc.uploadedIncomeFile,
          referenceType: "INCOME",
        },
      }),
      ...(inc.uploadedTaxProof && {
        taxProofDocument: {
          documentType: `${inc.taxIdType?.code || "TAX"}_PROOF`,
          filestoreId: inc.uploadedTaxProof,
          referenceType: "INCOME",
        },
      }),
    } : undefined;

    // --- vehicles ---
    const vehicles = veh.vehicleNo ? [{
      vehicleNumber: veh.vehicleNo,
      purchaseDate: veh.purchaseYear,               // "2026-11-04" as-is
      vehicleType: veh.vehicleType?.code,
      vehicleCategory: veh.vehicleCategory?.code,
      fuelType: veh.fuelType?.code,
      isVerified: veh.vehicleNoValidated || false,
      ...(veh.uploadedRCFile && {
        rcDocument: {
          documentType: "VEHICLE_RC",
          filestoreId: veh.uploadedRCFile,
          referenceType: "VEHICLE",
        },
      }),
    }] : undefined;

    // --- properties ---
    let properties;
    if (prop.localityType === "RURAL") {
      properties = [{
        localityType: "RURAL",
        ownershipType: prop.ownershipType || "INDIVIDUAL",
        declarationConfirmed: true,
        ruralPropertyDetail: {
          gramPanchayat: prop.gramPanchayat?.name,
          villageName: prop.village?.name,
          wardNumber: prop.ward?.name,
          propertyAddress: prop.propertyAddress,
          propertyType: prop.propertyType?.code,
          propertyUsage: prop.propertyUsage?.code,
          surveyNumber: prop.surveyNumber,
        },
        owners: [{ ownerType: "INDIVIDUAL", userUuid: member.userUuid }],
        ...(prop.propertyDocument?.fileStoreId && {
          document: {
            documentType: prop.documentType?.code || "PROPERTY_ADDRESS_PROOF",
            filestoreId: prop.propertyDocument.fileStoreId,
            fileName: prop.propertyDocument.name,
            referenceType: "PROPERTY",
          },
        }),
      }];
    } else if (prop.localityType === "URBAN" && prop.primaryPropertyId) {
      properties = [{
        localityType: "URBAN",
        ownershipType: "INDIVIDUAL",
        declarationConfirmed: true,
        urbanPropertyDetail: {
          upyogPropertyId: prop.primaryPropertyId || prop.propertyId,
          propertyTaxAmount: prop.propertyTaxAmount || 0,
          urbanAddress: prop.propertyAddress,
        },
        owners: [{ ownerType: "INDIVIDUAL", userUuid: member.userUuid }],
      }];
    }

    return {
      userUuid: member.userUuid,
      name: member.name || p.name || "Family Member",
      relationshipWithHead: member.relationshipWithHead,
      isHead: member.userUuid === finalHeadUuid,
      isVerified: true,
      ...(Object.keys(citizenDetails).some((k) => k === "documents" ? citizenDetails.documents.length > 0 : !!citizenDetails[k]) && { citizenDetails }),
      ...(income && { income }),
      ...(vehicles && { vehicles }),
      ...(properties && { properties }),
    };
  });

  return {
    family: {
      tenantId,
      headUserUuid: finalHeadUuid,
      members: finalMembers,
      workflow: {
            action: "APPLY",
            businessService: "FAMILY_REGISTRATION",
            comment: "",
            assignees: []
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
