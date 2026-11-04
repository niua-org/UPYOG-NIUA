
#queries for the TL module
def extract_tl_license_issued_by_boundary(metrics, region_bucket):
    val = region_bucket.get('todaysApplications')
    metrics['todaysApplications'] = val.get('value', 0) if val else 0
    return metrics

tl_license_issued_by_boundary = {'path': 'tlindex-v1/_search',
                                 'name': 'tl_license_issued_by_boundary',
                                 'lambda': extract_tl_license_issued_by_boundary,
                                 'query':
                                     """
    {{
      "size": 0,
        "query": {{
            "bool": {{
              "must_not": [
                {{
                  "term": {{
                    "Data.tradelicense.tenantId.keyword": "pb.testing"
                  }}
                }}
              ],
              "must": [
                {{
                  "range": {{
                    "Data.tradelicense.applicationDate": {{
                    "gte": {0},
                    "lte": {1},
                    "format": "epoch_millis"
          }}
        }}
                }}
              ]
            }}
          }},
        "aggs": {{   
          "ward": {{
                      "terms": {{
                        "field": "Data.ward.name.keyword",
                        "size":10000
                      }},
    
              "aggs": {{
                "ulb": {{
                  "terms": {{
                    "field": "Data.tradelicense.tenantId.keyword",
                    "size":10000
                  }},
                             "aggs": {{
             "region": {{
              "terms": {{
              "field": "Data.tenantData.city.districtName.keyword",
              "size":10000
              }},
    
                        "aggs": {{
                          "todaysApplications": {{
                            "value_count": {{
                              "field": "Data.tradelicense.applicationNumber.keyword"
                              }}
                          }}
                            }}
                          }}
                             }}
                           }}
                           }}
                           }}
                           }}
                           }}  
    """
                                 }


def extract_tl_collection_adhoc_penalty(metrics, region_bucket):
    penalty = region_bucket.get('adhocPenalty')
    val = penalty.get('aggrFilter', {}).get('amount', {}).get('value') if penalty else 0
    metrics['adhocPenalty'] = val if val else 0
    return metrics

tl_collection_adhoc_penalty = {
    'path': 'dss-collection_v2/_search',
    'name': 'tl_collection_adhoc_penalty',
    'lambda': extract_tl_collection_adhoc_penalty,
    'query': """
{{
  "size": 0,
  "query": {{
    "bool": {{
      "must_not": [
        {{
          "term": {{
            "dataObject.tenantId.keyword": "pb.testing"
          }}
        }}
      ],
          "must": [
            {{
                "range": {{
                    "dataObject.paymentDetails.receiptDate": {{
                    "gte": {0},
                    "lte": {1},
                    "format": "epoch_millis"
                }}
              }}
            }},
            {{
          "term": {{
            "dataObject.paymentDetails.businessService.keyword": {{
              "value": "TL"
            }}
          }}
        }}
          ]
    }}
    }},
  "aggs": {{
    "ward": {{
      "terms": {{
        "field": "domainObject.ward.name.keyword",
        "size":10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "dataObject.tenantId.keyword",
            "size":10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "dataObject.tenantData.city.districtName.keyword",
                "size":10000
              }},
              "aggs": {{
                "adhocPenalty": {{
                  "nested": {{
                    "path": "dataObject.paymentDetails.bill.billDetails.billAccountDetails"
                  }},
                  "aggs": {{
                    "aggrFilter": {{
                      "filter": {{
                        "terms": {{
                          "dataObject.paymentDetails.bill.billDetails.billAccountDetails.taxHeadCode.keyword": [
                            "TL_ADHOC_PENALTY"
                          ]
                        }}
                      }},
                      "aggs": {{
                        "amount" : {{
                          "sum": {{
                            "field": "dataObject.paymentDetails.bill.billDetails.billAccountDetails.amount"
                          }}
                        }}
                      }}
                    }}
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
  }}
}}

"""
}


def extract_tl_collection_adhoc_rebate(metrics, region_bucket):
    rebate = region_bucket.get('adhocRebate')
    val = rebate.get('aggrFilter', {}).get('amount', {}).get('value') if rebate else 0
    metrics['adhocRebate'] = val if val else 0
    return metrics


