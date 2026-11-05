package org.egov.infra.indexer.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndexJobModelTest {

    @Test
    void statusEnum_roundTripsKnownValues() {
        assertThat(IndexJob.StatusEnum.fromValue("INPROGRESS")).isEqualTo(IndexJob.StatusEnum.INPROGRESS);
        assertThat(IndexJob.StatusEnum.fromValue("completed")).isEqualTo(IndexJob.StatusEnum.COMPLETED);
        assertThat(IndexJob.StatusEnum.fromValue("FAILED")).isEqualTo(IndexJob.StatusEnum.FAILED);
        assertThat(IndexJob.StatusEnum.fromValue("unknown")).isNull();
    }

    @Test
    void statusEnum_toString_returnsValue() {
        assertThat(IndexJob.StatusEnum.INPROGRESS.toString()).isEqualTo("INPROGRESS");
    }

    @Test
    void builder_populatesFields() {
        IndexJob job = IndexJob.builder()
                .jobId("job-1")
                .jobStatus(IndexJob.StatusEnum.INPROGRESS)
                .recordsToBeIndexed(10)
                .totalRecordsIndexed(5)
                .build();

        assertThat(job.getJobId()).isEqualTo("job-1");
        assertThat(job.getJobStatus()).isEqualTo(IndexJob.StatusEnum.INPROGRESS);
        assertThat(job.getRecordsToBeIndexed()).isEqualTo(10);
        assertThat(job.getTotalRecordsIndexed()).isEqualTo(5);
    }

    @Test
    void equalsAndHashCode_considersCoreFields() {
        IndexJob left = IndexJob.builder().jobId("j").tenantId("t1").jobStatus(IndexJob.StatusEnum.FAILED).build();
        IndexJob same = IndexJob.builder().jobId("j").tenantId("t1").jobStatus(IndexJob.StatusEnum.FAILED).build();
        IndexJob other = IndexJob.builder().jobId("j2").tenantId("t1").build();

        assertThat(left).isEqualTo(same);
        assertThat(left.hashCode()).isEqualTo(same.hashCode());
        assertThat(left).isNotEqualTo(other);
        assertThat(left).isNotEqualTo(null);
        assertThat(left).isNotEqualTo("string");
    }
}
