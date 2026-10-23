package org.upyog.family.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service configuration properties for upyog-family-service.
 *
 * <p>Contains integration host paths, MDMS master data settings, Kafka topic names,
 * and business threshold rules for the UPYOG platform.</p>
 *
 * <p>Strictly avoids default values inside &#64;Value annotations to adhere to strict validation guidelines.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyConfiguration {

    @Value("${egov.idgen.host}")
    private String idGenHost;

    @Value("${egov.idgen.path}")
    private String idGenPath;

    @Value("${egov.idgen.family.id.name}")
    private String idGenFamilyIdName;

    @Value("${egov.idgen.family.id.format}")
    private String idGenFamilyIdFormat;

    @Value("${egov.user.host}")
    private String userHost;

    @Value("${egov.user.search.path}")
    private String userSearchPath;

    @Value("${egov.filestore.host}")
    private String fileStoreHost;

    @Value("${egov.filestore.url.path}")
    private String fileStoreUrlPath;

    @Value("${egov.mdms.host}")
    private String mdmsHost;

    @Value("${egov.mdms.search.path}")
    private String mdmsSearchPath;

    @Value("${requester.services.dx.host}")
    private String requesterDxHost;

    @Value("${requester.services.dx.verify.otp.path}")
    private String requesterDxVerifyOtpPath;

    @Value("${family.mdms.module}")
    private String mdmsFamilyModule;

    @Value("${family.mdms.relationship.master}")
    private String mdmsRelationshipMaster;

    @Value("${family.mdms.relationship.filter}")
    private String mdmsRelationshipFilter;

    @Value("${family.relationship.cache.ttl}")
    private Long relationshipCacheTtl;

    @Value("${family.kafka.create.topic}")
    private String createFamilyTopic;

    @Value("${family.kafka.update.topic}")
    private String updateFamilyTopic;

    @Value("${family.kafka.split.topic}")
    private String splitFamilyTopic;

    @Value("${family.kafka.merge.topic}")
    private String mergeFamilyTopic;

    @Value("${family.kafka.add.member.topic}")
    private String addMemberTopic;

    @Value("${family.kafka.leave.family.topic}")
    private String leaveFamilyTopic;

    @Value("${family.kafka.assign.hof.topic}")
    private String assignHofTopic;

    @Value("${family.default.offset}")
    private Integer defaultOffset;

    @Value("${family.default.limit}")
    private Integer defaultLimit;

    @Value("${family.max.limit}")
    private Integer maxLimit;
}
