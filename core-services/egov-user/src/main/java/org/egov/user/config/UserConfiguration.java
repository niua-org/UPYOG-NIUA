package org.egov.user.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Value("${user.address.mandatory.fields.enabled}")
    private Boolean addressMandatoryFieldsEnabled;

    @Value("${user.address.update.strategy}")
    private Boolean addressSoftUpdateFlag;

    // Cache Configuration
    @Value("${address.type.cache.ttl.millis}")
    private Long addressTypeCacheTtlMillis;

    @Value("${address.type.cache.cron}")
    private String addressTypeCacheCron;

    // Income Identifier Type MDMS & Fallback Configuration
    @Value("${mdms.income.identifier.type.enabled}")
    private Boolean mdmsIncomeIdentifierTypeEnabled;

    @Value("${mdms.income.identifier.type.moduleName}")
    private String mdmsIncomeIdentifierTypeModuleName;

    @Value("${mdms.income.identifier.type.masterName}")
    private String mdmsIncomeIdentifierTypeMasterName;

    @Value("${mdms.income.identifier.type.filter}")
    private String mdmsIncomeIdentifierTypeFilter;

    @Value("${income.identifier.types.allowed}")
    private String allowedIncomeIdentifierTypes;

    @Value("${income.identifier.type.cache.ttl.millis}")
    private Long incomeIdentifierTypeCacheTtlMillis;

    // Token Validity & Format Configuration
    @Value("${access.token.validity.in.minutes}")
    private Integer accessTokenValidityInMinutes;

    @Value("${refresh.token.validity.in.minutes}")
    private Integer refreshTokenValidityInMinutes;

    @Value("${oauth2.token.format}")
    private String oauth2TokenFormat;

    // Multi-Auth & Verification Configuration
    @Value("${citizen.login.password.otp.enabled}")
    private Boolean citizenLoginPasswordOtpEnabled;

    @Value("${employee.login.password.otp.enabled}")
    private Boolean employeeLoginPasswordOtpEnabled;

    @Value("${citizen.login.password.otp.fixed.enabled}")
    private Boolean citizenLoginPasswordOtpFixedEnabled;

    @Value("${citizen.multi.account.selection.enabled}")
    private Boolean citizenMultiAccountSelectionEnabled;

    @Value("${citizen.login.password.otp.fixed.value}")
    private String citizenLoginPasswordOtpFixedValue;

    @Value("${roles.state.level.enabled}")
    private Boolean rolesStateLevelEnabled;

    @Value("${otp.max.invalid.attempts}")
    private Integer otpMaxInvalidAttempts;

    @Value("${aadhaar.otp.max.attempts}")
    private Integer aadhaarOtpMaxAttempts;

    @Value("${aadhaar.mock.enabled}")
    private Boolean aadhaarMockEnabled;

    public Boolean getAadhaarGatewayMockEnabled() {
        return this.aadhaarMockEnabled;
    }

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

    @Value("${family.service.host}")
    private String familyServiceHost;

    @Value("${family.service.state.jit.endpoint}")
    private String familyServiceStateJitEndpoint;

    @Value("${family.service.search.endpoint}")
    private String familyServiceSearchEndpoint;

    // Family MDMS Configuration
    @Value("${family.mdms.module}")
    private String mdmsFamilyModule;

    @Value("${family.mdms.configuration.master}")
    private String mdmsFamilyConfigurationMaster;

    @Value("${family.mdms.configuration.filter}")
    private String mdmsFamilyConfigurationFilter;

    @Value("${family.configuration.cache.ttl}")
    private Long familyConfigurationCacheTtl;

    @Value("${egov.user.oauth.token.endpoint}")
    private String userOauthTokenEndpoint;

    @Value("${egov.user.host}")
    private String userHost;

    @Value("${account.unlock.cool.down.period.minutes}")
    private Long accountUnlockCoolDownPeriod;

    @Value("${max.invalid.login.attempts.period.minutes}")
    private Long maxInvalidLoginAttemptsPeriod;

    @Value("${create.user.validate.name}")
    private Boolean createUserValidateName;

    @Value("${max.invalid.login.attempts}")
    private Long maxInvalidLoginAttempts;

    @Value("${egov.user.pwd.pattern}")
    private String pwdRegex;

    @Value("${egov.user.pwd.pattern.min.length}")
    private Integer pwdMinLength;

    @Value("${egov.user.pwd.pattern.max.length}")
    private Integer pwdMaxLength;

    @Value("${default.password.expiry.in.days}")
    private Integer defaultPasswordExpiryInDays;

    // DigiLocker Configuration
    @Value("${digilocker.register}")
    private Boolean digilockerRegister;

    @Value("${digilocker.search}")
    private Boolean digilockerSearch;

    @Value("${requester.service.endpoint}")
    private String requesterServiceEndpoint;

    // User Search & Registration Configuration
    @Value("${egov.user.search.default.size}")
    private Integer defaultSearchSize;

    @Value("${mobile.number.validation.workaround.enabled}")
    private String mobileValidationWorkaroundEnabled;

    @Value("${otp.validation.register.mandatory}")
    private Boolean otpValidationRegisterMandatory;

    @Value("${citizen.registration.withlogin.enabled}")
    private Boolean citizenRegistrationWithLoginEnabled;

    // IDGen Configuration
    @Value("${egov.idgen.host}")
    private String idGenHost;

    @Value("${egov.idgen.path}")
    private String idGenPath;

    @Value("${egov.idgen.citizen.id.name}")
    private String idGenCitizenIdName;

    @Value("${egov.idgen.user.doc.id.name}")
    private String idGenUserDocIdName;

    @Value("${egov.idgen.rural.property.id.name}")
    private String idGenRuralPropertyIdName;

    // Asset Authorization Configuration
    @Value("${egov.asset.privileged.roles}")
    private String assetPrivilegedRoles;

    // Asset Search Pagination Configuration
    @Value("${egov.asset.search.default.limit}")
    private Integer assetSearchDefaultLimit;

    @Value("${egov.asset.search.max.limit}")
    private Integer assetSearchMaxLimit;

    @Value("${egov.asset.search.default.offset}")
    private Integer assetSearchDefaultOffset;

    /**
     * Resolves the configured set of privileged role codes permitted to access citizen assets.
     *
     * @return set of uppercase role codes
     */
    public Set<String> getAssetPrivilegedRoles() {
        if (StringUtils.isBlank(assetPrivilegedRoles)) {
            return Collections.emptySet();
        }
        return Arrays.stream(assetPrivilegedRoles.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }
}
