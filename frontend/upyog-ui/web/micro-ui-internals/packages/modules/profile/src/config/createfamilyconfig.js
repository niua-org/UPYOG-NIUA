/**
 * @file createfamilyconfig.js
 * @description Configuration for the Create Family Flow
 * 
 * 
 * @usage
 * Used to define steps and navigation in the PPP.
 */


export const createfamilyconfig =
    [
        {
            "head": "ES_TITLE_PROFILE_DETAILS",
            "body": [
                {
                    "route": "profileDetails",
                    "component": "CreateProfile",
                    "withoutLabel": true,
                    "key": "profileDetails",
                    "type": "component",
                    "nextStep": "propertyDetails",
                    "hideInEmployee": true,
                    "isMandatory": true,
                    "texts": {
                        "submitBarLabel": "COMMON_SAVE_NEXT",
                        "header": "ES_TITLE_PROFILE_DETAILS",
                        "cardText": "",
                    }
                },
            ],
        },
        {
            "head": "ES_TITILE_PROPERTY_DETAILS",
            "body": [
                {
                    "route": "propertyDetails",
                    "component": "PropertyDetails",
                    "withoutLabel": true,
                    "key": "property",
                    "type": "component",
                    "isMandatory": true,
                    "hideInEmployee": true,
                    "nextStep": "incomeDetails",
                    "texts": {
                        "submitBarLabel": "COMMON_SAVE_NEXT",
                        "header": "ES_TITILE_PROPERTY_DETAILS",
                        "cardText": "",
                    }
                },
            ],
        },
        {
            "head": "ES_TITLE_INCOME_DETAILS",
            "body": [
                {
                    "route": "incomeDetails",
                    "component": "IncomeDetails",
                    "withoutLabel": true,
                    "key": "income",
                    "type": "component",
                    "isMandatory": true,
                    "hideInEmployee": true,
                    "nextStep": "vehicles",
                    "texts": {
                        "submitBarLabel": "COMMON_SAVE_NEXT",
                        "header": "ES_TITLE_INCOME_DETAILS",
                        "cardText": "",
                    }
                },
            ],
        },
        {
            "head": "ES_TITLE_VEHICLE_DETAILS",
            "body":
                [
                    {
                        "route": "vehicles",
                        "component": "VehicleDetails",
                        "withoutLabel": true,
                        "texts": {
                            "submitBarLabel": "COMMON_SAVE_NEXT",
                            "header": "ES_TITLE_VEHICLE_DETAILS",
                        },
                        "key": "vehicles",
                        "nextStep": null, // last step now; was "documents" (see commented Documents step below)
                        "isMandatory": true,
                        "type": "component",
                    },
                ],
        },
        // Documents step is not needed for now; kept for future use.
        // When re-enabling: uncomment this block and set vehicles `nextStep` back to "documents".
//        {
//            "head": "ES_TITLE_DOCUMENT_DETAILS",
//            "body": [
//                {
//                    "route": "documents",
//                    "component": "DocumentDetails",
//                    "withoutLabel": true,
//                    "key": "documents",
//                    "type": "component",
//                    "nextStep": null,
//                    "texts": {
//                        "submitBarLabel": "COMMON_SAVE_NEXT",
//                        "header": "ES_TITLE_DOCUMENT_DETAILS",
//                    },
//                }
//            ],
//        },
    ];