package org.upyog.family.persistence.repository;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import org.upyog.family.config.FamilyConfiguration;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdGenRepositoryImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FamilyConfiguration configuration;

    private IdGenRepositoryImpl idGenRepository;

    @BeforeEach
    void setUp() {
        idGenRepository = new IdGenRepositoryImpl(restTemplate, configuration);
    }

    @Test
    @DisplayName("generateFamilyId: Successfully generates single family ID via IDGen")
    void testGenerateFamilyId_Success() {
        when(configuration.getIdGenHost()).thenReturn("http://localhost:8086");
        when(configuration.getIdGenPath()).thenReturn("/egov-idgen/id/_generate");
        when(configuration.getIdGenFamilyIdName()).thenReturn("family.id");
        when(configuration.getIdGenFamilyIdFormat()).thenReturn("FAM-[cy:yyyy]-[seq_family_id]");

        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> idRes = new HashMap<>();
        idRes.put("id", "FAM-2026-000001");
        mockResponse.put("idResponses", Collections.singletonList(idRes));

        when(restTemplate.postForObject(eq("http://localhost:8086/egov-idgen/id/_generate"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        String familyId = idGenRepository.generateFamilyId(new RequestInfo(), "pg.citya");

        assertEquals("FAM-2026-000001", familyId);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        List<Map<String, Object>> requests = (List<Map<String, Object>>) body.get("idRequests");
        assertEquals(1, requests.size());
        assertEquals("family.id", requests.get(0).get("idName"));
        assertEquals("pg", requests.get(0).get("tenantId"));
    }

    @Test
    @DisplayName("generateMemberIds: Successfully generates multiple member IDs in bulk via IDGen")
    void testGenerateMemberIds_Success() {
        when(configuration.getIdGenHost()).thenReturn("http://localhost:8086");
        when(configuration.getIdGenPath()).thenReturn("/egov-idgen/id/_generate");
        when(configuration.getIdGenMemberIdName()).thenReturn("family.member.id");
        when(configuration.getIdGenMemberIdFormat()).thenReturn("MEM-[cy:yyyy]-[seq_family_member_id]");

        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> idRes1 = new HashMap<>();
        idRes1.put("id", "MEM-2026-000001");
        Map<String, Object> idRes2 = new HashMap<>();
        idRes2.put("id", "MEM-2026-000002");
        mockResponse.put("idResponses", Arrays.asList(idRes1, idRes2));

        when(restTemplate.postForObject(eq("http://localhost:8086/egov-idgen/id/_generate"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        List<String> memberIds = idGenRepository.generateMemberIds(new RequestInfo(), "pg.citya", 2);

        assertEquals(2, memberIds.size());
        assertEquals("MEM-2026-000001", memberIds.get(0));
        assertEquals("MEM-2026-000002", memberIds.get(1));
    }

    @Test
    @DisplayName("generateFamilyId: Throws CustomException when host is missing")
    void testGenerateFamilyId_MissingHost_ThrowsCustomException() {
        when(configuration.getIdGenFamilyIdName()).thenReturn("family.id");
        when(configuration.getIdGenHost()).thenReturn("");

        assertThrows(CustomException.class, () -> idGenRepository.generateFamilyId(new RequestInfo(), "pg.citya"));
    }

    @Test
    @DisplayName("generateMemberIds: Throws CustomException when member id name is missing")
    void testGenerateMemberIds_MissingMemberIdName_ThrowsCustomException() {
        when(configuration.getIdGenMemberIdName()).thenReturn(null);

        assertThrows(CustomException.class, () -> idGenRepository.generateMemberIds(new RequestInfo(), "pg.citya", 2));
    }

    @Test
    @DisplayName("getIdList: Throws CustomException when IDGen service returns empty response")
    void testGetIdList_EmptyResponse_ThrowsCustomException() {
        when(configuration.getIdGenHost()).thenReturn("http://localhost:8086");
        when(configuration.getIdGenPath()).thenReturn("/egov-idgen/id/_generate");

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Collections.emptyMap());

        assertThrows(CustomException.class, () ->
                idGenRepository.getIdList(new RequestInfo(), "pg.citya", "family.id", null, 1));
    }
}