tl_collection_adhoc_rebate = {'path': 'dss-collection_v2/_search',
                              'name': 'tl_collection_adhoc_rebate',
                              'lambda': extract_tl_collection_adhoc_rebate,
                              'query': """
{{
  "size": 0,
  "query": {{
    "bool": {{
      "must_not": [
        {{
          "term": {{
            "dataObject.tenantId.keyword": "pb.testing"
          }}
        }}
      ],
          "must": [
            {{
                "range": {{
                    "dataObject.paymentDetails.receiptDate": {{
                "gte": {0},
                "lte": {1},
                    "format": "epoch_millis"
                }}
              }}
            }},
            {{
          "term": {{
            "dataObject.paymentDetails.businessService.keyword": {{
              "value": "TL"
            }}
          }}
        }}
          ]
    }}
    }},
  "aggs": {{
    "ward": {{
      "terms": {{
        "field": "domainObject.ward.name.keyword",
        "size":10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "dataObject.tenantId.keyword",
            "size":10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "dataObject.tenantData.city.districtName.keyword",
                "size":10000
              }},
              "aggs": {{
                "adhocRebate": {{
                  "nested": {{
                    "path": "dataObject.paymentDetails.bill.billDetails.billAccountDetails"
                  }},
                  "aggs": {{
                    "aggrFilter": {{
                      "filter": {{
                        "terms": {{
                          "dataObject.paymentDetails.bill.billDetails.billAccountDetails.taxHeadCode.keyword": [
                            "TL_ADHOC_REBATE"
                          ]
                        }}
                      }},
                      "aggs": {{
                        "amount" : {{
                          "sum": {{
                            "field": "dataObject.paymentDetails.bill.billDetails.billAccountDetails.amount"
                          }}
                        }}
                      }}
                    }}
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
  }}
}}



	
"""
                              }


def extract_tl_collection_tax(metrics, region_bucket):
    tax = region_bucket.get('tlTax')
    val = tax.get('aggrFilter', {}).get('amount', {}).get('value') if tax else 0
    metrics['tlTax'] = val if val else 0
    return metrics


tl_collection_tax = {'path': 'dss-collection_v2/_search',
                     'name': 'tl_collection_tax',
                     'lambda': extract_tl_collection_tax,

                     'query': """

{{
  "size": 0,
  "query": {{
    "bool": {{
      "must_not": [
        {{
          "term": {{
            "dataObject.tenantId.keyword": "pb.testing"
          }}
        }}
      ],
          "must": [
            {{
                "range": {{
                    "dataObject.paymentDetails.receiptDate": {{
                "gte": {0},
                "lte": {1},
                    "format": "epoch_millis"
                }}
              }}
            }},
            {{
          "term": {{
            "dataObject.paymentDetails.businessService.keyword": {{
              "value": "TL"
            }}
          }}
        }}
          ]
    }}
    }},
  "aggs": {{
    "ward": {{
      "terms": {{
        "field": "domainObject.ward.name.keyword",
        "size":10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "dataObject.tenantId.keyword",
            "size":10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "dataObject.tenantData.city.districtName.keyword",
                "size":10000
              }},
              "aggs": {{
                "tlTax": {{
                  "nested": {{
                    "path": "dataObject.paymentDetails.bill.billDetails.billAccountDetails"
                  }},
                  "aggs": {{
                    "aggrFilter": {{
                      "filter": {{
                        "terms": {{
                          "dataObject.paymentDetails.bill.billDetails.billAccountDetails.taxHeadCode.keyword": [
                            "TL_TAX"
                          ]
                        }}
                      }},
                      "aggs": {{
                        "amount" : {{
                          "sum": {{
                            "field": "dataObject.paymentDetails.bill.billDetails.billAccountDetails.amount"
                          }}
                        }}
                      }}
                    }}
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
  }}
}}

"""
                     }


def extract_tl_todays_trade_licenses(metrics, region_bucket):
    status_agg = region_bucket.get('Status')
    if not status_agg:
        return metrics
    status_buckets = status_agg.get('buckets', [])
    grouped_by = []
    for status_bucket in status_buckets:
        val_agg = status_bucket.get('todaysTradeLicenses')
        val = val_agg.get('value', 0) if val_agg else 0
        grouped_by.append({'name': status_bucket.get('key'), 'value': val})
    metrics['todaysTradeLicenses'] = [
        {'groupBy': 'status', 'buckets': grouped_by}]
    return metrics


