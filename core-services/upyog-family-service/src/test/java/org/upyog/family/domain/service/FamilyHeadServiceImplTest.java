package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.validator.AssignHofValidator;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyHeadServiceImplTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private AssignHofValidator assignHofValidator;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private FamilyConfiguration configuration;

    @InjectMocks
    private FamilyHeadServiceImpl headService;

    private Family activeFamily;

    @BeforeEach
    void setUp() {
        FamilyMember head = FamilyMember.builder()
                .userUuid("head-uuid-1")
                .isHead(true)
                .active(true)
                .relationshipWithHead("HEAD")
                .auditDetails(AuditDetails.builder().createdBy("sys").build())
                .build();

        FamilyMember member2 = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .isHead(false)
                .active(true)
                .relationshipWithHead("SPOUSE")
                .auditDetails(AuditDetails.builder().createdBy("sys").build())
                .build();

        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .members(new ArrayList<>(Arrays.asList(head, member2)))
                .auditDetails(AuditDetails.builder().createdBy("system").createdTime(1000L).build())
                .build();

        org.mockito.Mockito.lenient().when(configuration.getAssignHofTopic()).thenReturn("assign-ug-family-hof");
    }

    @Test
    @DisplayName("Should transfer HoF role and re-point head flags")
    void assignHeadOfFamily_Success() {
        AssignHofRequest request = AssignHofRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .newHeadUserUuid("member-uuid-2")
                .reason("Senior citizen transfer")
                .build();

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.singletonList(activeFamily));
        when(familyRepository.update(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Family result = headService.assignHeadOfFamily(request);

        assertNotNull(result);
        assertEquals("member-uuid-2", result.getHeadUserUuid());

        FamilyMember oldHead = result.getMembers().stream().filter(m -> "head-uuid-1".equals(m.getUserUuid())).findFirst().orElse(null);
        FamilyMember newHead = result.getMembers().stream().filter(m -> "member-uuid-2".equals(m.getUserUuid())).findFirst().orElse(null);

        assertNotNull(oldHead);
        assertNotNull(newHead);
        assertFalse(oldHead.getIsHead());
        assertTrue(newHead.getIsHead());
        assertEquals("HEAD", newHead.getRelationshipWithHead());
        verify(kafkaProducer).push(any(), any());
    }
}
