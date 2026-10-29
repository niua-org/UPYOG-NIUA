package org.upyog.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.upyog.web.models.CommonDetails;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityHallDetailsMapperBehaviorTest {

    private final CommunityHallDetailsMapper mapper = new CommunityHallDetailsMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapJsonToCommonDetails_bookedStatus_mapsApplicantDetails() throws Exception {
        String json = """
                {
                  "hallsBookingApplication": [{
                    "bookingNo": "CHB-1",
                    "bookingStatus": "BOOKED",
                    "bookingSlotDetails": [{"bookingDate": "2024-06-01", "hallCode": "HALL-A"}],
                    "applicantDetail": {"applicantName": "Citizen", "applicantMobileNo": "9999007890"}
                  }]
                }
                """;
        CommonDetails details = mapper.mapJsonToCommonDetails(objectMapper.readTree(json));

        assertThat(details.getApplicationNumber()).isEqualTo("CHB-1");
        assertThat(details.getStatus()).isEqualTo("BOOKED");
        assertThat(details.getMobileNumber()).isEqualTo("999****890");
    }

    @Test
    void mapJsonToCommonDetails_pendingStatus_returnsPending() throws Exception {
        String json = """
                {"hallsBookingApplication":[{"bookingNo":"CHB-2","bookingStatus":"APPLIED"}]}
                """;
        CommonDetails details = mapper.mapJsonToCommonDetails(objectMapper.readTree(json));
        assertThat(details.getStatus()).isEqualTo("Pending");
    }
}
