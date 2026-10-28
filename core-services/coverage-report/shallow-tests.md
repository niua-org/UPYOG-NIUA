# Shallow / low-value tests (15-module scope)

Modules: core-services PR set + finance-collections-voucher-consumer + dashboard-analytics.

- **egov-user** `src/test/java/org/egov/user/web/controller/UserRequestControllerTest.java`: no assertions or verify()
- **egov-workflow-v2** `src/test/java/org/egov/wf/web/controllers/BusinessServiceControllerTest.java`: no assertions or verify()
- **egov-workflow-v2** `src/test/java/org/egov/wf/web/controllers/EscalationControllerTest.java`: no assertions or verify()
