package org.egov.dx.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class Configurations {

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
	@Value("${tan.verification.mock.enabled}")
	private Boolean tanVerificationMockEnabled;

	@Value("${tan.verification.mock.records.path}")
	private String tanVerificationMockRecordsPath;

	@Value("${tan.verification.base.url}")
	private String tanVerificationBaseUrl;

	@Value("${tan.verification.path}")
	private String tanVerificationPath;

	@Value("${tan.verification.client.id}")
	private String tanVerificationClientId;

	@Value("${tan.verification.client.secret}")
	private String tanVerificationClientSecret;

	@Value("${tan.verification.connect.timeout}")
	private Integer tanVerificationConnectTimeout;

	@Value("${tan.verification.read.timeout}")
	private Integer tanVerificationReadTimeout;

	// ---------------- PAN Verification Configurations ----------------
	@Value("${pan.verification.mock.enabled}")
	private Boolean panVerificationMockEnabled;

	@Value("${pan.verification.mock.records.path}")
	private String panVerificationMockRecordsPath;

	@Value("${pan.verification.base.url}")
	private String panVerificationBaseUrl;

	@Value("${pan.verification.path}")
	private String panVerificationPath;

	@Value("${pan.verification.client.id}")
	private String panVerificationClientId;

	@Value("${pan.verification.client.secret}")
	private String panVerificationClientSecret;

	@Value("${pan.verification.connect.timeout}")
	private Integer panVerificationConnectTimeout;

	@Value("${pan.verification.read.timeout}")
	private Integer panVerificationReadTimeout;

	// ---------------- GST Verification Configurations ----------------
	@Value("${gst.verification.mock.enabled}")
	private Boolean gstVerificationMockEnabled;

	@Value("${gst.verification.mock.records.path}")
	private String gstVerificationMockRecordsPath;

	@Value("${gst.verification.base.url}")
	private String gstVerificationBaseUrl;

	@Value("${gst.verification.path}")
	private String gstVerificationPath;

	@Value("${gst.verification.api.key}")
	private String gstVerificationApiKey;

	@Value("${gst.verification.connect.timeout}")
	private Integer gstVerificationConnectTimeout;

	@Value("${gst.verification.read.timeout}")
	private Integer gstVerificationReadTimeout;

	// ---------------- MDMS Configurations ----------------
	@Value("${egov.mdms.host}")
	private String mdmsHost;

	@Value("${egov.mdms.search.endpoint}")
	private String mdmsSearchEndpoint;

	@Value("${mdms.verification.module}")
	private String mdmsVerificationModule;

	@Value("${mdms.verification.master}")
	private String mdmsVerificationMaster;
}
