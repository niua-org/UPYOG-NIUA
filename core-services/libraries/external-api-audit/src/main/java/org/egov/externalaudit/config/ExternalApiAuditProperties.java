package org.egov.externalaudit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.api.audit")
public class ExternalApiAuditProperties {

    /**
     * Kafka topic consumed by egov-persister ({@code external-api-audit-persister.yml}).
     */
    private String detailTopic = "external-api-audit-details";

    private int maxPayloadBytes = 204800;

    /**
     * When false, request/response bodies are not persisted (metadata envelope only).
     */
    private boolean capturePayloadEnabled = true;

    /**
     * Source service identifier stored in column {@code state}.
     */
    private String sourceService = "unknown";

    private long staleThresholdMs = 600000;

    private List<String> sensitiveFields = new ArrayList<>(List.of(
            "password",
            "accessToken",
            "access_token",
            "refreshToken",
            "refresh_token",
            "authorization",
            "authToken",
            "token",
            "ulbBankAccountNumber",
            "beneficiaryAccountNumber",
            "fromAccount",
            "toAccount",
            "accountNumber",
            "bankAccount",
            "ULBBankAccountNumber",
            "BeneficiaryAccountNumber",
            "FromAccount",
            "ToAccount",
            "Password",
            "AccessToken",
            "RefreshToken"
    ));

    private Reconciliation reconciliation = new Reconciliation();

    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Reconciliation {
        /**
         * Enable only in the service whose datasource hosts {@code ug_external_api_*} tables.
         */
        private boolean enabled = false;
        private String cron = "0 0 6 * * *";
        private String zone = "Asia/Kolkata";
    }

    @Getter
    @Setter
    public static class Cleanup {
        /**
         * Enable only in the service whose datasource hosts {@code ug_external_api_*} tables.
         */
        private boolean enabled = false;
        private String cron = "0 30 3 * * *";
        private String zone = "Asia/Kolkata";
        private long retentionMs = 2592000000L;
    }
}
