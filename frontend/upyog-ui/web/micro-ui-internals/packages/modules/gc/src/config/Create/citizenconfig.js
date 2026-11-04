/**
 * commonConfig
 * 
 * Defines the 6-step wizard configuration for the GC (Garbage Collection) 
 * application creation flow as requested:
 * - Step 1: Property Location & PT Validation (GCPropertyLocDetails)
 * - Step 2: Owner Details (ApplicantDetails)
 * - Step 3: Address Details (AddressDetails)
 * - Step 4: Specifications, Floor Detection & Tenant Add-On (GCSpecifications)
 * - Step 5: Multi-Card Document Uploads (GCDocumentDetails)
 * - Step 6: Application Review Summary (CheckPage)
 */
export const commonConfig = [
    {
        head: "GC_TITLE_SERVICE_REGISTRATION",
        body: [
            {
                route: "garbage-propertyLocation",
                component: "GCPropertyLocDetails",
                withoutLabel: true,
                key: "gcpropertylocdetails",
                type: "component",
                nextStep: "applicant-details",
                hideInEmployee: false,
                isMandatory: true,
                texts: {
                    submitBarLabel: "COMMON_SAVE_NEXT",
                    header: "GC_GARBAGE_PROPERTY_LOCATION"
                },
                timeLine: [{
                    currentStep: 1,
                    totalSteps: 6,
                    actions: "GC_GARBAGE_PROPERTY_LOCATION"
                }]
            },
            {
                route: "applicant-details",
                component: "ApplicantDetails",
                withoutLabel: true,
                key: "owner",
                type: "component",
                nextStep: "address-details",
                hideInEmployee: false,
                isMandatory: true,
                texts: {
                    submitBarLabel: "COMMON_SAVE_NEXT",
                    header: "GC_OWNER_DETAILS"
                },
                labels: {
                    applicantName: "GC_OWNER_NAME"
                },
                disableFields: {
                    applicantName: true,
                    mobileNumber: true,
                    alternateNumber: true,
                    gender: true,
                    dateOfBirth: true,
                    dateofBirth: true,
                    guardianName: true,
                    relationShipType: true,
                    emailId: true
                },
                additionaFields: {
                    gender: true,
                    dateofBirth: false,
                    guardianName: true,
                    relationShipType: true
                },
                timeLine: [{
                    currentStep: 2,
                    totalSteps: 6,
                    actions: "ES_TITILE_OWNER_DETAILS"
                }]
            },
            {
                route: "address-details",
                component: "AddressDetails",
                withoutLabel: true,
                key: "address",
                type: "component",
                nextStep: "garbage-specifications",
                hideInEmployee: false,
                isMandatory: true,
                texts: {
                    submitBarLabel: "COMMON_SAVE_NEXT",
                    header: "ES_ADDRESS_DETAILS"
                },
                disableFields: {
                    // addressType: true,
                    // houseNo: true,
                    // streetName: true,
                    // addressLine1: true,
                    // addressLine2: true,
                    // landmark: true,
                    // city: true,
                    // locality: true,
                    // pincode: true
                },
                timeLine: [{
                    currentStep: 3,
                    totalSteps: 6,
                    actions: "ES_ADDRESS_DETAILS"
                }]
            },
            {
                route: "garbage-specifications",
                component: "GCSpecifications",
                withoutLabel: true,
                key: "gcspecifications",
                type: "component",
                nextStep: "garbage-documents",
                hideInEmployee: false,
                isMandatory: true,
                texts: {
                    submitBarLabel: "COMMON_SAVE_NEXT",
                    header: "GC_GARBAGE_SPECIFICATIONS"
                },
                timeLine: [{
                    currentStep: 4,
                    totalSteps: 6,
                    actions: "GC_GARBAGE_SPECIFICATIONS"
                }]
            },
            {
                route: "garbage-documents",
                component: "GCDocumentDetails",
                withoutLabel: true,
                key: "gcdocuments",
                type: "component",
                nextStep: null,
                hideInEmployee: false,
                isMandatory: true,
                texts: {
                    submitBarLabel: "COMMON_SAVE_NEXT",
                    header: "GC_GARBAGE_DOCUMENTS"
                },
                timeLine: [{
                    currentStep: 5,
                    totalSteps: 6,
                    actions: "GC_GARBAGE_DOCUMENTS"
                }]
            },
            {
                route: "check",
                component: "CheckPage",
                withoutLabel: true,
                key: "check",
                type: "component",
                nextStep: null,
                hideInEmployee: false,
                isMandatory: true,
                texts: {
                    submitBarLabel: "COMMON_SAVE_NEXT",
                    header: "CS_COMMON_SUMMARY"
                },
                timeLine: [{
                    currentStep: 6,
                    totalSteps: 6,
                    actions: "COMMON_SUMMARY"
                }]
            }
        ]
    }
];