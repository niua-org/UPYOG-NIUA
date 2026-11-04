// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.web.model.elasticsearch;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class InboxElasticSearchRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new InboxElasticSearchRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(InboxElasticSearchRequest.builder().build()).isNotNull();
}

    }
