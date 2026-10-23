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
import org.upyog.family.domain.model.ExitReason;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.model.MemberStatus;
import org.upyog.family.domain.validator.AddMemberValidator;
import org.upyog.family.domain.validator.JoinFamilyValidator;
import org.upyog.family.domain.validator.LeaveFamilyValidator;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.upyog.family.web.contract.LeaveFamilyRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyMemberServiceImplTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private AddMemberValidator addMemberValidator;

    @Mock
    private JoinFamilyValidator joinFamilyValidator;

    @Mock
    private LeaveFamilyValidator leaveFamilyValidator;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private FamilyConfiguration configuration;

    @InjectMocks
    private FamilyMemberServiceImpl memberService;

    private Family activeFamily;

    @BeforeEach
    void setUp() {
        FamilyMember head = FamilyMember.builder()
                .userUuid("head-uuid-1")
                .isHead(true)
                .active(true)
                .status(MemberStatus.ACTIVE)
                .build();

        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .members(new ArrayList<>(Collections.singletonList(head)))
                .auditDetails(AuditDetails.builder().createdBy("system").createdTime(1000L).build())
                .build();

        org.mockito.Mockito.lenient().when(configuration.getAddMemberTopic()).thenReturn("add-ug-family-member");
        org.mockito.Mockito.lenient().when(configuration.getLeaveFamilyTopic()).thenReturn("leave-ug-family-member");
    }

    @Test
    @DisplayName("Should successfully add a member and publish kafka event")
    void addMember_Success() {
        FamilyMember newMember = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .relationshipWithHead("SPOUSE")
                .build();

        AddMemberRequest request = AddMemberRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .member(newMember)
                .consentToken("CONSENT-VERIFIED-12345")
                .build();

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.singletonList(activeFamily));
        when(familyRepository.update(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Family result = memberService.addMember(request);

        assertNotNull(result);
        assertEquals(2, result.getMembers().size());
        FamilyMember added = result.getMembers().get(1);
        assertEquals("member-uuid-2", added.getUserUuid());
        assertTrue(added.getActive());
        assertEquals(MemberStatus.ACTIVE, added.getStatus());
        verify(kafkaProducer).push(any(), any());
    }

    @Test
    @DisplayName("Should successfully handle join family request")
    void joinFamily_Success() {
        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-3")
                .relationshipWithHead("DAUGHTER")
                .hofConsentOtp("OTP-1234")
                .build();

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.singletonList(activeFamily));
        when(familyRepository.update(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Family result = memberService.joinFamily(request);

        assertNotNull(result);
        assertEquals(2, result.getMembers().size());
        FamilyMember joined = result.getMembers().get(1);
        assertEquals("applicant-uuid-3", joined.getUserUuid());
        assertEquals("DAUGHTER", joined.getRelationshipWithHead());
        verify(kafkaProducer).push(any(), any());
    }

    @Test
    @DisplayName("Should successfully process leave family and set exit status")
    void leaveFamily_Success() {
        FamilyMember member2 = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .isHead(false)
                .active(true)
                .status(MemberStatus.ACTIVE)
                .build();
        activeFamily.getMembers().add(member2);

        LeaveFamilyRequest request = LeaveFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .memberUserUuid("member-uuid-2")
                .exitReason(ExitReason.MARRIAGE)
                .build();

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.singletonList(activeFamily));
        when(familyRepository.update(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Family result = memberService.leaveFamily(request);

        assertNotNull(result);
        FamilyMember leftMember = result.getMembers().stream()
                .filter(m -> "member-uuid-2".equals(m.getUserUuid()))
                .findFirst().orElse(null);

        assertNotNull(leftMember);
        assertFalse(leftMember.getActive());
        assertEquals(MemberStatus.EXITED, leftMember.getStatus());
        assertEquals(ExitReason.MARRIAGE, leftMember.getExitReason());
        verify(kafkaProducer).push(any(), any());
    }

    @Test
    @DisplayName("Should auto-lookup or create citizen profile when userUuid is null and mobile is supplied")
    void addMember_AutoProvisionByMobile_Success() {
        FamilyMember newMember = FamilyMember.builder()
                .mobileNumber("9876543210")
                .name("Dependent Child")
                .gender("MALE")
                .relationshipWithHead("SON")
                .build();

        AddMemberRequest request = AddMemberRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .member(newMember)
                .consentToken("CONSENT-VERIFIED-999")
                .build();

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.singletonList(activeFamily));
        when(userClient.searchUsersByMobile(any(), any(), any())).thenReturn(Collections.emptyList());
        when(userClient.createCitizenProfile(any(), any())).thenReturn(org.upyog.family.web.contract.CitizenProfile.builder()
                .uuid("provisioned-uuid-99")
                .name("Dependent Child")
                .mobileNumber("9876543210")
                .build());
        when(familyRepository.update(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Family result = memberService.addMember(request);

        assertNotNull(result);
        assertEquals(2, result.getMembers().size());
        FamilyMember added = result.getMembers().get(1);
        assertEquals("provisioned-uuid-99", added.getUserUuid());
        assertTrue(added.getActive());
        verify(userClient).createCitizenProfile(any(), any());
    }
}
