package org.egov.inbox.service.V2.validator;

import org.egov.inbox.util.MDMSUtil;
import org.egov.inbox.web.model.InboxRequest;
import org.egov.inbox.web.model.InboxSearchCriteria;
import org.egov.inbox.web.model.V2.InboxQueryConfiguration;
import org.egov.inbox.web.model.V2.SearchParam;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatorDefaultImplementationTest {

    @Mock
    private MDMSUtil mdmsUtil;

    @InjectMocks
    private ValidatorDefaultImplementation validator;

    @Test
    void validateSearchCriteria_whenMandatoryFieldMissing_throws() {
        when(mdmsUtil.getConfigFromMDMS(anyString(), anyString())).thenReturn(InboxQueryConfiguration.builder()
                .allowedSearchCriteria(List.of(
                        SearchParam.builder().name("applicationNumber").isMandatory(true).build()))
                .build());

        InboxSearchCriteria inbox = new InboxSearchCriteria();
        inbox.setTenantId("pb.amritsar");
        ProcessInstanceSearchCriteria processCriteria = new ProcessInstanceSearchCriteria();
        processCriteria.setModuleName("pt-services");
        inbox.setProcessSearchCriteria(processCriteria);
        inbox.setModuleSearchCriteria(new HashMap<>());

        InboxRequest request = InboxRequest.builder().inbox(inbox).build();

        assertThatThrownBy(() -> validator.validateSearchCriteria(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateSearchCriteria_whenMandatoryFieldsPresent_passes() {
        when(mdmsUtil.getConfigFromMDMS(anyString(), anyString())).thenReturn(InboxQueryConfiguration.builder()
                .allowedSearchCriteria(List.of(
                        SearchParam.builder().name("applicationNumber").isMandatory(true).build()))
                .build());

        InboxSearchCriteria inbox = new InboxSearchCriteria();
        inbox.setTenantId("pb.amritsar");
        ProcessInstanceSearchCriteria processCriteria = new ProcessInstanceSearchCriteria();
        processCriteria.setModuleName("pt-services");
        inbox.setProcessSearchCriteria(processCriteria);
        HashMap<String, Object> moduleCriteria = new HashMap<>();
        moduleCriteria.put("applicationNumber", "PT-001");
        inbox.setModuleSearchCriteria(moduleCriteria);

        InboxRequest request = InboxRequest.builder().inbox(inbox).build();

        assertThatCode(() -> validator.validateSearchCriteria(request)).doesNotThrowAnyException();
    }
}
