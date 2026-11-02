"""Updated WS Elasticsearch query definitions for current Punjab indices.

All date arguments are numeric epoch milliseconds. Query strings preserve the legacy .format(from_epoch_ms, to_epoch_ms) contract; use start-inclusive and next-day-start-exclusive values. Do not pass now/d to mapped long fields.

Mapping limits: receipt index has no ward/usage-type field; pending status allowlist and pending age bands need business rules; current status + lastModifiedTime cannot prove closure today; completion-within-SLA and SLA compliance need a completion event/SLA outcome not represented here. These extractors return empty/zero for unsupported values. Tax-head amount grouping uses non-nested arrays and can cross-associate values."""
import logging
import json

def _format_template(body):
    text = json.dumps(body, indent=2)
    text = text.replace('"__FROM_MS__"', '{0}').replace('"__TO_MS__"', '{1}')
    return text.replace('{', '{{').replace('}', '}}').replace('{{0}}', '{0}').replace('{{1}}', '{1}')

def extract_ws_collection_by_payment_channel_type(metrics, region_bucket):
    groupby_usage = []
    groupby_channel = []
    collection = []

    if region_bucket.get('byChannel'):
        channel_buckets = region_bucket.get('byChannel').get('buckets')
        for channel_bucket in channel_buckets:
            channel = channel_bucket.get('key')
            value = channel_bucket.get('byChannel').get('value') if channel_bucket.get('byChannel') else 0
            groupby_channel.append({ 'name' : channel, 'value' : value})

    if region_bucket.get('byUsageType'):
        usage_type_buckets = region_bucket.get('byUsageType').get('buckets')
        for usage_type_bucket in usage_type_buckets:
            usage_type = usage_type_bucket.get('key')
            value = usage_type_bucket.get('byUsageType').get('value') if usage_type_bucket.get('byUsageType') else 0
            groupby_usage.append({ 'name' : usage_type, 'value' : value})


    collection.append({ 'groupBy': 'usageType', 'buckets' : groupby_usage})
    collection.append({ 'groupBy': 'paymentChannelType', 'buckets' : groupby_channel})
    metrics['todaysCollection'] = collection


    return metrics

def extract_ws_collection_by_tax_head_connection_type(metrics, region_bucket):
    collection = metrics.get('todaysCollection') or []
    buckets = []
    by_head = region_bucket.get('byTaxHead') or {}
    for bucket in by_head.get('buckets', []):
        total = (bucket.get('amount') or {}).get('value', 0)
        buckets.append({'name': bucket.get('key'), 'value': total})
    collection.append({'groupBy': 'taxHeads', 'buckets': buckets})
    metrics['todaysCollection'] = collection
    return metrics

def extract_ws_pending_connections(metrics, region_bucket):
    # Pending age bands need a business-approved definition of pending status and SLA clock.
    metrics['pendingConnections'] = []
    return metrics

def extract_ws_sewerage_connections(metrics, region_bucket):
    groupby_usage = []
    groupby_channel = []
    collection =  []

    if region_bucket.get('sewerageConnectionsbyChannelType'):
        channel_buckets = region_bucket.get('sewerageConnectionsbyChannelType').get('buckets')
        for channel_bucket in channel_buckets:
            channel = channel_bucket.get('key')
            value = channel_bucket.get('sewerageConnectionsbyChannelType').get('value') if channel_bucket.get('sewerageConnectionsbyChannelType') else 0
            groupby_channel.append({ 'name' : channel, 'value' : value})

    if region_bucket.get('sewerageConnectionsbyUsageType'):
        usage_type_buckets = region_bucket.get('sewerageConnectionsbyUsageType').get('buckets')
        for usage_type_bucket in usage_type_buckets:
            usage_type = usage_type_bucket.get('key')
            value = usage_type_bucket.get('sewerageConnectionsbyUsageType').get('value') if usage_type_bucket.get('sewerageConnectionsbyUsageType') else 0
            groupby_usage.append({ 'name' : usage_type, 'value' : value})


    collection.append({ 'groupBy': 'usageType', 'buckets' : groupby_usage})
    collection.append({ 'groupBy': 'channelType', 'buckets' : groupby_channel})
    metrics['sewerageConnections'] = collection


    return metrics

