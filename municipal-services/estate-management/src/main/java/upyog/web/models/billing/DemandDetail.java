package upyog.web.models.billing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandDetail {
    private String id;
    private String demandId;
    private String taxHeadMasterCode;
    private BigDecimal taxAmount;
    private BigDecimal collectionAmount;
    private Object additionalDetails;
    private String tenantId;
}
