package org.egov.dx.upyogdata.pfms.models;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SchedulerLog {
    private String schedulerType;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private long durationMs;
    private String status;
    private int totalPicked;
    private int successCount;
    private int failedCount;
    private String createdBy;
}
