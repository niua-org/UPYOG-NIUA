package com.tarento.analytics.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JSONObjectUtilTest {

    @Test
    void getJsonString_withMapper_serializesObject() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = JSONObjectUtil.getJsonString(mapper, Map.of("key", "value"));

        assertThat(json).contains("\"key\":\"value\"");
    }

    @Test
    void getJsonString_nullMapper_returnsNull() throws Exception {
        assertThat(JSONObjectUtil.getJsonString(null, Map.of())).isNull();
    }
}
