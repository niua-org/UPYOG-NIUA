/**
 * @file createfamilyconfig.js
 * @description Configuration for the Create Family Flow
 * 
 * 
 * @usage
 * Used to define steps and navigation in the citizen's PTR application.
 */


export const createfamilyconfig =
    [
        {
            "head": "ES_TITILE_PROFILE_DETAILS",
            "body": [
                {
                    "route": "profileDetails",
                    "component": "CreateProfile",
                    "withoutLabel": true,
                    "key": "profileDetails",
                    "type": "component",
                    "nextStep": "incomeDetails",
                    "hideInEmployee": true,
                    "isMandatory": true,
                    "texts": {
                        "submitBarLabel": "COMMON_NEXT",
                    }
                },
            ],
        },
        {
            "head": "ES_TITILE_INCOME_DETAILS",
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
                        "submitBarLabel": "COMMON_NEXT",
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
                            "submitBarLabel": "COMMON_NEXT",
                        },
                        "key": "vehicles",
                        "nextStep": "documents",
                        "isMandatory": true,
                        "type": "component",
                    },
                ],
        },
        {
            "head": "ES_TITILE_DOCUMENT_DETAILS",
            "body": [
                {
                    "route": "documents",
                    "component": "EditDocuments",
                    "withoutLabel": true,
                    "key": "documents",
                    "type": "component",
                    "nextStep": null,
                    "texts": {
                        "submitBarLabel": "COMMON_NEXT",
                    },
                }
            ],
        },
    ];