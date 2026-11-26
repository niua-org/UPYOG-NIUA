package org.egov.dx.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * Centralized Spring configuration bean binding external application properties
 * for requester-services-dx including TAN, PAN, GST, and MDMS verification services.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Configuration
@Getter
public class Configurations {

	/**
	 * Configured application timezone identifier (property: {@code app.timezone}, e.g., {@code Asia/Kolkata}).
	 */
	@Value("${app.timezone}")
	private String appTimezone;

	@Value("${egov.payer.validation.enable}")
	private String validationFlag;

	@Value("${egov.integration.system.user.uuid}")
	private String authTokenVariable;
		
	@Value("${authorization.url}")
	private String authorizationURL;

	@Value("${pt.redirect.url}")
	private String ptRedirectURL;

	@Value("${register.redirect.url}")
	private String registerRedirectURL;
	
	@Value("${api.host}")
	private String apiHost;

	@Value("${token.oauth.uri}")
	private String tokenOauthURI;
	
	@Value("${user.oauth.uri}")
	private String userOauthURI;

	@Value("${issued.files.uri}")
	private String issuedFilesURI;

	@Value("${get.file.uri}")
	private String getFileURI;

	@Value("${response.type}")
	private String responseType;

	@Value("${client.id}")
	private String clientId;

	@Value("${client.secret}")
	private String clientSecret;
	
	@Value("${register.client.id}")
	private String registerClientId;

	@Value("${register.client.secret}")
	private String registerClientSecret;

	@Value("${state}")
	private String state;

	@Value("${dl.flow}")
	private String dlFlow;

	
	@Value("${esign.responseurl}")
	private String responseUrl;
	
	@Value("${esign.redirecturl}")
	private String redirectUrl;
	
	@Value("${esign.licencefile}" )
	private String licenceFile;
	
	@Value("${esign.pfxpath}")
	private String pfxPath;
	
	@Value("${esign.pfxpassword}")
	private String pfxPassword;
	
	@Value("${esign.pfxallias}")
	private String pfxAllias;
	
	@Value("${esign.pdf}")
	private String pdf;
	
	@Value("${esign.tempfolder}")
	private String tempFolder;
	
	@Value("${esign.outputfolder}")
	private String outputFolder;
	
	@Value("${esign.UIURL}")
	private String uIURL;
		
	@Value("${persister.save.tl.esign.txns}")
	private String saveTLEsignTxnTopic;
	
	@Value("${persister.update.tl.esign.txns}")
	private String updateTLEsignTxnTopic;
	
	
	@Value("${egov.idgen.host}")
	private String IdGenHost;
	
	@Value("${egov.idgen.path}")
	private String IdGenPath;
	
	@Value("${egov.idgen.ack.format}")
	private String IdGenFormat;
	
	@Value("${egov.idgen.ack.name}")
	private String IdGenName;

	@Value("${egov.enc.host}")
	private String encHost;

	@Value("${egov.enc.encrypt.endpoint}")
	private String encEncryptURL;
	
	@Value("${egov.enc.decrypt.endpoint}")
	private String encDecryptURL;
	
	@Value("${egov.user.host}")
	private String UserHost;
	
	@Value("${egov.user.endpoint}")
	private String userEndpoint;
	
	
	@Value("${esign.aspId}")
	private String AspId;
	
	@Value("${esign.v1.url}")
	private String EsignV1Url;
	
	@Value("${esign.v2.url}")
	private String EsignV2Url;
	
	@Value("${esign.signature.content.size}")
	private int SignatureContentSize;

	// ---------------- Aadhaar Gateway Configurations ----------------
	@Value("${aadhaar.gateway.mock.enabled}")
	private Boolean aadhaarGatewayMockEnabled;

	@Value("${aadhaar.gateway.base.url}")
	private String aadhaarGatewayBaseUrl;

	@Value("${aadhaar.gateway.send.otp.path}")
	private String aadhaarGatewaySendOtpPath;

	@Value("${aadhaar.gateway.verify.otp.path}")
	private String aadhaarGatewayVerifyOtpPath;

	@Value("${aadhaar.gateway.api.key}")
	private String aadhaarGatewayApiKey;

	@Value("${aadhaar.gateway.api.secret}")
	private String aadhaarGatewayApiSecret;

	@Value("${aadhaar.gateway.api.version}")
	private String aadhaarGatewayApiVersion;

	@Value("${aadhaar.gateway.connect.timeout}")
	private Integer aadhaarGatewayConnectTimeout;

	@Value("${aadhaar.gateway.read.timeout}")
	private Integer aadhaarGatewayReadTimeout;

	@Value("${aadhaar.otp.max.attempts}")
	private Integer aadhaarOtpMaxAttempts;
  
	@Value("${esign.gateway.url}")
	private String gatewayUrl;

	@Value("${filestore.host}")
	private String fileStoreUrl;
	
	@Value("${filestore.post.endpoint}")
	private String fileStorePostUrl;

