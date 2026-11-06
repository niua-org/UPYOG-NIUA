package org.upyog.family.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.tracer.model.CustomException;
import org.upyog.family.util.FamilyConstants;
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

    @Value("${egov.idgen.member.id.name}")
    private String idGenMemberIdName;

    @Value("${egov.idgen.member.id.format}")
    private String idGenMemberIdFormat;

    @Value("${egov.user.host}")
    private String userHost;

    @Value("${egov.user.search.path}")
    private String userSearchPath;

    @Value("${egov.user.create.path}")
    private String userCreatePath;

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

    @Value("${family.consent.mock.enabled}")
    private Boolean mockConsentEnabled;

    @Value("${family.consent.mock.fixed.otp}")
    private String mockFixedOtp;

    @Value("${family.otp.verify.max.attempts}")
    private Integer familyOtpVerifyMaxAttempts;

    @Value("${family.otp.reference.ttl.minutes}")
    private Long familyOtpReferenceTtlMinutes;

    @Value("${family.consent.token.ttl.minutes}")
    private Long consentTokenTtlMinutes;

    @Value("${family.sms.otp.message.template}")
    private String smsOtpMessageTemplate;

    @Value("${egov.otp.host}")
    private String otpHost;

    @Value("${egov.otp.create.path}")
    private String otpCreatePath;

    @Value("${egov.otp.validate.path}")
    private String otpValidatePath;

    @Value("${kafka.topics.notification.sms}")
    private String smsNotifTopic;

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

    /**
     * Gets the configured time-to-live (TTL) for consent tokens in milliseconds.
     * Defaults to 15 minutes if not explicitly configured or non-positive.
     *
     * @return TTL duration in milliseconds
     */
    public long getConsentTokenTtlMs() {
        if (consentTokenTtlMinutes == null || consentTokenTtlMinutes <= 0) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "family.consent.token.ttl.minutes is not configured");
        }
        return consentTokenTtlMinutes * 60L * 1000L;
    }

    /**
     * Returns configured maximum failed OTP validation attempts per reference.
     *
     * @return max attempts, at least 1
     */
    public int getFamilyOtpVerifyMaxAttempts() {
        if (familyOtpVerifyMaxAttempts == null || familyOtpVerifyMaxAttempts < 1) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "family.otp.verify.max.attempts is not configured");
        }
        return familyOtpVerifyMaxAttempts;
    }

    /**
     * Returns OTP reference TTL in milliseconds.
     *
     * @return TTL in milliseconds
     */
    public long getFamilyOtpReferenceTtlMs() {
        if (familyOtpReferenceTtlMinutes == null || familyOtpReferenceTtlMinutes <= 0) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                    "family.otp.reference.ttl.minutes is not configured");
        }
        return familyOtpReferenceTtlMinutes * 60L * 1000L;
    }
}
