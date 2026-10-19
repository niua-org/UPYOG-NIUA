package org.egov.user.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Centralized configuration class managing application properties for the egov-user service.
 *
 * <p>Strictly adheres to the <b>Single Responsibility Principle (SRP)</b> and clean architecture
 * standards by consolidating property bindings loaded from {@code application.properties} without
 * hardcoded inline fallback values in {@code @Value} annotations.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConfiguration {

    // MDMS Configuration
    @Value("${mdms.host}")
    private String mdmsHost;

    @Value("${mdms.path}")
    private String mdmsPath;

    @Value("${mdms.address.type.enabled}")
    private Boolean mdmsAddressTypeEnabled;

    @Value("${mdms.address.type.moduleName}")
    private String mdmsAddressTypeModuleName;

    @Value("${mdms.address.type.masterName}")
    private String mdmsAddressTypeMasterName;

    @Value("${mdms.address.type.filter}")
    private String mdmsAddressTypeFilter;

    @Value("${address.types.allowed}")
    private String allowedAddressTypes;

    // Cache Configuration
    @Value("${address.type.cache.ttl.millis}")
    private Long addressTypeCacheTtlMillis;

    @Value("${address.type.cache.cron}")
    private String addressTypeCacheCron;

    // Token Validity & Format Configuration
    @Value("${access.token.validity.in.minutes}")
    private Integer accessTokenValidityInMinutes;

    @Value("${refresh.token.validity.in.minutes}")
    private Integer refreshTokenValidityInMinutes;

    @Value("${oauth2.token.format}")
    private String oauth2TokenFormat;

    // Multi-Auth & Verification Configuration
    @Value("${citizen.login.password.otp.fixed.enabled}")
    private Boolean citizenLoginPasswordOtpFixedEnabled;

    @Value("${citizen.login.password.otp.fixed.value}")
    private String citizenLoginPasswordOtpFixedValue;

    @Value("${roles.state.level.enabled}")
    private Boolean rolesStateLevelEnabled;

    @Value("${otp.max.invalid.attempts}")
    private Integer otpMaxInvalidAttempts;

    @Value("${aadhaar.otp.max.attempts}")
    private Integer aadhaarOtpMaxAttempts;

    @Value("${requester.service.host}")
    private String requesterServiceHost;

    @Value("${requester.aadhaar.verify.endpoint}")
    private String requesterAadhaarVerifyEndpoint;

    @Value("${fixed.otp.enabled}")
    private Boolean fixedOtpEnabled;

    @Value("${fixed.otp.password}")
    private String fixedOtpPassword;

    @Value("${state.level.tenant.id}")
    private String stateLevelTenantId;

    @Value("${filestore.validation.enabled}")
    private Boolean filestoreValidationEnabled;
}
