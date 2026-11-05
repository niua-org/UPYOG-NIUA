package org.egov.garbageservice.web.models.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SMSRequest {
    private String mobileNumber;
    private String message;
    private String category;
    private String templateId;
}
