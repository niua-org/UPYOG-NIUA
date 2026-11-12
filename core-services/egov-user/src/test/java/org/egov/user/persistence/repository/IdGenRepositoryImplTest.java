package org.egov.user.persistence.repository;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdGenRepositoryImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserConfiguration configuration;

    private IdGenRepositoryImpl idGenRepository;

    @BeforeEach
    void setUp() {
        idGenRepository = new IdGenRepositoryImpl(restTemplate, configuration);
    }

    @Test
    @DisplayName("generateCitizenId: Successfully generates citizen ID via IDGen")
    void testGenerateCitizenId_Success() {
        when(configuration.getIdGenHost()).thenReturn("http://localhost:8088");
        when(configuration.getIdGenPath()).thenReturn("/egov-idgen/id/_generate");
        when(configuration.getIdGenCitizenIdName()).thenReturn("user.citizen.id");
        when(configuration.getIdGenCitizenIdFormat()).thenReturn("CITIZEN-[cy:yyyy]-[seq_citizen_id]");

        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> idRes = new HashMap<>();
        idRes.put("id", "CITIZEN-2026-000001");
        mockResponse.put("idResponses", Collections.singletonList(idRes));

        when(restTemplate.postForObject(eq("http://localhost:8088/egov-idgen/id/_generate"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        String citizenId = idGenRepository.generateCitizenId(new RequestInfo(), "pg.citya");

        assertEquals("CITIZEN-2026-000001", citizenId);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        List<Map<String, Object>> requests = (List<Map<String, Object>>) body.get("idRequests");
        assertEquals(1, requests.size());
        assertEquals("user.citizen.id", requests.get(0).get("idName"));
        assertEquals("pg", requests.get(0).get("tenantId"));
    }

    @Test
    @DisplayName("generateUserDocumentId: Successfully generates user document ID via IDGen")
    void testGenerateUserDocumentId_Success() {
        when(configuration.getIdGenHost()).thenReturn("http://localhost:8088");
        when(configuration.getIdGenPath()).thenReturn("/egov-idgen/id/_generate");
        when(configuration.getIdGenUserDocIdName()).thenReturn("user.document.id");
        when(configuration.getIdGenUserDocIdFormat()).thenReturn("DOC-[cy:yyyy]-[seq_user_doc_id]");

        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> idRes = new HashMap<>();
        idRes.put("id", "DOC-2026-000001");
        mockResponse.put("idResponses", Collections.singletonList(idRes));

        when(restTemplate.postForObject(eq("http://localhost:8088/egov-idgen/id/_generate"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        String docId = idGenRepository.generateUserDocumentId(new RequestInfo(), "pg.citya");

        assertEquals("DOC-2026-000001", docId);
    }

    @Test
    @DisplayName("generateCitizenId: Throws CustomException when citizen id name is missing")
    void testGenerateCitizenId_MissingName_ThrowsCustomException() {
        when(configuration.getIdGenCitizenIdName()).thenReturn(null);

        assertThrows(CustomException.class, () -> idGenRepository.generateCitizenId(new RequestInfo(), "pg.citya"));
    }

    @Test
    @DisplayName("getIdList: Throws CustomException when IDGen host is not configured")
    void testGetIdList_MissingHost_ThrowsCustomException() {
        when(configuration.getIdGenHost()).thenReturn("");

        assertThrows(CustomException.class, () ->
                idGenRepository.getIdList(new RequestInfo(), "pg.citya", "user.citizen.id", null, 1));
    }

    @Test
    @DisplayName("getIdList: Returns empty list when count is zero or negative")
    void testGetIdList_ZeroCount_ReturnsEmptyList() {
        List<String> result = idGenRepository.getIdList(new RequestInfo(), "pg.citya", "user.citizen.id", null, 0);

        assertTrue(result.isEmpty());
        verifyNoInteractions(restTemplate);
    }
}