tl_todays_trade_licenses = {'path': 'tlindex-v1/_search',
                            'name': 'tl_todays_trade_licenses',
                            'lambda': extract_tl_todays_trade_licenses,
                            'query': """
{{
  "size":0 ,
    "query": {{
        "bool": {{
          "must_not": [
            {{
              "term": {{
                "Data.tradelicense.tenantId.keyword": "pb.testing"
              }}
            }}
          ],
      "must": [
            {{
                "range": {{
                    "Data.tradelicense.issuedDate": {{
                    "gte": {0},
                    "lte": {1},
                    "format": "epoch_millis"
                }}
              }}
            }}
          ]
        }}
      }},
      "aggs": {{
                "ward": {{
                  "terms": {{
                    "field": "Data.ward.name.keyword",
                    "size":10000
                  }},
         "aggs": {{
            "ulb": {{
              "terms": {{
                "field": "Data.tradelicense.tenantId.keyword",
                "size":10000
              }},
        "aggs": {{
         "region": {{
          "terms": {{
          "field": "Data.tenantData.city.districtName.keyword",
          "size":10000
          }},
            "aggs": {{
                      "Status": {{
                        "terms": {{
                          "field": "Data.tradelicense.status.keyword"
                        }},
                        "aggs": {{
                          "todaysTradeLicenses": {{
                           "cardinality": {{
                             "field": "Data.tradelicense.licenseNumber.keyword"
                           }}
                          }}
                        }}
                      }}
                    }}
                 }}
              }}
            }}
          }}
         }}
         }}
         }}




"""
                            }


def extract_tl_total_transactions(metrics, region_bucket):
    metrics['transactions'] = region_bucket.get('transactions').get(
        'value') if region_bucket.get('transactions') else 0
    return metrics


tl_total_transactions = {'path': 'dss-collection_v2/_search',
                         'name': 'tl_total_transactions',
                         'lambda': extract_tl_total_transactions,
                         'query': """
{{
  "size":0,
  "query": {{	
        "bool": {{
          "must_not": [
            {{
              "term": {{
                "dataObject.tenantId.keyword": "pb.testing"
              }}
            }},
            {{
              "terms": {{
                "dataObject.paymentDetails.bill.status.keyword": [
                  "Cancelled"
                ]
              }}
            }}
          ],
        "must": [
            {{
                "range": {{
                    "dataObject.paymentDetails.receiptDate": {{
                    "gte": {0},
                    "lte": {1},
                    "format": "epoch_millis"
                }}
              }}
            }},
            {{
          "term": {{
            "dataObject.paymentDetails.businessService.keyword": {{
              "value": "TL"
            }}
          }}
        }}
          ]
        }}
        }},
         "aggs": {{
                "ward": {{
                  "terms": {{
                    "field": "domainObject.ward.name.keyword",
                    "size":10000
                  }},
                            "aggs": {{
            "ulb": {{
              "terms": {{
                "field": "dataObject.tenantId.keyword",
                "size":10000
              }},
        "aggs": {{
         "region": {{
            "terms": {{
              "field": "dataObject.tenantData.city.districtName.keyword",
              "size":10000
            }},
                  "aggs": {{
                    "transactions": {{
                      "value_count": {{
                        "field": "dataObject.transactionNumber.keyword"
                      }}
                    }}
      }}
    }}
  }}
}}
}}
}}
}}
}}




"""
                         }


def extract_applications_moved_today(metrics, region_bucket):
    status_agg = region_bucket.get('applicationsMovedToday')
    if not status_agg:
        return metrics
    status_buckets = status_agg.get('buckets', [])
    grouped_by = []
    for status_bucket in status_buckets:
        val_agg = status_bucket.get('applicationsMovedToday')
        val = val_agg.get('value', 0) if val_agg else 0
        grouped_by.append({'name': status_bucket.get('key'), 'value': val})
    metrics['applicationsMovedToday'] = [
        {'groupBy': 'status', 'buckets': grouped_by}]
    return metrics