	// ---------------- Family Service Integration Configurations ----------------
	@Value("${family.service.host}")
	private String familyServiceHost;

	@Value("${family.service.search.endpoint}")
	private String familyServiceSearchEndpoint;

	// ---------------- TAN Verification Configurations ----------------

	/**
	 * Property: {@code tan.verification.mock.enabled}.
	 * Master switch for TAN verification mock adapter.
	 * <b>CRITICAL:</b> Must be {@code false} in production environments.
	 */
	@Value("${tan.verification.mock.enabled}")
	private Boolean tanVerificationMockEnabled;

	/**
	 * Property: {@code tan.verification.mock.records.path}.
	 * Classpath resource location containing curated mock TAN JSON dataset.
	 */
	@Value("${tan.verification.mock.records.path}")
	private String tanVerificationMockRecordsPath;

	/**
	 * Property: {@code tan.verification.mock.synthesize-unknown}.
	 * When false (default), unlisted valid TANs return NOT_FOUND. When true, synthesizes active TAN.
	 */
	@Value("${tan.verification.mock.synthesize-unknown}")
	private Boolean tanVerificationMockSynthesizeUnknownEnabled;

	/**
	 * Property: {@code tan.verification.base.url}.
	 * Base HTTP endpoint for official Income Tax Department (ITD) TAN verification service.
	 */
	@Value("${tan.verification.base.url}")
	private String tanVerificationBaseUrl;

	/**
	 * Property: {@code tan.verification.path}.
	 * URI path for ITD TAN verification endpoint (e.g., {@code tan/verify}).
	 */
	@Value("${tan.verification.path}")
	private String tanVerificationPath;

	/**
	 * Property: {@code tan.verification.client.id}.
	 * Client identifier / API credentials assigned by ITD vendor for TAN verification.
	 */
	@Value("${tan.verification.client.id}")
	private String tanVerificationClientId;

	/**
	 * Property: {@code tan.verification.client.secret}.
	 * Client secret / signature key assigned by ITD vendor for TAN verification.
	 */
	@Value("${tan.verification.client.secret}")
	private String tanVerificationClientSecret;

	/**
	 * Property: {@code tan.verification.connect.timeout}.
	 * HTTP connection timeout in milliseconds for ITD TAN service (e.g. 5000 ms).
	 */
	@Value("${tan.verification.connect.timeout}")
	private Integer tanVerificationConnectTimeout;

	/**
	 * Property: {@code tan.verification.read.timeout}.
	 * HTTP socket read timeout in milliseconds for ITD TAN service (e.g. 10000 ms).
	 */
	@Value("${tan.verification.read.timeout}")
	private Integer tanVerificationReadTimeout;

	// ---------------- PAN Verification Configurations ----------------

	/**
	 * Property: {@code pan.verification.mock.enabled}.
	 * Master switch for PAN verification mock adapter.
	 * <b>CRITICAL:</b> Must be {@code false} in production environments.
	 */
	@Value("${pan.verification.mock.enabled}")
	private Boolean panVerificationMockEnabled;

	/**
	 * Property: {@code pan.verification.mock.records.path}.
	 * Classpath resource location containing curated mock PAN JSON dataset.
	 */
	@Value("${pan.verification.mock.records.path}")
	private String panVerificationMockRecordsPath;

	/**
	 * Property: {@code pan.verification.mock.synthesize-unknown}.
	 * When false (default), unlisted valid PANs return NOT_FOUND. When true, synthesizes active PAN.
	 */
	@Value("${pan.verification.mock.synthesize-unknown}")
	private Boolean panVerificationMockSynthesizeUnknownEnabled;

	/**
	 * Property: {@code pan.verification.base.url}.
	 * Base HTTP endpoint for official Income Tax Department (ITD) PAN verification service.
	 */
	@Value("${pan.verification.base.url}")
	private String panVerificationBaseUrl;

	/**
	 * Property: {@code pan.verification.path}.
	 * URI path for ITD PAN verification endpoint (e.g., {@code pan/verify}).
	 */
	@Value("${pan.verification.path}")
	private String panVerificationPath;

	/**
	 * Property: {@code pan.verification.client.id}.
	 * Client identifier / API credentials assigned by ITD vendor for PAN verification.
	 */
	@Value("${pan.verification.client.id}")
	private String panVerificationClientId;

	/**
	 * Property: {@code pan.verification.client.secret}.
	 * Client secret / signature key assigned by ITD vendor for PAN verification.
	 */
	@Value("${pan.verification.client.secret}")
	private String panVerificationClientSecret;

	/**
	 * Property: {@code pan.verification.connect.timeout}.
	 * HTTP connection timeout in milliseconds for ITD PAN service (e.g. 5000 ms).
	 */
	@Value("${pan.verification.connect.timeout}")
	private Integer panVerificationConnectTimeout;

	/**
	 * Property: {@code pan.verification.read.timeout}.
	 * HTTP socket read timeout in milliseconds for ITD PAN service (e.g. 10000 ms).
	 */
	@Value("${pan.verification.read.timeout}")
	private Integer panVerificationReadTimeout;

