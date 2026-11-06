package org.egov.garbageservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring configuration properties bean holding application settings, external endpoints, Kafka topics, and MDMS configs.
 */
@Component
@Getter
@Setter
public class GarbageServiceConfig {

    @Value("${workflow.context.path}")
    public String workflowHost;

    @Value("${workflow.transition.path}")
    public String workflowEndpointTransition;

    @Value("${workflow.business.search}")
    public String workflowBusinessServiceSearchPath;

    @Value("${workflow.valid.action.search.path}")
    private String workflowValidActionSearchPath;

    @Value("${egov.billing.host}")
    public String billingHost;

    @Value("${egov.bill.endpoint.fetch}")
    public String fetchBillEndpoint;

    @Value("${egov.bill.endpoint.search}")
    public String searchBillEndpoint;

    @Value("${egov.cancel.bill.endpoint}")
    public String cancleBillEndpoint;

    @Value("${egov.update.bill.endpoint}")
    private String updateBillEndpoint;

    @Value("${egov.gc.business.service}")
    private String businessService;

    @Value("${egov.gc.module.name}")
    private String moduleName;

    @Value("${egov.demand.create.endpoint}")
    private String demandCreateEndpoint;

    @Value("${egov.demand.update.endpoint}")
    private String demandUpdateEndpoint;

    @Value("${egov.demand.search.endpoint}")
    private String demandSearchEndpoint;

    @Value("${kafka.topics.save.scheduler.log}")
    private String schedulerLogTopic;

    @Value("${kafka.topics.save.monthly.bill}")
    private String monthlyRentPaymentSaveTopic;

    @Value("${egov.mdms.host}")
    private String mdmsHost;

    @Value("${egov.mdms.search.endpoint}")
    private String mdmsSearchEndpoint;

    @Value("${egov.mdms.host}")
    private String mdmsServiceHostUrl;

    @Value("${kafka.topics.save.garbage}")
    private String saveGarbageApplicationTopic;

    @Value("${kafka.topics.update.garbage}")
    private String updateGarbageApplicationTopic;

    @Value("${kafka.topics.save.connection.log}")
    private String saveConnectionLogTopic;

    @Value("${kafka.topics.notification.sms}")
    private String notificationSmsTopic;

    @Value("${egov.user.host}")
    private String userServiceHostUrl;

    @Value("${egov.user.search.endpoint}")
    private String userSearchEndpoint;

    @Value("${egov.pt.host}")
    private String ptHost;

    @Value("${egov.pt.search.endpoint}")
    private String ptSearchEndpoint;
}