tl_applications_moved_today = {'path': 'tlindex-v1/_search',
                               'name': 'tl_applications_moved_today',
                               'lambda': extract_applications_moved_today,
                               'query': """
{{
  "size":0,
   "query": {{
        "bool": {{
          "must_not": [
            {{
              "term": {{
                "Data.tradelicense.tenantId.keyword": "pb.testing"
              }}
            }}
          ],
      "must": [
            {{
                "range": {{
                    "Data.history.auditDetails.createdTime": {{
                    "gte": {0},
                    "lte": {1},
                    "format": "epoch_millis"
                }}
              }}
            }}
          ]
        }}
      }},
    "aggs": {{   
      "ward": {{
          "terms": {{
            "field": "Data.ward.name.keyword",
            "size":10000
          }},

          "aggs": {{
            "ulb": {{
              "terms": {{
                "field": "Data.tradelicense.tenantId.keyword",
                "size":10000
              }},
        "aggs": {{
         "region": {{
          "terms": {{
          "field": "Data.tenantData.city.districtName.keyword",
          "size":10000
          }},
          "aggs": {{
            "applicationsMovedToday": {{
              "terms": {{
                "field": "Data.history.state.applicationStatus.keyword"
              }},
              "aggs": {{
                "applicationsMovedToday": {{
                  "value_count": {{
                    "field": "Data.tradelicense.applicationNumber.keyword"
                  }}
                }}
              }}
            }}
          }}
}}
}}
}}
}}
}}
}}
}}


"""
                               }





def extract_collections_by_trade_category(metrics, region_bucket):
    tradeType_agg = region_bucket.get('tradeType')
    tradeType_buckets = tradeType_agg.get('buckets')
    grouped_by = []
    for tradeType_bucket in tradeType_buckets:
        grouped_by.append({'name': tradeType_bucket.get('key'), 'value': tradeType_bucket.get(
            'todaysCollection').get('value') if tradeType_bucket.get('todaysCollection') else 0})
    metrics['todaysCollection'] = [{'groupBy': 'tradeType', 'buckets': grouped_by}]
    return metrics


tl_collections_by_trade_category = {'path': 'dss-collection_v2/_search',
                                    'name': 'tl_collections_by_trade_category',
                                    'lambda': extract_collections_by_trade_category,
                                    'query': """
{{
  "size": 0,
    "query": {{
        "bool": {{
          "must_not": [
            {{
              "term": {{
                "dataObject.tenantId.keyword": "pb.testing"
              }}
            }},
            {{
              "terms": {{
                "dataObject.paymentDetails.bill.status.keyword": [
                  "Cancelled"
                ]
              }}
            }}
          ],
          "must": [
            {{
              "term": {{
                "dataObject.paymentDetails.businessService.keyword": "TL"
              }}
            }},
            {{
              "range": {{
                      "dataObject.paymentDetails.receiptDate": {{
                      "gte": {0},
                      "lte": {1},
                      "format": "epoch_millis"
                }}
              }}

            }}
          ]
        }}
      }},
         "aggs": {{
         "region_buckets": {{
            "terms": {{
              "field": "dataObject.tenantData.city.districtCode.keyword",
              "size":10000
            }},
          "aggs": {{
            "ulb_buckets": {{
              "terms": {{
                "field": "dataObject.tenantId.keyword",
                "size":10000
              }},
              "aggs": {{
                "ward_buckets": {{
                  "terms": {{
                    "field": "domainObject.ward.name.keyword",
                    "size":10000
                  }},
                "aggs": {{
                    "byTradeType": {{
                      "terms": {{
                       "field": "domainObject.tradelicense.calculation.tradeLicense.tradeLicenseDetail.tradeUnits.tradeType.keyword",
                        "size":1
                      }},
                  "aggs": {{
                    "Total Collection": {{
                      "sum": {{
                        "field": "dataObject.paymentDetails.totalAmountPaid"
                      }}
                    }}
                  }}
    }}
  }}
}}	
}}
}}
}}
}}
}}
}}


"""
                                    }


def extract_todays_license_by_sla(metrics, region_bucket):
    val = region_bucket.get('todaysLicenseIssuedWithinSLA')
    metrics['todaysLicenseIssuedWithinSLA'] = val.get('doc_count', 0) if val else 0
    return metrics


