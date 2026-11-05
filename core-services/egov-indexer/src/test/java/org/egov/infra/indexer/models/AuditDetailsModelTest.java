package org.egov.infra.indexer.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditDetailsModelTest {

    @Test
    void builder_andEquality() {
        AuditDetails left = AuditDetails.builder()
                .createdBy("u1")
                .createdTime(1L)
                .lastModifiedBy("u1")
                .lastModifiedTime(2L)
                .build();
        AuditDetails right = AuditDetails.builder()
                .createdBy("u1")
                .createdTime(1L)
                .lastModifiedBy("u1")
                .lastModifiedTime(2L)
                .build();
        AuditDetails different = AuditDetails.builder().createdBy("u2").build();

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
        assertThat(left).isNotEqualTo(different);
        assertThat(left.toString()).contains("u1");
    }
}