def extract_ws_water_connections(metrics, region_bucket):
    groupby_usage = []
    groupby_channel = []
    groupby_meter = []
    collection =  []

    if region_bucket.get('waterConnectionsbyChannelType'):
        channel_buckets = region_bucket.get('waterConnectionsbyChannelType').get('buckets')
        for channel_bucket in channel_buckets:
            channel = channel_bucket.get('key')
            value = channel_bucket.get('waterConnectionsbyChannelType').get('value') if channel_bucket.get('waterConnectionsbyChannelType') else 0
            groupby_channel.append({ 'name' : channel, 'value' : value})

    if region_bucket.get('waterConnectionsbyUsageType'):
        usage_type_buckets = region_bucket.get('waterConnectionsbyUsageType').get('buckets')
        for usage_type_bucket in usage_type_buckets:
            usage_type = usage_type_bucket.get('key')
            value = usage_type_bucket.get('waterConnectionsbyUsageType').get('value') if usage_type_bucket.get('waterConnectionsbyUsageType') else 0
            groupby_usage.append({ 'name' : usage_type, 'value' : value})

    if region_bucket.get('waterConnectionsbyMeterType'):
        meter_type_buckets = region_bucket.get('waterConnectionsbyMeterType').get('buckets')
        for meter_type_bucket in meter_type_buckets:
            meter_type = meter_type_bucket.get('key')
            value = meter_type_bucket.get('waterConnectionsbyMeterType').get('value') if meter_type_bucket.get('waterConnectionsbyMeterType') else 0
            groupby_meter.append({ 'name' : meter_type, 'value' : value})


    collection.append({ 'groupBy': 'usageType', 'buckets' : groupby_usage})
    collection.append({ 'groupBy': 'channelType', 'buckets' : groupby_channel})
    collection.append({ 'groupBy': 'meterType', 'buckets' : groupby_meter})
    metrics['waterConnections'] = collection


    return metrics

def extract_ws_todays_applications(metrics, region_bucket):
    metrics['todaysTotalApplications'] = region_bucket.get('todaysTotalApplications').get(
        'value') if region_bucket.get('todaysTotalApplications') else 0
    return metrics

def extract_ws_closed_applications(metrics, region_bucket):
    # Current status + last-modified time cannot prove a closure transition occurred today.
    metrics['todaysClosedApplications'] = 0
    return metrics

def extract_ws_connections_created_by_connection_type(metrics, region_bucket):
    groupby_connection_type = []
    collection = metrics.get('connectionsCreated') if metrics.get('connectionsCreated') else []

    if region_bucket.get('meteredconnectionCreated'):
        created_buckets = region_bucket.get('meteredconnectionCreated').get('meteredconnectionCreated')
        if created_buckets:
            groupby_connection_type.append({'name' : 'WATER.METERED', 'value' : created_buckets.get('value') if created_buckets else 0})

    if region_bucket.get('sewerageconnectionCreated'):
        created_buckets = region_bucket.get('sewerageconnectionCreated').get('sewerageconnectionCreated')
        if created_buckets:
            groupby_connection_type.append({'name' : 'SEWERAGE', 'value' : created_buckets.get('value') if created_buckets else 0})

    if region_bucket.get('non-meteredconnectionCreated'):
        created_buckets = region_bucket.get('non-meteredconnectionCreated').get('non-meteredconnectionCreated')
        if created_buckets:
            groupby_connection_type.append({'name' : 'WATER.NONMETERED', 'value' : created_buckets.get('value') if created_buckets else 0})

    collection.append({ 'groupBy': 'connectionType', 'buckets' : groupby_connection_type})
    metrics['connectionsCreated'] = collection

    return metrics

def extract_ws_connections_created_by_channel_type(metrics, region_bucket):
    groupby_channel = []
    collection = metrics.get('connectionsCreated') if metrics.get('connectionsCreated') else []

    if region_bucket.get('channelType'):
        channel_buckets = region_bucket.get('channelType').get('buckets')
        for channel_bucket in channel_buckets:
            channel = channel_bucket.get('key')
            value = channel_bucket.get('count').get('value') if channel_bucket.get('count') else 0
            groupby_channel.append({ 'name' : channel, 'value' : value})


    collection.append({ 'groupBy': 'channelType', 'buckets' : groupby_channel})
    metrics['connectionsCreated'] = collection

    return metrics