tl_license_issued_within_sla = {'path': 'tlindex-v1/_search',
                                'name': 'tl_license_issued_within_sla',
                                'lambda': extract_todays_license_by_sla,
                                'query': """
{{
  "size": 0, 
  "query" :{{
        "bool": {{
          "must_not": [
            {{
              "term": {{
                "Data.tradelicense.tenantId.keyword": "pb.testing"
              }}
            }}
          ],
          "must": [
            {{
                "range": {{
                "Data.tradelicense.@timestamp": {{
                "gte": {0},
                "lte": {1},
                "format": "epoch_millis"
              }}
            }}
            }}
          ]
        }}
      }},
     "aggs": {{
          "ward": {{
            "terms": {{
              "field": "Data.ward.name.keyword",
              "size":10000
            }},
   "aggs": {{
      "ulb": {{
        "terms": {{
          "field": "Data.tradelicense.tenantId.keyword",
          "size":10000
        }},
  "aggs": {{
   "region": {{
    "terms": {{
    "field": "Data.tenantData.city.districtName.keyword",
    "size":10000
    }},
      "aggs": {{
            "todaysLicenseIssuedWithinSLA": {{
              "filter": {{
                "bool": {{
                  "must": [
                    {{
                      "script": {{
                        "script": {{
                          "source": "doc['Data.tradelicense.auditDetails.lastModifiedTime'].value-doc['Data.tradelicense.auditDetails.createdTime'].value<params.threshold",
                          "lang": "painless",
                          "params": {{
                            "threshold": 172800000
                          }}
                        }}
                      }}
                    }}
                  ]
                }}
              }}
            }}
          }}   
    }}
    }}
    }}
    }}
    }}
    }}
    }}



"""
                                }


def extract_tl_todays_collection_by_trade_type(metrics, region_bucket):
    tt_agg = region_bucket.get('tradeType')
    if not tt_agg:
        return metrics
    tt_buckets = tt_agg.get('buckets', [])
    grouped_by = []
    for tt_bucket in tt_buckets:
        coll_agg = tt_bucket.get('todaysCollection')
        val = coll_agg.get('value', 0) if coll_agg else 0
        grouped_by.append({'name': tt_bucket.get('key'), 'value': val})
    metrics['todaysCollection'] = [
        {'groupBy': 'tradeType', 'buckets': grouped_by}]
    return metrics


tl_todays_collection_by_trade_type = {'path': 'dss-collection_v2/_search',
                                      'name': 'tl_todays_collection_by_trade_type',
                                      'lambda': extract_tl_todays_collection_by_trade_type,
                                      'query':
                                          """
         {{
           "size": 0,
           "query": {{
             "bool": {{
               "must_not": [
                 {{
                   "term": {{
                     "dataObject.tenantId.keyword": "pb.testing"
                   }}
                 }},
                 {{
                   "terms": {{
                     "dataObject.paymentDetails.bill.status.keyword": [
                       "Cancelled"
                     ]
                   }}
                 }}
               ],
               "must": [
                 {{
                   "term": {{
                     "dataObject.paymentDetails.businessService.keyword": "TL"
                   }}
                 }},
                 {{
                   "range": {{
                     "dataObject.paymentDetails.receiptDate": {{
                       "gte": {0},
                       "lte": {1},
                       "format": "epoch_millis"
                     }}
                   }}
                 }}
               ],
               "filter": {{
                 "exists": {{
                   "field": "domainObject.tradelicense.tradeLicenseDetail.tradeUnits.tradeType"
                 }}
               }}
             }}
           }},
           "aggs": {{
             "ward": {{
               "terms": {{
                 "field": "domainObject.ward.name.keyword",
                 "size":10000
               }},
               "aggs": {{
                 "ulb": {{
                   "terms": {{
                     "field": "dataObject.tenantId.keyword",
                     "size":10000
                   }},
                   "aggs": {{
                     "region": {{
                       "terms": {{
                         "field": "dataObject.tenantData.city.districtName.keyword",
                         "size":10000
                       }},
                       "aggs": {{
                         "tradeType": {{
                           "terms": {{
                             "field": "domainObject.tradelicense.tradeLicenseDetail.tradeUnits.tradeType.keyword"
                           }},
                           "aggs": {{
                             "todaysCollection": {{
                               "sum": {{
                                 "field": "dataObject.paymentDetails.totalAmountPaid"
                               }}
                             }}
                           }}
                         }}
                       }}
                     }}
                   }}
                 }}
               }}
             }}
           }}
         }}
         
         """
                                      }



