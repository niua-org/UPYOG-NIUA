package org.egov.pt.consumer;

import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.Property;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.service.AssessmentNotificationService;
import org.egov.pt.service.NotificationService;
import org.egov.pt.web.contracts.PropertyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationConsumerTest {

    @Mock private com.fasterxml.jackson.databind.ObjectMapper mapper;
    @Mock private PropertyConfiguration configs;
    @Mock private AssessmentNotificationService assessmentNotificationService;
    @Mock private NotificationService notifService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void listen_whenSavePropertyTopic_callsUpdateNotification() throws Exception {
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .creationReason(CreationReason.UPDATE)
                .build();
        PropertyRequest request = PropertyRequest.builder().property(property).build();
        when(configs.getSavePropertyTopic()).thenReturn("save-property");
        when(configs.getUpdatePropertyTopic()).thenReturn("update-property");
        when(configs.getCreateAssessmentTopic()).thenReturn("create-assessment");
        when(configs.getUpdateAssessmentTopic()).thenReturn("update-assessment");
        when(mapper.convertValue(any(), eq(PropertyRequest.class))).thenReturn(request);

        notificationConsumer.listen(new HashMap<>(), "tenant.save-property");

        verify(notifService).sendNotificationForUpdate(request);
    }

    @Test
    void listen_whenMutationTopic_callsMutationNotification() throws Exception {
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .creationReason(CreationReason.MUTATION)
                .build();
        PropertyRequest request = PropertyRequest.builder().property(property).build();
        when(configs.getSavePropertyTopic()).thenReturn("save-property");
        when(configs.getUpdatePropertyTopic()).thenReturn("update-property");
        when(configs.getCreateAssessmentTopic()).thenReturn("create-assessment");
        when(configs.getUpdateAssessmentTopic()).thenReturn("update-assessment");
        when(mapper.convertValue(any(), eq(PropertyRequest.class))).thenReturn(request);

        notificationConsumer.listen(new HashMap<>(), "tenant.update-property");

        verify(notifService).sendNotificationForMutation(request);
    }
}
