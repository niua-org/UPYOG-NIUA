package org.upyog.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.upyog.web.models.CommonDetails;

import static org.assertj.core.api.Assertions.assertThat;

class PetDetailsMapperTest {

    private final PetDetailsMapper mapper = new PetDetailsMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getModuleName_isNotBlank() {
        assertThat(mapper.getModuleName()).isNotBlank();
    }

    @Test
    void mapJsonToCommonDetails_emptyJson_returnsDefaults() throws Exception {
        CommonDetails details = mapper.mapJsonToCommonDetails(objectMapper.readTree("{}"));
        assertThat(details).isNotNull();
        assertThat(details.getStatus()).isNotNull();
    }
}