def extract_tl_todays_approved_applications(metrics, region_bucket):
    val = region_bucket.get('todaysApprovedApplications')
    metrics['todaysApprovedApplications'] = val.get('value', 0) if val else 0
    return metrics


tl_todays_approved_applications = {
    'path': 'tlindex-v1/_search',
    'name': 'tl_todays_approved_applications',
    'lambda': extract_tl_todays_approved_applications,
    'query': """
{{
  "size": 0,
  "query": {{
    "bool": {{
      "must_not": [
        {{
          "term": {{
            "Data.tradelicense.tenantId.keyword": "pb.testing"
          }}
        }}
      ],
      "must": [
        {{
          "term": {{
            "Data.tradelicense.status.keyword": "APPROVED"
          }}
        }},
        {{
          "range": {{
            "Data.tradelicense.auditDetails.lastModifiedTime": {{
              "gte": {0},
              "lte": {1},
              "format": "epoch_millis"
            }}
          }}
        }}
      ]
    }}
  }},
  "aggs": {{
    "ward": {{
      "terms": {{
        "field": "Data.ward.name.keyword",
        "size": 10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "Data.tradelicense.tenantId.keyword",
            "size": 10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "Data.tenantData.city.districtName.keyword",
                "size": 10000
              }},
              "aggs": {{
                "todaysApprovedApplications": {{
                  "cardinality": {{
                    "field": "Data.tradelicense.applicationNumber.keyword"
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
  }}
}}
"""
}


def extract_tl_todays_approved_applications_within_sla(metrics, region_bucket):
    val = region_bucket.get('todaysApprovedApplicationsWithinSLA')
    metrics['todaysApprovedApplicationsWithinSLA'] = val.get('value', 0) if val else 0
    return metrics


tl_todays_approved_applications_within_sla = {
    'path': 'tlindex-v1/_search',
    'name': 'tl_todays_approved_applications_within_sla',
    'lambda': extract_tl_todays_approved_applications_within_sla,
    'query': """
{{
  "size": 0,
  "query": {{
    "bool": {{
      "must_not": [
        {{
          "term": {{
            "Data.tradelicense.tenantId.keyword": "pb.testing"
          }}
        }}
      ],
      "must": [
        {{
          "term": {{
            "Data.tradelicense.status.keyword": "APPROVED"
          }}
        }},
        {{
          "range": {{
            "Data.tradelicense.auditDetails.lastModifiedTime": {{
              "gte": {0},
              "lte": {1},
              "format": "epoch_millis"
            }}
          }}
        }},
        {{
          "script": {{
            "script": {{
              "source": "doc['Data.tradelicense.auditDetails.lastModifiedTime'].value - doc['Data.tradelicense.auditDetails.createdTime'].value < params.threshold",
              "lang": "painless",
              "params": {{
                "threshold": 172800000
              }}
            }}
          }}
        }}
      ]
    }}
  }},
  "aggs": {{
    "ward": {{
      "terms": {{
        "field": "Data.ward.name.keyword",
        "size": 10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "Data.tradelicense.tenantId.keyword",
            "size": 10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "Data.tenantData.city.districtName.keyword",
                "size": 10000
              }},
              "aggs": {{
                "todaysApprovedApplicationsWithinSLA": {{
                  "cardinality": {{
                    "field": "Data.tradelicense.applicationNumber.keyword"
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
  }}
}}
"""
}


def extract_tl_pending_applications_beyond_timeline(metrics, region_bucket):
    val = region_bucket.get('pendingApplicationsBeyondTimeline')
    metrics['pendingApplicationsBeyondTimeline'] = val.get('value', 0) if val else 0
    return metrics


