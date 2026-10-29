package org.upyog.draft.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.upyog.draft.config.DraftConfiguration;
import org.upyog.draft.producer.Producer;
import org.upyog.draft.repository.query.DraftQueryBuilder;
import org.upyog.draft.repository.rowmapper.DraftRowMapper;
import org.upyog.draft.util.DraftConstants;
import org.upyog.draft.web.models.DraftDetail;
import org.upyog.draft.web.models.DraftRequest;
import org.upyog.draft.web.models.DraftSearchCriteria;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DraftRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private DraftQueryBuilder queryBuilder;
    @Mock
    private DraftRowMapper rowMapper;
    @Mock
    private Producer producer;
    @Mock
    private DraftConfiguration configuration;

    @InjectMocks
    private DraftRepository repository;

    @BeforeEach
    void stubTopics() {
        lenient().when(configuration.getSaveDraftTopic()).thenReturn("save-draft");
        lenient().when(configuration.getUpdateDraftTopic()).thenReturn("update-draft");
        lenient().when(configuration.getUpdateDraftStatusTopic()).thenReturn("update-status");
        lenient().when(configuration.getDeleteDraftTopic()).thenReturn("delete-draft");
    }

    @Test
    void save_isCreateTrue_pushesToSaveTopic() {
        DraftRequest request = DraftRequest.builder().build();

        repository.save(request, true);

        verify(producer).push("save-draft", request);
    }

    @Test
    void save_isCreateFalse_pushesToUpdateTopic() {
        DraftRequest request = DraftRequest.builder().build();

        repository.save(request, false);

        verify(producer).push("update-draft", request);
    }

    @Test
    void updateStatus_pushesStatusTopic() {
        DraftRequest request = DraftRequest.builder().build();

        repository.updateStatus(request);

        verify(producer).push("update-status", request);
    }

    @Test
    void delete_pushesDeleteTopic() {
        DraftRequest request = DraftRequest.builder().build();

        repository.delete(request);

        verify(producer).push("delete-draft", request);
    }

    @Test
    void search_buildsQueryAndMapsRows() {
        DraftSearchCriteria criteria = DraftSearchCriteria.builder().tenantId("pb.amritsar").build();
        DraftDetail row = DraftDetail.builder().draftId("d-1").build();
        when(queryBuilder.getSearchQuery(eq(criteria), any())).thenReturn("SELECT 1");
        when(jdbcTemplate.query(eq("SELECT 1"), eq(rowMapper), any(Object[].class))).thenReturn(List.of(row));

        List<DraftDetail> result = repository.search(criteria);

        assertThat(result).containsExactly(row);
        verify(queryBuilder).getSearchQuery(eq(criteria), any());
    }

    @Test
    void findByDraftId_noRows_returnsNull() {
        when(jdbcTemplate.query(anyString(), eq(rowMapper), eq("d-1"), eq("pb"), eq("user-1")))
                .thenReturn(Collections.emptyList());

        assertThat(repository.findByDraftId("d-1", "pb", "user-1")).isNull();
    }

    @Test
    void findByDraftId_singleRow_returnsFirst() {
        DraftDetail draft = DraftDetail.builder().draftId("d-1").build();
        when(jdbcTemplate.query(anyString(), eq(rowMapper), eq("d-1"), eq("pb"), eq("user-1")))
                .thenReturn(List.of(draft));

        assertThat(repository.findByDraftId("d-1", "pb", "user-1")).isSameAs(draft);
    }

    @Test
    void count_nullJdbcResult_returnsZero() {
        DraftSearchCriteria criteria = DraftSearchCriteria.builder().build();
        when(queryBuilder.getCountQuery(eq(criteria), any())).thenReturn("SELECT COUNT(*)");
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*)"), eq(Integer.class), any(Object[].class)))
                .thenReturn(null);

        assertThat(repository.count(criteria)).isZero();
    }

    @Test
    void findActiveDraftsOlderThan_passesActiveStatusAndCutoff() {
        when(jdbcTemplate.query(anyString(), eq(rowMapper), eq(DraftConstants.STATUS_ACTIVE), eq(99L)))
                .thenReturn(List.of());

        repository.findActiveDraftsOlderThan(99L);

        verify(jdbcTemplate).query(anyString(), eq(rowMapper), eq(DraftConstants.STATUS_ACTIVE), eq(99L));
    }
}
