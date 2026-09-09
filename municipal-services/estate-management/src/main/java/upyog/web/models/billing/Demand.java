package upyog.web.models.billing;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.User;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Demand {
    private String id;
    private String tenantId;
    private String consumerCode;
    private String consumerType;
    private String businessService;
    private User payer;
    private Long taxPeriodFrom;
    private Long taxPeriodTo;
    private List<DemandDetail> demandDetails;
    private Object additionalDetails;
}
