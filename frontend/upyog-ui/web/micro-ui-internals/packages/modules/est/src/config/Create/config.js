export const Config = [
  {
    "head": "EST_COMMON_NEW_REGISTRATION",
      "body": [
        {
          "key": "newRegistration",
          "route": "newRegistration",
          "component": "NewRegistration",
          "nextStep": null,
          "isPreview": false,
          "withoutLabel": true,
          "type": "component",
          "hideInEmployee": false,
          "isMandatory": true,
          "sectionHeading": null,
       "form": [
            {
              "order": 0,
              "key": "EST_BUILDING_NAME",
              "field": {
                "code": "EST_BUILDING_NAME",
                "name": "buildingName",
                "placeholder": "EST_ENTER_BUILDING_NAME",
                "type": "text"
              },
              "validation": {
                "maxLength": 100,
                "pattern": "^[a-zA-Z0-9\\s]+$",
                "regex": {
                  "pattern": "[^a-zA-Z0-9\\s]",
                  "flags": "g"
                },
                "required": true,
                "disabled": false,
                "readOnly": false
              },
              "messages": {
                "error": "EST_INVALID_BUILDING_NAME"
              }
            },
            {
              "order": 1,
              "key": "EST_BUILDING_NUMBER",
              "field": {
                "code": "EST_BUILDING_NUMBER",
                "name": "buildingNo",
                "placeholder": "EST_ENTER_BUILDING_NUMBER",
                "type": "text"
              },
              "validation": {
                "maxLength": 10,
                "pattern": "^[0-9]+$",
                "regex": {
                  "pattern": "\\D",
                  "flags": "g"
                },
                "required": false,
                "disabled": false,
                "readOnly": false
              },
              "messages": {
                "error": "EST_INVALID_BUILDING_NUMBER"
              }
            },
            {
              "order": 2,
              "key": "EST_BUILDING_FLOOR",
              "field": {
                "code": "EST_BUILDING_FLOOR",
                "name": "floor",
                "placeholder": "EST_ENTER_BUILDING_FLOOR",
                "type": "text"
              },
              "validation": {
                "maxLength": 3,
                "pattern": "^[0-9]+$",
                "regex": {
                  "pattern": "\\D",
                  "flags": "g"
                },
                "required": false,
                "disabled": false,
                "readOnly": false
              },
              "messages": {
                "error": "EST_INVALID_BUILDING_FLOOR"
              }
            },
            {
              "order": 3,
              "key": "EST_BUILDING_BLOCK",
              "field": {
                "code": "EST_BUILDING_BLOCK",
                "name": "buildingBlock",
                "placeholder": "EST_ENTER_BUILDING_BLOCK",
                "type": "text"
              },
              "validation": {
                "maxLength": 50,
                "pattern": "^[a-zA-Z0-9\\s]+$",
                "regex": {
                  "pattern": "[^a-zA-Z0-9\\s]",
                  "flags": "g"
                },
                "required": false,
                "disabled": false,
                "readOnly": false
              },
              "messages": {
                "error": "EST_INVALID_BUILDING_BLOCK"
              }
            },
            {
              "order": 4,
              "key": "EST_CITY",
              "field": {
                "code": "EST_CITY",
                "name": "city",
                "placeholder": "EST_SELECT_CITY",
                "type": "dropdown"
              },
              "validation": {
                "required": false,
                "disabled": false,
                "readOnly": false
              }
            },
            {
              "order": 5,
              "key": "EST_LOCALITY",
              "field": {
                "code": "EST_LOCALITY",
                "name": "serviceType",
                "placeholder": "EST_SELECT_LOCALITY",
                "type": "dropdown"
              },
              "validation": {
                "required": false,
                "disabled": false,
                "readOnly": false
              }
            },
            {
              "order": 6,
              "key": "EST_TOTAL_PLOT_AREA",
              "field": {
                "code": "EST_TOTAL_PLOT_AREA",
                "name": "totalFloorArea",
                "placeholder": "EST_ENTER_TOTAL_PLOT_AREA",
                "type": "text",
                "unit": "( In sq.ft)"
              },
              "validation": {
                "pattern": "^[0-9]+$",
                "regex": {
                  "pattern": "\\D",
                  "flags": "g"
                },
                "required": false,
                "disabled": false,
                "readOnly": false
              },
              "messages": {
                "error": "EST_INVALID_TOTAL_PLOT_AREA"
              }
            },
            {
              "order": 7,
              "key": "EST_DIMENSION",
              "label": {
                "code": "EST_DIMENSION",
                "unit": "( In sq.ft)"
              },
              "type": "group",
              "children": [
                {
                  "order": 0,
                  "key": "EST_LENGTH",
                  "field": {
                    "code": "EST_LENGTH",
                    "name": "dimensionLength",
                    "placeholder": "EST_LENGTH",
                    "type": "text"
                  },
                  "validation": {
                    "pattern": "^[0-9]+$",
                    "regex": {
                      "pattern": "\\D",
                      "flags": "g"
                    },
                    "required": false,
                    "disabled": false,
                    "readOnly": false
                  },
                  "messages": {
                    "error": "EST_INVALID_LENGTH"
                  }
                },
                {
                  "order": 1,
                  "key": "EST_WIDTH",
                  "field": {
                    "code": "EST_WIDTH",
                    "name": "dimensionWidth",
                    "placeholder": "EST_WIDTH",
                    "type": "text"
                  },
                  "validation": {
                    "pattern": "^[0-9]+$",
                    "regex": {
                      "pattern": "\\D",
                      "flags": "g"
                    },
                    "required": false,
                    "disabled": false,
                    "readOnly": false
                  },
                  "messages": {
                    "error": "EST_INVALID_WIDTH"
                  }
                }
              ],
              "messages": {
                "error": "dimensionError"
              }
            },
            {
              "order": 8,
              "key": "EST_RATES",
              "field": {
                "code": "EST_RATES",
                "name": "rate",
                "placeholder": "EST_ENTER_RATE",
                "type": "text",
                "unit": "(Per sq ft)"
              },
              "validation": {
                "maxLength": 10,
                "pattern": "^[0-9]+$",
                "regex": {
                  "pattern": "\\D",
                  "flags": "g"
                },
                "required": false,
                "disabled": false,
                "readOnly": false
              },
              "messages": {
                "error": "EST_INVALID_RATE"
              }
            },
            {
              "order": 9,
              "key": "EST_ASSET_REFERENCE_NUMBER",
              "field": {
                "code": "EST_ASSET_REFERENCE_NUMBER",
                "name": "assetRef",
                "placeholder": "EST_ENTER_ASSET_REFERENCE_NUMBER",
                "type": "text"
              },
              "validation": {
                "maxLength": 50,
                "required": false,
                "disabled": false,
                "readOnly": false
              }
            },
            {
              "order": 10,
              "key": "EST_ASSET_TYPE",
              "field": {
                "code": "EST_ASSET_TYPE",
                "name": "assetType",
                "placeholder": "EST_SELECT_ASSET_TYPE",
                "type": "dropdown"
              },
              "validation": {
                "required": false,
                "disabled": false,
                "readOnly": false
              },
              "options": [
                {
                  "code": "ASSET_TYPE_RESIDENTIAL",
                  "value": "RESIDENTIAL"
                },
                {
                  "code": "ASSET_TYPE_COMMERCIAL",
                  "value": "COMMERCIAL"
                },
                {
                  "code": "ASSET_TYPE_INDUSTRIAL",
                  "value": "INDUSTRIAL"
                }
              ]
            }
          ],
          "actionButton": {
            "text": {
              "create": "SAVE_&_NEXT",
              "edit": "UPDATE"
            },
            "variant": "contained",
            "color": "primary"
          }
        },
        {
          "key": "ownerDetails",
          "route": "ownerDetails",
          "component": "OwnerDetails",
          "nextStep": null,
          "isPreview": false,
          "withoutLabel": false,
          "type": "component",
          "hideInEmployee": false,
          "isMandatory": true,
          "sectionHeading": "EST_OWNER_DETAILS",
          "form": [
            {
              "order": 0,
              "key": "EST_OWNER_NAME",
              "field": {
                "code": "EST_OWNER_NAME",
                "name": "ownerName",
                "placeholder": "EST_ENTER_OWNER_NAME",
                "type": "text"
              },
              "validation": {
                "maxLength": 60,
                "pattern": "^[a-zA-Z\\s]+$",
                "regex": {
                  "pattern": "[^a-zA-Z\\s]",
                  "flags": "g"
                },
                "required": true,
                "disabled": false,
                "readOnly": false
              },
              "messages": {
                "error": "EST_INVALID_OWNER_NAME"
              }
            },
            {
              "order": 1,
              "key": "EST_OWNER_MOBILE",
              "field": {
                "code": "EST_OWNER_MOBILE",
                "name": "ownerMobile",
                "placeholder": "EST_ENTER_OWNER_MOBILE",
                "type": "text"
              },
              "validation": {
                "maxLength": 10,
                "pattern": "^[6-9][0-9]{9}$",
                "regex": {
                  "pattern": "\\D",
                  "flags": "g"
                },
                "required": true,
                "disabled": false,
                "readOnly": false
              },
              "messages": {
                "error": "EST_INVALID_OWNER_MOBILE"
              }
            }
          ],
          "actionButton": {
            "text": {
              "create": "SAVE_&_NEXT",
              "edit": "UPDATE"
            },
            "variant": "contained",
            "color": "primary"
          }
        },
        {
          "key": "reviewDetails",
          "route": "reviewDetails",
          "component": "ReviewDetails",
          "nextStep": null,
          "isPreview": true,
          "withoutLabel": false,
          "type": "component",
          "hideInEmployee": false,
          "isMandatory": false,
          "sectionHeading": "EST_REVIEW_DETAILS",
          "form": [],
          "actionButton": {
            "text": {
              "create": "SUBMIT",
              "edit": "UPDATE"
            },
            "variant": "contained",
            "color": "primary"
          }
        }
      ]
    }
]

