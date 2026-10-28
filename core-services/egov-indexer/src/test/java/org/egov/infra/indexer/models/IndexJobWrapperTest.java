package org.egov.infra.indexer.models;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndexJobWrapperTest {

    @Test
    void builderAndEquality() {
        IndexJob job = IndexJob.builder().jobId("j1").build();
        IndexJobWrapper left = IndexJobWrapper.builder()
                .requestInfo(RequestInfo.builder().apiId("indexer").build())
                .job(job)
                .build();
        IndexJobWrapper right = IndexJobWrapper.builder()
                .requestInfo(RequestInfo.builder().apiId("indexer").build())
                .job(job)
                .build();

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
        assertThat(left.getJob().getJobId()).isEqualTo("j1");
    }
}
