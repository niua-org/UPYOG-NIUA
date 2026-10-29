package org.egov.pqm.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pqm.error.CustomException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonParserTest {

    private final JsonParser<Object> parser = new JsonParser<>(new ObjectMapper());

    @Test
    void parseJsonToMap_whenInvalidJson_throwsCustomException() {
        assertThatThrownBy(() -> parser.parseJsonToMap("{}", Object.class))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void parseJsonToMap_whenValidPayload_returnsCodeMap() {
        String json = "{\"mdms\":[{\"data\":{\"code\":\"C1\",\"name\":\"n\"}}]}";
        Map<String, Object> out = parser.parseJsonToMap(json, Object.class);
        assertThat(out).containsKey("C1");
    }
}
