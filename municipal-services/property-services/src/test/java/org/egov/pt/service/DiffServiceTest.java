package org.egov.pt.service;

import org.egov.pt.models.Property;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiffServiceTest {

    private final DiffService diffService = new DiffService();

    @Test
    void getUpdatedFields_whenLandAreaChanges_includesLandArea() {
        Property fromDb = Property.builder().tenantId("pb.amritsar").landArea(100.0).build();
        Property update = Property.builder().tenantId("pb.amritsar").landArea(200.0).build();

        List<String> updated = diffService.getUpdatedFields(update, fromDb, "");

        assertThat(updated).contains("landArea");
    }

    @Test
    void getUpdatedFields_whenIdentical_returnsEmptyList() {
        Property fromDb = Property.builder().tenantId("pb.amritsar").landArea(100.0).build();
        Property update = Property.builder().tenantId("pb.amritsar").landArea(100.0).build();

        List<String> updated = diffService.getUpdatedFields(update, fromDb, "");

        assertThat(updated).isEmpty();
    }

}