def extract_ws_total_transactions(metrics, region_bucket):
    metrics['transactions'] = region_bucket.get('transactions').get(
        'value') if region_bucket.get('transactions') else 0
    return metrics

def extract_ws_todays_completed_applications_withinSLA(metrics, region_bucket):
    # No normalized completion timestamp/SLA outcome is present in these mappings.
    metrics['todaysCompletedApplicationsWithinSLA'] = 0
    return metrics

def extract_ws_todays_sla_compliance(metrics, region_bucket):
    # Mapping alone does not define an SLA-compliance numerator and denominator.
    metrics['slaCompliance'] = 0
    return metrics

def empty_ws_payload(region, ulb, ward, date):
    return {
        "date": date,
        "module": "WS",
        "ward": ward,
        "ulb": ulb,
        "region": region,
        "state": "Punjab",
        "metrics": {
            "transactions": 0,
            "connectionsCreated": [

            ],
            "todaysCollection": [

            ],
            "sewerageConnections": [

            ],
            "waterConnections": [

            ],
            "pendingConnections": [

            ],
            "slaCompliance": 0,
            "todaysTotalApplications": 0,
            "todaysClosedApplications": 0,
            "todaysCompletedApplicationsWithinSLA": 0,
            "pendingApplicationsBeyondTimeline": 0,
            "avgDaysForApplicationApproval": 0,
            "StipulatedDays": 0
        }
    }

