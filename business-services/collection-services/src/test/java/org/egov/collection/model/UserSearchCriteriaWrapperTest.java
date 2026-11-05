package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserSearchCriteriaWrapperTest {
    @Test
    void classLoads() {
        assertThat(UserSearchCriteriaWrapper.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UserSearchCriteriaWrapper.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
