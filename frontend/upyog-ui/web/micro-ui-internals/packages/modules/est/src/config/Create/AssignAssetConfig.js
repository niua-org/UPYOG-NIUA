export const Config =
[  
    {
        "head": "EST_ASSIGN_ASSETS_DETAILS",
        "body": [
            {
                "route":"info",
                "component":"ESTAssignAstRequiredDoc",
                "nextStep": "assign-assets",
                "key": "Documents"
            },
            {
                "route": "assign-assets",
                "component": "ESTAssignAssets",
                "withoutLabel": true,
                "key": "AssignAssetsData",
                "type": "component",
                "nextStep": null,
                "hideInEmployee": true,
                "isMandatory": true,
                "texts": {
                    "submitBarLabel": "COMMON_SAVE_NEXT",
                    "header": "EST_ASSIGN_PROPERTY_DETAILS",
                }
            },
            
        ],
    },
];