	// ---------------- GST Verification Configurations ----------------

	/**
	 * Property: {@code gst.verification.mock.enabled}.
	 * Master switch for GST taxpayer verification mock adapter.
	 * <b>CRITICAL:</b> Must be {@code false} in production environments.
	 */
	@Value("${gst.verification.mock.enabled}")
	private Boolean gstVerificationMockEnabled;

	/**
	 * Property: {@code gst.verification.mock.records.path}.
	 * Classpath resource location containing curated mock GST JSON dataset.
	 */
	@Value("${gst.verification.mock.records.path}")
	private String gstVerificationMockRecordsPath;

	/**
	 * Property: {@code gst.verification.mock.synthesize-unknown}.
	 * When false (default), unlisted valid GSTINs return NOT_FOUND. When true, synthesizes active taxpayer.
	 */
	@Value("${gst.verification.mock.synthesize-unknown}")
	private Boolean gstVerificationMockSynthesizeUnknownEnabled;

	/**
	 * Property: {@code gst.verification.base.url}.
	 * Base HTTP endpoint for official IRIS GST taxpayer search service.
	 */
	@Value("${gst.verification.base.url}")
	private String gstVerificationBaseUrl;

	/**
	 * Property: {@code gst.verification.path}.
	 * URI path for IRIS GST search endpoint (e.g., {@code api/search}).
	 */
	@Value("${gst.verification.path}")
	private String gstVerificationPath;

	/**
	 * Property: {@code gst.verification.api.key}.
	 * API authorization key for accessing the IRIS GST taxpayer gateway.
	 */
	@Value("${gst.verification.api.key}")
	private String gstVerificationApiKey;

	/**
	 * Property: {@code gst.verification.connect.timeout}.
	 * HTTP connection timeout in milliseconds for IRIS GST service (e.g. 5000 ms).
	 */
	@Value("${gst.verification.connect.timeout}")
	private Integer gstVerificationConnectTimeout;

	/**
	 * Property: {@code gst.verification.read.timeout}.
	 * HTTP socket read timeout in milliseconds for IRIS GST service (e.g. 10000 ms).
	 */
	@Value("${gst.verification.read.timeout}")
	private Integer gstVerificationReadTimeout;

	// ---------------- MDMS Configurations ----------------

	/**
	 * Property: {@code egov.mdms.host}.
	 * Base host URL for eGov MDMS service.
	 */
	@Value("${egov.mdms.host}")
	private String mdmsHost;

	/**
	 * Property: {@code egov.mdms.search.endpoint}.
	 * Search endpoint URI for querying MDMS masters.
	 */
	@Value("${egov.mdms.search.endpoint}")
	private String mdmsSearchEndpoint;

	/**
	 * Property: {@code mdms.verification.module}.
	 * MDMS module name hosting verification configuration (default: {@code common-masters}).
	 */
	@Value("${mdms.verification.module}")
	private String mdmsVerificationModule;

	/**
	 * Property: {@code mdms.verification.master}.
	 * MDMS master record name governing tenant verification rules (default: {@code VerificationConfiguration}).
	 */
	@Value("${mdms.verification.master}")
	private String mdmsVerificationMaster;

	/**
	 * Property: {@code mdms.verification.cache.ttl.seconds}.
	 * Cache TTL in seconds for MDMS tenant verification configurations (default: 300 seconds / 5 mins).
	 */
	@Value("${mdms.verification.cache.ttl.seconds}")
	private Long mdmsVerificationCacheTtlSeconds;

	/**
	 * Property: {@code mdms.gender.module}.
	 * MDMS module name hosting gender master data.
	 */
	@Value("${mdms.gender.module}")
	private String mdmsGenderModule;

	/**
	 * Property: {@code mdms.gender.master}.
	 * MDMS master name governing gender codes.
	 */
	@Value("${mdms.gender.master}")
	private String mdmsGenderMaster;

	/**
	 * Helper method returning whether TAN mock synthesis of unknown records is enabled.
	 *
	 * @return true if enabled, false otherwise
	 */
	public boolean isTanVerificationMockSynthesizeUnknownEnabled() {
		return Boolean.TRUE.equals(tanVerificationMockSynthesizeUnknownEnabled);
	}

	/**
	 * Helper method returning whether PAN mock synthesis of unknown records is enabled.
	 *
	 * @return true if enabled, false otherwise
	 */
	public boolean isPanVerificationMockSynthesizeUnknownEnabled() {
		return Boolean.TRUE.equals(panVerificationMockSynthesizeUnknownEnabled);
	}

	/**
	 * Helper method returning whether GST mock synthesis of unknown records is enabled.
	 *
	 * @return true if enabled, false otherwise
	 */
	public boolean isGstVerificationMockSynthesizeUnknownEnabled() {
		return Boolean.TRUE.equals(gstVerificationMockSynthesizeUnknownEnabled);
	}
}
