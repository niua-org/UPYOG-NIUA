package org.upyog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.config.MainConfiguration;
import org.upyog.config.ModuleConfig;
import org.upyog.mapper.CommonDetailsMapper;
import org.upyog.mapper.CommonDetailsMapperFactory;
import org.upyog.mapper.PropertyDetailsMapper;
import org.upyog.repository.ServiceRequestRepository;
import org.upyog.web.models.CommonDetails;
import org.upyog.web.models.ModuleSearchCriteria;
import org.upyog.web.models.ModuleSearchRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonServiceImplTest {

    @Mock
    private CommonDetailsMapperFactory mapperFactory;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private UserService userService;
    @Mock
    private MainConfiguration mainConfiguration;
    @Mock
    private CommonDetailsMapper mapper;

    private CommonServiceImpl commonService;

    @BeforeEach
    void setUp() {
        Map<String, String> hosts = new HashMap<>();
        hosts.put("property", "http://property/");
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("property", "property/_search");
        Map<String, String> uniqueIds = new HashMap<>();
        uniqueIds.put("property", "propertyId");
        ModuleConfig moduleConfig = ModuleConfig.builder()
                .host(hosts)
                .endpoint(endpoints)
                .uniqueIdParam(uniqueIds)
                .build();
        commonService = new CommonServiceImpl(moduleConfig, mapperFactory, new ObjectMapper(),
                serviceRequestRepository, userService, mainConfiguration);
    }

    @Test
    void getApplicationCommonDetails_invalidModule_throws() {
        ModuleSearchRequest request = ModuleSearchRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .moduleSearchCriteria(ModuleSearchCriteria.builder()
                        .moduleName("unknown")
                        .applicationNumber("APP1")
                        .tenantId("pb")
                        .build())
                .build();

        assertThatThrownBy(() -> commonService.getApplicationCommonDetails(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getApplicationCommonDetails_pendingStatus_returnsNull() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(Map.of("status", "Pending"));
        when(mapperFactory.getMapper("property")).thenReturn(mapper);
        when(mapper.mapJsonToCommonDetails(any())).thenReturn(CommonDetails.builder().status("Pending").build());

        ModuleSearchRequest request = ModuleSearchRequest.builder()
                .requestInfo(RequestInfo.builder()
                        .userInfo(org.egov.common.contract.request.User.builder().uuid("user-1").build())
                        .build())
                .moduleSearchCriteria(ModuleSearchCriteria.builder()
                        .moduleName("property")
                        .applicationNumber("APP1")
                        .tenantId("pb.amritsar")
                        .build())
                .build();

        assertThat(commonService.getApplicationCommonDetails(request)).isNull();
    }

    @Test
    void getApplicationCommonDetails_activeStatus_returnsDetails() throws Exception {
        when(serviceRequestRepository.fetchResult(any(), any())).thenReturn(Map.of("status", "ACTIVE"));
        when(mapperFactory.getMapper("property")).thenReturn(new PropertyDetailsMapper());

        ModuleSearchRequest request = ModuleSearchRequest.builder()
                .requestInfo(RequestInfo.builder()
                        .userInfo(org.egov.common.contract.request.User.builder().uuid("user-1").build())
                        .build())
                .moduleSearchCriteria(ModuleSearchCriteria.builder()
                        .moduleName("property")
                        .applicationNumber("APP1")
                        .tenantId("pb.amritsar")
                        .build())
                .build();

        CommonDetails details = commonService.getApplicationCommonDetails(request);
        assertThat(details).isNotNull();
    }

    @Test
    void getApplicationCommonDetails_apiFailure_throwsCustomException() {
        when(serviceRequestRepository.fetchResult(any(), any())).thenThrow(new RuntimeException("down"));

        ModuleSearchRequest request = ModuleSearchRequest.builder()
                .requestInfo(RequestInfo.builder()
                        .userInfo(org.egov.common.contract.request.User.builder().uuid("user-1").build())
                        .build())
                .moduleSearchCriteria(ModuleSearchCriteria.builder()
                        .moduleName("property")
                        .applicationNumber("APP1")
                        .tenantId("pb.amritsar")
                        .build())
                .build();

        assertThatThrownBy(() -> commonService.getApplicationCommonDetails(request))
                .isInstanceOf(CustomException.class);
    }
}
