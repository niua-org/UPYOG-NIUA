package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserSearchCriteriaTest {
    @Test
    void classLoads() {
        assertThat(UserSearchCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UserSearchCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
