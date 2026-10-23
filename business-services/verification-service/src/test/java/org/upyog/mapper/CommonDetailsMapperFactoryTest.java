package org.upyog.mapper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonDetailsMapperFactoryTest {

    @Test
    void getMapper_returnsRegisteredMapper() {
        CommonDetailsMapperFactory factory = new CommonDetailsMapperFactory(
                List.of(new PropertyDetailsMapper(), new PetDetailsMapper()));
        assertThat(factory.getMapper("property")).isInstanceOf(PropertyDetailsMapper.class);
    }

    @Test
    void getMapper_unknownModule_throws() {
        CommonDetailsMapperFactory factory = new CommonDetailsMapperFactory(List.of(new PropertyDetailsMapper()));
        assertThatThrownBy(() -> factory.getMapper("UNKNOWN")).isInstanceOf(IllegalArgumentException.class);
    }
}
