// generated-by: generate_per_class_unit_tests.py
package org.egov.waterconnection.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Channel.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Channel first = Channel.values()[0];
        assertThat(Channel.valueOf(first.name())).isEqualTo(first);
    }
}
