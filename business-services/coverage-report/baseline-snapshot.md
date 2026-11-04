# Coverage pass baseline snapshot (service / validator / util packages)

Captured at the start of the behavioural coverage pass (before this branch’s deepening commits).  
Regenerate comparable numbers with:

```bash
python3 scripts/coverage/module-summary-from-jacoco.py
```

| Module | Line % | Branch % |
|--------|--------|----------|
| billing-service | 61.2 | 50.5 |
| collection-services | 73.1 | 58.1 |
| dashboard-analytics | 59.0 | 34.7 |
| dashboard-data-engine | 79.2 | 54.5 |
| dashboard-data-extractor | 57.5 | 45.4 |
| dashboard-ingest | 47.8 | 36.6 |
| egf-instrument | 38.8 | 33.9 |
| egf-master | 29.2 | 24.0 |
| egov-apportion-service | 68.7 | 52.3 |
| egov-hrms | 41.3 | 22.7 |
| employee-dashboard | 67.7 | 27.5 |
| finance-collections-voucher-consumer | 35.1 | 6.4 |
| reconciliation-service | 87.5 | 100.0 |
| verification-service | 64.4 | 45.2 |

Full-module (all classes) rankings: see `baseline.md` (refreshed after each JaCoCo run).