ws_collection_by_payment_channel_type = {
    'path': 'dss-collection_v2/_search',
    'name': 'ws_collection_by_payment_channel_type',
    'lambda': extract_ws_collection_by_payment_channel_type,
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
          "terms": {{
            "dataObject.paymentDetails.businessService.keyword": [
              "WS",
              "WS.ONE_TIME_FEE",
              "SW.ONE_TIME_FEE",
              "SW"
            ]
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
    "ward": {{
      "terms": {{
        "field": "domainObject.ward.name.keyword",
        "size": 10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "dataObject.tenantId.keyword",
            "size": 10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "dataObject.tenantData.city.districtName.keyword",
                "size": 10000
              }},
              "aggs": {{
                "byChannel": {{
                  "terms": {{
                    "field": "dataObject.paymentMode.keyword",
                    "size": 1000
                  }},
                  "aggs": {{
                    "byChannel": {{
                      "sum": {{
                        "field": "dataObject.paymentDetails.totalAmountPaid"
                      }}
                    }}
                  }}
                }},
                "byUsageType": {{
                  "terms": {{
                    "field": "domainObject.propertyUsageType.keyword",
                    "size": 1000
                  }},
                  "aggs": {{
                    "byUsageType": {{
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

ws_collection_by_tax_head_connection_type = {
    'path': 'dss-collection_v2/_search',
    'name': 'ws_collection_by_tax_head_connection_type',
    'lambda': extract_ws_collection_by_tax_head_connection_type,
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
          "terms": {{
            "dataObject.paymentDetails.businessService.keyword": [
              "WS",
              "WS.ONE_TIME_FEE",
              "SW.ONE_TIME_FEE",
              "SW"
            ]
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
    "ward": {{
      "terms": {{
        "field": "domainObject.ward.name.keyword",
        "size": 10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "dataObject.tenantId.keyword",
            "size": 10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "dataObject.tenantData.city.districtName.keyword",
                "size": 10000
              }},
              "aggs": {{
                "byTaxHead": {{
                  "terms": {{
                    "field": "dataObject.paymentDetails.bill.billDetails.billAccountDetails.taxHeadCode.keyword",
                    "size": 1000
                  }},
                  "aggs": {{
                    "amount": {{
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
"""
}

ws_pending_connections = {'path': 'water-services,sewerage-services/_search', 'name': 'ws_pending_connections', 'lambda': extract_ws_pending_connections, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": []\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "applicationStatusBuckets": {{\n                  "terms": {{\n                    "field": "Data.applicationStatus.keyword",\n                    "size": 1000\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

ws_sewerage_connections = {'path': 'sewerage-services/_search', 'name': 'ws_sewerage_connections', 'lambda': extract_ws_sewerage_connections, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": [\n        {{\n          "range": {{\n            "Data.auditDetails.createdTime": {{\n              "gte": {0},\n              "lt": {1}\n            }}\n          }}\n        }}\n      ]\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "sewerageConnectionsbyChannelType": {{\n                  "terms": {{\n                    "field": "Data.channel.keyword",\n                    "size": 1000\n                  }},\n                  "aggs": {{\n                    "sewerageConnectionsbyChannelType": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }},\n                "sewerageConnectionsbyUsageType": {{\n                  "terms": {{\n                    "field": "Data.propertyUsageType.keyword",\n                    "size": 1000\n                  }},\n                  "aggs": {{\n                    "sewerageConnectionsbyUsageType": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

ws_water_connections = {'path': 'water-services/_search', 'name': 'ws_water_connections', 'lambda': extract_ws_water_connections, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": [\n        {{\n          "range": {{\n            "Data.auditDetails.createdTime": {{\n              "gte": {0},\n              "lt": {1}\n            }}\n          }}\n        }}\n      ]\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "waterConnectionsbyChannelType": {{\n                  "terms": {{\n                    "field": "Data.channel.keyword",\n                    "size": 1000\n                  }},\n                  "aggs": {{\n                    "waterConnectionsbyChannelType": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }},\n                "waterConnectionsbyUsageType": {{\n                  "terms": {{\n                    "field": "Data.propertyUsageType.keyword",\n                    "size": 1000\n                  }},\n                  "aggs": {{\n                    "waterConnectionsbyUsageType": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }},\n                "waterConnectionsbyMeterType": {{\n                  "terms": {{\n                    "field": "Data.connectionType.keyword",\n                    "size": 1000\n                  }},\n                  "aggs": {{\n                    "waterConnectionsbyMeterType": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

ws_todays_applications = {'path': 'water-services,sewerage-services/_search', 'name': 'ws_todays_applications', 'lambda': extract_ws_todays_applications, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": [\n        {{\n          "range": {{\n            "Data.auditDetails.createdTime": {{\n              "gte": {0},\n              "lt": {1}\n            }}\n          }}\n        }}\n      ]\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "todaysTotalApplications": {{\n                  "cardinality": {{\n                    "field": "Data.applicationNo.keyword"\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

ws_closed_applications = {'path': 'water-services,sewerage-services/_search', 'name': 'ws_closed_applications', 'lambda': extract_ws_closed_applications, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": [\n        {{\n          "range": {{\n            "Data.auditDetails.lastModifiedTime": {{\n              "gte": {0},\n              "lt": {1}\n            }}\n          }}\n        }}\n      ]\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "status_distribution": {{\n                  "terms": {{\n                    "field": "Data.applicationStatus.keyword",\n                    "size": 1000\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

ws_connections_created_by_connection_type = {'path': 'water-services,sewerage-services/_search', 'name': 'ws_connections_created_by_connection_type', 'lambda': extract_ws_connections_created_by_connection_type, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": [\n        {{\n          "range": {{\n            "Data.auditDetails.createdTime": {{\n              "gte": {0},\n              "lt": {1}\n            }}\n          }}\n        }}\n      ]\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "meteredconnectionCreated": {{\n                  "filter": {{\n                    "term": {{\n                      "Data.connectionType.keyword": "WATER.METERED"\n                    }}\n                  }},\n                  "aggs": {{\n                    "meteredconnectionCreated": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }},\n                "sewerageconnectionCreated": {{\n                  "filter": {{\n                    "term": {{\n                      "Data.connectionType.keyword": "SEWERAGE"\n                    }}\n                  }},\n                  "aggs": {{\n                    "sewerageconnectionCreated": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }},\n                "non-meteredconnectionCreated": {{\n                  "filter": {{\n                    "term": {{\n                      "Data.connectionType.keyword": "WATER.NONMETERED"\n                    }}\n                  }},\n                  "aggs": {{\n                    "non-meteredconnectionCreated": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

ws_connections_created_by_channel_type = {'path': 'water-services,sewerage-services/_search', 'name': 'ws_connections_created_by_channel_type', 'lambda': extract_ws_connections_created_by_channel_type, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": [\n        {{\n          "range": {{\n            "Data.auditDetails.createdTime": {{\n              "gte": {0},\n              "lt": {1}\n            }}\n          }}\n        }}\n      ]\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "channelType": {{\n                  "terms": {{\n                    "field": "Data.channel.keyword",\n                    "size": 1000\n                  }},\n                  "aggs": {{\n                    "count": {{\n                      "value_count": {{\n                        "field": "Data.applicationNo.keyword"\n                      }}\n                    }}\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

ws_total_transactions = {
    'path': 'dss-collection_v2/_search',
    'name': 'ws_total_transactions',
    'lambda': extract_ws_total_transactions,
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
          "terms": {{
            "dataObject.paymentDetails.businessService.keyword": [
              "WS",
              "WS.ONE_TIME_FEE",
              "SW.ONE_TIME_FEE",
              "SW"
            ]
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
    "ward": {{
      "terms": {{
        "field": "domainObject.ward.name.keyword",
        "size": 10000
      }},
      "aggs": {{
        "ulb": {{
          "terms": {{
            "field": "dataObject.tenantId.keyword",
            "size": 10000
          }},
          "aggs": {{
            "region": {{
              "terms": {{
                "field": "dataObject.tenantData.city.districtName.keyword",
                "size": 10000
              }},
              "aggs": {{
                "transactions": {{
                  "cardinality": {{
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

ws_todays_completed_application_withinSLA = {'path': 'water-services,sewerage-services/_search', 'name': 'ws_todays_completed_application_withinSLA', 'lambda': extract_ws_todays_completed_applications_withinSLA, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": [\n        {{\n          "range": {{\n            "Data.auditDetails.lastModifiedTime": {{\n              "gte": {0},\n              "lt": {1}\n            }}\n          }}\n        }}\n      ]\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "status_distribution": {{\n                  "terms": {{\n                    "field": "Data.applicationStatus.keyword",\n                    "size": 1000\n                  }}\n                }},\n                "sla_values": {{\n                  "sum": {{\n                    "field": "Data.history.state.sla"\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

ws_todays_sla_compliance = {'path': 'water-services,sewerage-services/_search', 'name': 'ws_todays_sla_compliance', 'lambda': extract_ws_todays_sla_compliance, 'query': '{{\n  "size": 0,\n  "query": {{\n    "bool": {{\n      "filter": [\n        {{\n          "range": {{\n            "Data.auditDetails.createdTime": {{\n              "gte": {0},\n              "lt": {1}\n            }}\n          }}\n        }}\n      ]\n    }}\n  }},\n  "aggs": {{\n    "ward": {{\n      "terms": {{\n        "field": "Data.ward.name.keyword",\n        "size": 10000\n      }},\n      "aggs": {{\n        "ulb": {{\n          "terms": {{\n            "field": "Data.tenantId.keyword",\n            "size": 10000\n          }},\n          "aggs": {{\n            "region": {{\n              "terms": {{\n                "field": "Data.tenantData.city.districtName.keyword",\n                "size": 10000\n              }},\n              "aggs": {{\n                "status_distribution": {{\n                  "terms": {{\n                    "field": "Data.applicationStatus.keyword",\n                    "size": 1000\n                  }}\n                }},\n                "sla_values": {{\n                  "sum": {{\n                    "field": "Data.history.state.sla"\n                  }}\n                }}\n              }}\n            }}\n          }}\n        }}\n      }}\n    }}\n  }}\n}}'}

# Public list imported by the scheduled and manual dashboard DAGs.
# Keep each query exactly once so the DAG iterates the intended WS metrics.
ws_queries = [
    ws_collection_by_payment_channel_type,
    ws_collection_by_tax_head_connection_type,
    ws_pending_connections,
    ws_sewerage_connections,
    ws_water_connections,
    ws_todays_applications,
    ws_closed_applications,
    ws_connections_created_by_connection_type,
    ws_connections_created_by_channel_type,
    ws_total_transactions,
    ws_todays_completed_application_withinSLA,
    ws_todays_sla_compliance,
]
