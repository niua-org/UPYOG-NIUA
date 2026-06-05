package org.egov.egf.contract.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.egov.infra.microservice.models.RequestInfo;
import org.hibernate.validator.constraints.SafeHtml;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AccountEntityRequest {

    @NotNull
    @JsonProperty("tenantId")
    @SafeHtml
    private String tenantId;

    @NotNull
    @JsonProperty("accountDetailType")
    @SafeHtml
    private Integer accountDetailType;

    @NotNull
    @JsonProperty("name")
    @SafeHtml
    private String name;

    @NotNull
    @JsonProperty("code")
    @SafeHtml
    private String code;

    @NotNull
    @JsonProperty("narration")
    @SafeHtml
    private String narration;

    @NotNull
    @JsonProperty("isActive")
    @SafeHtml
    private Boolean isActive;


    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;


    @Override
    public String toString() {
        return "AccountEntityRequest( [accountDetailType=" + accountDetailType + "], [name="+name+"],[code="+code+"],[narration="+narration+"],[isActive="+isActive+"])";
    }



}
