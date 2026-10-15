package org.upyog.dx.upyogdata.pfms.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;
import org.upyog.dx.upyogdata.pfms.entity.PfmsDataEntity;

import java.util.List;

/**
 * Kafka payload published after API contract is transformed into persistence entities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PFMSCreateTransactionKafkaMessage {

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    private List<PfmsDataEntity> data;
}
