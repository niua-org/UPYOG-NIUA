package org.egov.user.domain.service.document;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.persistence.repository.FileStoreRepository;
import org.egov.user.persistence.repository.IdGenRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.document.UserDocumentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDocumentServiceImplTest {

    @Mock
    private UserDocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStoreRepository fileStoreRepository;

    private UserDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDocumentServiceImpl(documentRepository, userRepository, fileStoreRepository);
    }

    @Test
    void createDocuments_emptyPayload_throwsCustomException() {
        UserDocumentRequest request = new UserDocumentRequest();
        request.setRequestInfo(RequestInfo.builder().build());

        assertThatThrownBy(() -> service.createDocuments(request)).isInstanceOf(CustomException.class);
    }

    @Test
    void createDocuments_validRequest_persistsDocuments() throws Exception {
        UserDocument doc = UserDocument.builder()
                .documentType("AADHAAR")
                .filestoreId("fs-1")
                .documentUid("1234")
                .build();
        UserDocumentRequest request = UserDocumentRequest.builder()
                .requestInfo(RequestInfo.builder()
                        .userInfo(User.builder().id(99L).uuid("op-uuid").build())
                        .build())
                .userId(10L)
                .uuid("user-uuid")
                .tenantId("pg.citya")
                .documents(List.of(doc))
                .build();

        lenient().when(userRepository.getUserIdByUuid("user-uuid")).thenReturn(10L);
        lenient().when(userRepository.getTenantIdByUserId(10L)).thenReturn("pg.citya");
        lenient().when(userRepository.getTenantIdByUuid("user-uuid")).thenReturn("pg.citya");
        doReturn(Collections.singletonMap("fs-1", "http://filestore/fs-1"))
                .when(fileStoreRepository).getUrlByFileStoreId(eq("pg.citya"), eq(Collections.singletonList("fs-1")));
        when(documentRepository.create(any(UserDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        List<UserDocument> created = service.createDocuments(request);

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getUserId()).isEqualTo(10L);
        assertThat(created.get(0).getUserUuid()).isEqualTo("user-uuid");
        verify(documentRepository).create(any(UserDocument.class));
    }

    @Test
    void updateDocument_nullBody_throwsCustomException() {
        assertThatThrownBy(() -> service.updateDocument(null)).isInstanceOf(CustomException.class);
    }

    @Test
    void searchDocuments_nullCriteria_throwsCustomException() {
        assertThatThrownBy(() -> service.searchDocuments(null, RequestInfo.builder().build()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void createDocuments_withIDGen_generatesFormattedDocId() throws Exception {
        IdGenRepository idGenRepository = mock(IdGenRepository.class);
        when(idGenRepository.generateUserDocumentId(any(), eq("pg.citya"))).thenReturn("DOC-2026-000001");

        UserDocumentServiceImpl serviceWithIdGen = new UserDocumentServiceImpl(
                documentRepository, userRepository, fileStoreRepository, null, idGenRepository);

        UserDocument doc = UserDocument.builder()
                .documentType("AADHAAR")
                .filestoreId("fs-1")
                .documentUid("1234")
                .build();
        UserDocumentRequest request = UserDocumentRequest.builder()
                .requestInfo(RequestInfo.builder()
                        .userInfo(User.builder().id(99L).uuid("op-uuid").build())
                        .build())
                .userId(10L)
                .uuid("user-uuid")
                .tenantId("pg.citya")
                .documents(List.of(doc))
                .build();

        lenient().when(userRepository.getUserIdByUuid("user-uuid")).thenReturn(10L);
        lenient().when(userRepository.getTenantIdByUserId(10L)).thenReturn("pg.citya");
        lenient().when(userRepository.getTenantIdByUuid("user-uuid")).thenReturn("pg.citya");
        doReturn(Collections.singletonMap("fs-1", "http://filestore/fs-1"))
                .when(fileStoreRepository).getUrlByFileStoreId(eq("pg.citya"), eq(Collections.singletonList("fs-1")));
        when(documentRepository.create(any(UserDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        List<UserDocument> created = serviceWithIdGen.createDocuments(request);

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getId()).isEqualTo("DOC-2026-000001");
        verify(idGenRepository).generateUserDocumentId(any(), eq("pg.citya"));
    }
}
