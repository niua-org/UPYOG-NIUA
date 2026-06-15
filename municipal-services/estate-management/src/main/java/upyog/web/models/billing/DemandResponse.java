package upyog.web.models.billing;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandResponse {
    private ResponseInfo responseInfo;
    private List<Demand> demands;
}
