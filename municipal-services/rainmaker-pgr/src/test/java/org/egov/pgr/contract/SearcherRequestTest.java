// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.contract;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class SearcherRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new SearcherRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(SearcherRequest.builder().build()).isNotNull();
}

    }