tl_pending_applications_beyond_timeline = {
    'path': 'tlindex-v1/_search',
    'name': 'tl_pending_applications_beyond_timeline',
    'lambda': extract_tl_pending_applications_beyond_timeline,
    'query': """
{{
  "size": 0,
  "query": {{
    "bool": {{
      "must_not": [
        {{
          "term": {{
            "Data.tradelicense.tenantId.keyword": "pb.testing"
          }}
        }},
        {{
          "terms": {{
            "Data.tradelicense.status.keyword": [
              "APPROVED",
              "REJECTED",
              "CANCELLED"
            ]
          }}
        }}
      ],
      "must": [
        {{
          "script": {{
            "script": {{
              "source": "!doc['Data.tradelicense.auditDetails.createdTime'].empty && ({1}L - doc['Data.tradelicense.auditDetails.createdTime'].value) > params.threshold",
              "lang": "painless",
              "params": {{
                "threshold": 172800000
              }}
            }}
          }}
        }}
      ]
    }}
  }},
  "aggs": {{
    "ward": {{
      "terms": {{
        "field": "Data.ward.name.keyword",
        "size": 10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "Data.tradelicense.tenantId.keyword",
            "size": 10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "Data.tenantData.city.districtName.keyword",
                "size": 10000
              }},
              "aggs": {{
                "pendingApplicationsBeyondTimeline": {{
                  "cardinality": {{
                    "field": "Data.tradelicense.applicationNumber.keyword"
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
  }}
}}
"""
}


def extract_tl_avg_days_for_application_approval(metrics, region_bucket):
    val = region_bucket.get('avgDaysForApplicationApproval', {}).get('value') if region_bucket.get('avgDaysForApplicationApproval') else None
    metrics['avgDaysForApplicationApproval'] = round(val, 2) if val is not None else 0
    return metrics


tl_avg_days_for_application_approval = {
    'path': 'tlindex-v1/_search',
    'name': 'tl_avg_days_for_application_approval',
    'lambda': extract_tl_avg_days_for_application_approval,
    'query': """
{{
  "size": 0,
  "query": {{
    "bool": {{
      "must_not": [
        {{
          "term": {{
            "Data.tradelicense.tenantId.keyword": "pb.testing"
          }}
        }}
      ],
      "must": [
        {{
          "term": {{
            "Data.tradelicense.status.keyword": "APPROVED"
          }}
        }},
        {{
          "range": {{
            "Data.tradelicense.auditDetails.lastModifiedTime": {{
              "gte": {0},
              "lte": {1},
              "format": "epoch_millis"
            }}
          }}
        }}
      ]
    }}
  }},
  "aggs": {{
    "ward": {{
      "terms": {{
        "field": "Data.ward.name.keyword",
        "size": 10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "Data.tradelicense.tenantId.keyword",
            "size": 10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "Data.tenantData.city.districtName.keyword",
                "size": 10000
              }},
              "aggs": {{
                "avgDaysForApplicationApproval": {{
                  "avg": {{
                    "script": {{
                      "source": "!doc['Data.tradelicense.auditDetails.lastModifiedTime'].empty && !doc['Data.tradelicense.auditDetails.createdTime'].empty ? (doc['Data.tradelicense.auditDetails.lastModifiedTime'].value - doc['Data.tradelicense.auditDetails.createdTime'].value) / 86400000.0 : 0",
                      "lang": "painless"
                    }}
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
  }}
}}
"""
}


def extract_tl_stipulated_days(metrics, region_bucket):
    val = region_bucket.get('stipulatedDays', {}).get('value') if region_bucket.get('stipulatedDays') else None
    metrics['StipulatedDays'] = int(round(val)) if val is not None else 7
    return metrics


