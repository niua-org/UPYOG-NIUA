package org.egov.pgr.web.modelsV2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.Workflow;

import javax.validation.Valid;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceWrapperV3 {


    @Valid
    @NonNull
    @JsonProperty("service")
    private ServiceV3 service = null;

    @Valid
    @JsonProperty("workflow")
    private Workflow workflow = null;

}
