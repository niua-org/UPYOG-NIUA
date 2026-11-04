package org.egov.garbageservice.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.egov.garbageservice.kafka.GarbageProducer;
import org.egov.garbageservice.repository.builder.GarbageAccountQueryBuilder;
import org.egov.garbageservice.repository.rowmapper.GarbageAccountRowMapper;
import org.egov.garbageservice.web.models.GarbageAccount;
import org.egov.garbageservice.web.models.SearchCriteriaGarbageAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central repository component for garbage account database queries and Kafka publishing.
 */
@Repository
@Slf4j
public class GarbageAccountRepository {

    @Autowired
    private GarbageProducer producer;

    @Autowired
    private GarbageAccountRowMapper garbageAccountRowMapper;

    private final JdbcTemplate jdbcTemplate;
    private final GarbageAccountQueryBuilder queryBuilder;

    @Autowired
    public GarbageAccountRepository(JdbcTemplate jdbcTemplate, GarbageAccountQueryBuilder queryBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryBuilder = queryBuilder;
    }

    /**
     * Retrieves the next available sequence value for a garbage account.
     *
     * @return the generated sequence ID
     */
    public Long getNextSequence() {
        return jdbcTemplate.queryForObject(GarbageAccountQueryBuilder.SELECT_NEXT_SEQUENCE, Long.class);
    }

    /**
     * Retrieves the next available business garbage ID from the sequence.
     *
     * @return the generated garbage ID
     */
    public Long getNextGarbageId() {
        return jdbcTemplate.queryForObject(GarbageAccountQueryBuilder.SELECT_NEXT_GARBAGE_ID, Long.class);
    }

    /**
     * Searches database for garbage accounts matching the provided criteria.
     *
     * @param searchCriteriaGarbageAccount the filter criteria defining search boundaries
     * @param garbageCriteriaMap           optional map of indexed search criteria
     * @return list of matched {@link GarbageAccount} objects
     */
    public List<GarbageAccount> searchGarbageAccount(SearchCriteriaGarbageAccount searchCriteriaGarbageAccount,
                                                     Map<Integer, SearchCriteriaGarbageAccount> garbageCriteriaMap) {
        StringBuilder searchQuery = null;
        final List<Object> preparedStatementValues = new ArrayList<>();

        searchQuery = queryBuilder.getSearchQueryByCriteria(searchQuery, searchCriteriaGarbageAccount, preparedStatementValues, garbageCriteriaMap);

        log.info("Executing database search with query: {} and parameters: {}", searchQuery, preparedStatementValues);

        List<GarbageAccount> garbageAccounts = jdbcTemplate.query(searchQuery.toString(), preparedStatementValues.toArray(), garbageAccountRowMapper);

        if (!CollectionUtils.isEmpty(garbageAccounts) && searchCriteriaGarbageAccount.getIsActiveAccount() != null) {
            garbageAccounts = garbageAccounts.stream().filter(garbageAccount -> searchCriteriaGarbageAccount
                    .getIsActiveAccount().equals(garbageAccount.getIsActive())).collect(Collectors.toList());
        }

        if (searchCriteriaGarbageAccount.getIsMonthlyBilling() != null) {
            garbageAccounts = garbageAccounts.stream().filter(garbageAccount -> searchCriteriaGarbageAccount
                    .getIsMonthlyBilling().equals(garbageAccount.getGrbgCollectionUnits().get(0).getIsmonthlybilling())).collect(Collectors.toList());
        }

        garbageAccounts = garbageAccounts.stream().filter(Objects::nonNull).map(garbageAccount -> {
            if (searchCriteriaGarbageAccount.getIsActiveSubAccount() != null) {
                Optional.ofNullable(garbageAccount.getChildGarbageAccounts())
                        .filter(childAccounts -> !childAccounts.isEmpty()).ifPresent(childAccounts -> {
                            List<GarbageAccount> filteredChildren = childAccounts.stream()
                                    .filter(child -> searchCriteriaGarbageAccount.getIsActiveSubAccount()
                                            .equals(child.getIsActive()))
                                    .collect(Collectors.toList());
                            garbageAccount.setChildGarbageAccounts(filteredChildren);
                        });
            }

            if (searchCriteriaGarbageAccount.getIsMonthlyBilling() != null) {
                Optional.ofNullable(garbageAccount.getChildGarbageAccounts())
                        .filter(childAccounts -> !childAccounts.isEmpty()).ifPresent(childAccounts -> {
                            List<GarbageAccount> filteredChildren = childAccounts.stream()
                                    .filter(child -> searchCriteriaGarbageAccount.getIsMonthlyBilling()
                                            .equals(child.getGrbgCollectionUnits().get(0).getIsmonthlybilling()))
                                    .collect(Collectors.toList());
                            garbageAccount.setChildGarbageAccounts(filteredChildren);
                        });
            }
            return garbageAccount;
        }).collect(Collectors.toList());

        log.info("Database search returned {} accounts.", garbageAccounts != null ? garbageAccounts.size() : 0);
        return garbageAccounts;
    }

    /**
     * Executes an optimized query for scheduler and payment update operations.
     *
     * @param searchCriteriaGarbageAccount the filter criteria defining search boundaries
     * @return list of matched {@link GarbageAccount} objects
     */
    public List<GarbageAccount> searchV2(SearchCriteriaGarbageAccount searchCriteriaGarbageAccount) {
        StringBuilder searchQuery = null;
        final List<Object> preparedStatementValues = new ArrayList<>();

        searchQuery = queryBuilder.getSearchQueryByCriteria(searchQuery, searchCriteriaGarbageAccount, preparedStatementValues, null);

        log.info("Executing searchV2 query: {} and parameters: {}", searchQuery, preparedStatementValues);

        List<GarbageAccount> garbageAccounts = jdbcTemplate.query(searchQuery.toString(), preparedStatementValues.toArray(), garbageAccountRowMapper);

        log.info("searchV2 returned {} accounts.", garbageAccounts != null ? garbageAccounts.size() : 0);
        return garbageAccounts;
    }

    /**
     * Pushes message to Kafka topic.
     *
     * @param topic the destination Kafka topic
     * @param value the message payload to publish
     */
    public void save(String topic, Object value) {
        producer.push(topic, value);
    }
}
