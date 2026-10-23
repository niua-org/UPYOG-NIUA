package com.ingestpipeline.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseCodeTest {
    @Test
    void values_nonEmpty() {
        assertThat(ResponseCode.values()).isNotEmpty();
    }
}
