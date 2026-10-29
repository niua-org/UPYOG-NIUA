package org.upyog.draft.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.upyog.draft.config.DraftConfiguration;
import org.upyog.draft.service.DraftService;
import org.upyog.draft.util.DraftConstants;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftCleanupSchedulerTest {

    @Mock
    private DraftService draftService;
    @Mock
    private DraftConfiguration configuration;

    @InjectMocks
    private DraftCleanupScheduler scheduler;

    @BeforeEach
    void enableScheduler() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        lenient().when(configuration.getActiveTtlDays()).thenReturn(30);
        lenient().when(configuration.getSubmittedRetentionDays()).thenReturn(90);
        lenient().when(configuration.getDiscardedRetentionDays()).thenReturn(7);
        lenient().when(configuration.isOrphanReconciliationEnabled()).thenReturn(true);
        lenient().when(draftService.purgeActiveOlderThan(anyLong())).thenReturn(1);
        lenient().when(draftService.purgeByStatusOlderThan(eq(DraftConstants.STATUS_SUBMITTED), anyLong())).thenReturn(2);
        lenient().when(draftService.purgeByStatusOlderThan(eq(DraftConstants.STATUS_DISCARDED), anyLong())).thenReturn(3);
        lenient().when(draftService.reconcileOrphanedDrafts()).thenReturn(4);
    }

    @Test
    void runCleanup_enabled_invokesAllPurgeAndReconcileSteps() {
        scheduler.runCleanup();

        verify(draftService).purgeActiveOlderThan(anyLong());
        verify(draftService).purgeByStatusOlderThan(eq(DraftConstants.STATUS_SUBMITTED), anyLong());
        verify(draftService).purgeByStatusOlderThan(eq(DraftConstants.STATUS_DISCARDED), anyLong());
        verify(draftService).reconcileOrphanedDrafts();
    }

    @Test
    void runCleanup_disabled_skipsDraftService() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.runCleanup();

        verify(draftService, never()).purgeActiveOlderThan(anyLong());
        verify(draftService, never()).reconcileOrphanedDrafts();
    }

    @Test
    void runCleanup_orphanReconciliationDisabled_skipsReconcile() {
        when(configuration.isOrphanReconciliationEnabled()).thenReturn(false);

        scheduler.runCleanup();

        verify(draftService).purgeActiveOlderThan(anyLong());
        verify(draftService, never()).reconcileOrphanedDrafts();
    }
}
