package com.ingestpipeline.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticServiceTest {

    private final ElasticService elasticService = new ElasticService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(elasticService, "userName", "elastic");
        ReflectionTestUtils.setField(elasticService, "password", "secret");
    }

    @Test
    void getESEncodedCredentials_returnsBasicAuthHeaderValue() {
        assertThat(elasticService.getESEncodedCredentials()).startsWith("Basic ");
    }

    @Test
    void searchQueryAccessors_roundTrip() {
        elasticService.setSearchQueryCollection("collection-q");
        elasticService.setSearchQueryBilling("billing-q");
        elasticService.setSearchQueryPayment("payment-q");

        assertThat(elasticService.getSearchQueryCollection()).isEqualTo("collection-q");
        assertThat(elasticService.getSearchQueryBilling()).isEqualTo("billing-q");
        assertThat(elasticService.getSearchQueryPayment()).isEqualTo("payment-q");
    }
}