tl_stipulated_days = {
    'path': 'tlindex-v1/_search',
    'name': 'tl_stipulated_days',
    'lambda': extract_tl_stipulated_days,
    'query': """
{{
  "size": 0,
  "query": {{
    "bool": {{
      "must_not": [
        {{
          "term": {{
            "Data.tradelicense.tenantId.keyword": "pb.testing"
          }}
        }}
      ]
    }}
  }},
  "aggs": {{
    "ward": {{
      "terms": {{
        "field": "Data.ward.name.keyword",
        "size": 10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "Data.tradelicense.tenantId.keyword",
            "size": 10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "Data.tenantData.city.districtName.keyword",
                "size": 10000
              }},
              "aggs": {{
                "stipulatedDays": {{
                  "avg": {{
                    "script": {{
                      "source": "doc.containsKey('Data.history.businesssServiceSla') && !doc['Data.history.businesssServiceSla'].empty ? doc['Data.history.businesssServiceSla'].value / 86400000.0 : 7",
                      "lang": "painless"
                    }}
                  }}
                }}
              }}
            }}
          }}
        }}
      }}
    }}
  }}
}}
"""
}


tl_queries = [
    tl_license_issued_by_boundary,
    tl_collection_adhoc_penalty,
    tl_collection_adhoc_rebate,
    tl_collection_tax,
    tl_todays_trade_licenses,
    tl_total_transactions,
    tl_applications_moved_today,
    tl_license_issued_within_sla,
    tl_todays_collection_by_trade_type,
    tl_todays_approved_applications,
    tl_todays_approved_applications_within_sla,
    tl_pending_applications_beyond_timeline,
    tl_avg_days_for_application_approval,
    tl_stipulated_days,
]

#the default payload for TL
def empty_tl_payload(region, ulb, ward, date):
    return {
        "date": date,
        "module": "TL",
        "ward": ward,
        "ulb": ulb,
        "region": region,
        "state": "Punjab",
        "metrics": {
            "transactions": 0,
            "todaysApplications": 0,
            "tlTax": 0,
            "adhocPenalty": 0,
            "adhocRebate": 0,
            "todaysLicenseIssuedWithinSLA": 0,
            "todaysApprovedApplications": 0,
            "pendingApplicationsBeyondTimeline": 0,
            "todaysApprovedApplicationsWithinSLA": 0,
            "avgDaysForApplicationApproval": 0,
            "StipulatedDays": 0,
            "todaysCollection": [
                {
                    "groupBy": "tradeType",
                    "buckets": [
                      {
                        "name": "BRICKFIELD",
                        "value": 0
                      },
                      {
                        "name": "GROCERYSTORES",
                        "value": 0
                      },
                      {
                        "name": "CHARCOAL_KLIN",
                        "value": 0
                      }
                    ]
                },
                {
                    "groupBy": "paymentChannelType",
                    "buckets": [
                      {
                      "name": "Digital",
                      "value": 0
                      },
                      {
                      "name": "Non Digital",
                      "value": 0
                      }
                    ]
                }
            ],
            "todaysTradeLicenses": [
                {
                    "groupBy": "status",
                    "buckets": [
                        {
                          "name": "INITIATED",
                          "value": 0
                        },
                        {
                          "name": "APPLIED",
                          "value": 0
                        },
                        {
                        "name": "FIELDINSPECTION",
                        "value": 0
                        },
                        {
                        "name": "PENDINGAPPROVAL",
                        "value": 0
                        },
                        {
                        "name": "PENDINGPAYMENT",
                        "value": 0
                        },
                        {
                        "name": "APPROVED",
                        "value": 0
                        },
                        {
                        "name": "REJECTED",
                        "value": 0
                        },
                        {
                        "name": "CANCELLED",
                        "value": 0
                        },
                        {
                        "name": "CITIZENACTIONREQUIRED",
                        "value": 0
                        }

                    ]
                }
            ],
            "applicationsMovedToday": [
                {
                    "groupBy": "status",
                    "buckets": [
                        {
                        "name": "INITIATED",
                        "value": 0
                        },
                        {
                        "name": "APPLIED",
                        "value": 0
                        },
                        {
                        "name": "FIELDINSPECTION",
                        "value": 0
                        },
                        {
                        "name": "PENDINGAPPROVAL",
                        "value": 0
                        },
                        {
                        "name": "PENDINGPAYMENT",
                        "value": 0
                        },
                        {
                        "name": "APPROVED",
                        "value": 0
                        },
                        {
                        "name": "REJECTED",
                        "value": 0
                        },
                        {
                        "name": "CANCELLED",
                        "value": 0
                        },
                        {
                        "name": "CITIZENACTIONREQUIRED",
                        "value": 0
                        }
                    ]
                }
            ]
        }
    }