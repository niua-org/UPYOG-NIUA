package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.domain.model.MemberStatus;
import org.upyog.family.domain.service.impl.StateFamilySyncServiceImpl;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.StateFamilySyncRepository;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.AuditDetails;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.state.StateFamily;
import org.upyog.family.web.contract.state.StateFamilyJitProvisionRequest;
import org.upyog.family.web.contract.state.StateFamilyMember;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateFamilySyncServiceImplTest {

    @Mock
    private StateFamilySyncRepository stateFamilySyncRepository;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    private StateFamilySyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StateFamilySyncServiceImpl(stateFamilySyncRepository, familyRepository, responseInfoFactory);
    }

    @Test
    @DisplayName("Branch B: Creates family with exactly N members when family does not exist yet")
    void testJitProvision_BranchB_CreatesExactFamilyMembers() {
        String stateFamilyId = "HR-2020-12342356";
        String tenantId = "pg.citya";
        String userUuid1 = "user-uuid-member-1";

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.emptyList());

        StateFamily staged = StateFamily.builder()
                .stateFamilyId(stateFamilyId)
                .tenantId(tenantId)
                .stateCode("HR")
                .familyName("Sharma Household")
                .members(Arrays.asList(
                        StateFamilyMember.builder().memberId("MEM-1").name("Ram Sharma").mobileNumber("9876543210").isHead(true).build(),
                        StateFamilyMember.builder().memberId("MEM-2").name("Sita Sharma").mobileNumber("9876543211").isHead(false).build(),
                        StateFamilyMember.builder().memberId("MEM-3").name("Lakshman Sharma").mobileNumber("9876543212").isHead(false).build(),
                        StateFamilyMember.builder().memberId("MEM-4").name("Bharat Sharma").mobileNumber("9876543213").isHead(false).build()
                ))
                .build();
        when(stateFamilySyncRepository.findByReferenceId("REF-1")).thenReturn(staged);

        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        StateFamilyJitProvisionRequest request = StateFamilyJitProvisionRequest.builder()
                .stateFamilyId(stateFamilyId)
                .tenantId(tenantId)
                .userUuid(userUuid1)
                .referenceId("REF-1")
                .selectedMemberId("MEM-1")
                .mobileNumber("9876543210")
                .build();

        Family result = service.jitProvisionFamilyAndMember(request);

        assertNotNull(result);
        assertEquals(4, result.getMembers().size(), "Total members must equal registry member count (4)");

        // MEM-1 is verified with real userUuid
        FamilyMember mem1 = result.getMembers().stream().filter(m -> userUuid1.equals(m.getUserUuid())).findFirst().orElse(null);
        assertNotNull(mem1);
        assertTrue(mem1.getIsVerified());

        // Other members are placeholders with EXT_ prefix and isVerified=false
        long placeholderCount = result.getMembers().stream()
                .filter(m -> m.getUserUuid().startsWith("EXT_") && !Boolean.TRUE.equals(m.getIsVerified()))
                .count();
        assertEquals(3, placeholderCount, "Exactly 3 placeholder members must be created");

        verify(familyRepository, times(1)).save(any(Family.class));
    }

    @Test
    @DisplayName("Branch A: In-place update of placeholder member when Member 2 logs in (No duplicate row added)")
    void testJitProvision_BranchA_UpdatesPlaceholderInPlaceWithoutDuplicate() {
        String stateFamilyId = "HR-2020-12342356";
        String tenantId = "pg.citya";
        String userUuid1 = "user-uuid-member-1";
        String userUuid2 = "user-uuid-member-2";

        // Existing family in UPYOG with 4 members (MEM-1 linked, MEM-2/3/4 as placeholders)
        Map<String, Object> mem1Details = new HashMap<>();
        mem1Details.put("memberId", "MEM-1");
        mem1Details.put("mobileNumber", "9876543210");

        Map<String, Object> mem2Details = new HashMap<>();
        mem2Details.put("memberId", "MEM-2");
        mem2Details.put("mobileNumber", "9876543211");

        Map<String, Object> mem3Details = new HashMap<>();
        mem3Details.put("memberId", "MEM-3");

        Map<String, Object> mem4Details = new HashMap<>();
        mem4Details.put("memberId", "MEM-4");

        List<FamilyMember> existingMembers = new ArrayList<>(Arrays.asList(
                FamilyMember.builder().id("row-1").familyId(stateFamilyId).userUuid(userUuid1).isVerified(true).active(true).additionalDetails(mem1Details).build(),
                FamilyMember.builder().id("row-2").familyId(stateFamilyId).userUuid("EXT_placeholder_2").isVerified(false).active(true).additionalDetails(mem2Details).build(),
                FamilyMember.builder().id("row-3").familyId(stateFamilyId).userUuid("EXT_placeholder_3").isVerified(false).active(true).additionalDetails(mem3Details).build(),
                FamilyMember.builder().id("row-4").familyId(stateFamilyId).userUuid("EXT_placeholder_4").isVerified(false).active(true).additionalDetails(mem4Details).build()
        ));

        Family existingFamily = Family.builder()
                .familyId(stateFamilyId)
                .tenantId(tenantId)
                .headUserUuid(userUuid1)
                .members(existingMembers)
                .auditDetails(AuditDetails.builder().createdBy(userUuid1).createdTime(1000L).build())
                .build();

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.singletonList(existingFamily));
        when(familyRepository.update(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        // Member 2 logs in
        StateFamilyJitProvisionRequest request = StateFamilyJitProvisionRequest.builder()
                .stateFamilyId(stateFamilyId)
                .tenantId(tenantId)
                .userUuid(userUuid2)
                .referenceId("REF-2")
                .selectedMemberId("MEM-2")
                .mobileNumber("9876543211")
                .build();

        Family result = service.jitProvisionFamilyAndMember(request);

        assertNotNull(result);
        assertEquals(4, result.getMembers().size(), "Total members must REMAIN 4, no 5th row created!");

        // Verify row-2 was updated in place
        FamilyMember updatedMember = result.getMembers().stream().filter(m -> "row-2".equals(m.getId())).findFirst().orElse(null);
        assertNotNull(updatedMember);
        assertEquals(userUuid2, updatedMember.getUserUuid(), "userUuid must be updated to Member 2's UUID");
        assertTrue(updatedMember.getIsVerified(), "isVerified must become true");
        assertEquals(MemberStatus.ACTIVE, updatedMember.getStatus());

        ArgumentCaptor<Family> captor = ArgumentCaptor.forClass(Family.class);
        verify(familyRepository, times(1)).update(captor.capture());
        assertEquals(4, captor.getValue().getMembers().size());
    }

    @Test
    @DisplayName("Branch A: Already mapped user returns family without DB write")
    void testJitProvision_AlreadyMappedUser_ReturnsDirectly() {
        String stateFamilyId = "HR-2020-12342356";
        String tenantId = "pg.citya";
        String userUuid1 = "user-uuid-member-1";

        List<FamilyMember> existingMembers = Collections.singletonList(
                FamilyMember.builder().id("row-1").familyId(stateFamilyId).userUuid(userUuid1).isVerified(true).active(true).build()
        );
        Family existingFamily = Family.builder()
                .familyId(stateFamilyId)
                .tenantId(tenantId)
                .members(existingMembers)
                .build();

        when(familyRepository.search(any(FamilySearchCriteria.class))).thenReturn(Collections.singletonList(existingFamily));

        StateFamilyJitProvisionRequest request = StateFamilyJitProvisionRequest.builder()
                .stateFamilyId(stateFamilyId)
                .tenantId(tenantId)
                .userUuid(userUuid1)
                .build();

        Family result = service.jitProvisionFamilyAndMember(request);

        assertSame(existingFamily, result);
        verify(familyRepository, never()).update(any());
        verify(familyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exclusivity check: Rejects JIT provision if user already belongs to another active family")
    void testJitProvision_UserInAnotherFamily_ThrowsCustomException() {
        String stateFamilyId = "HR-2020-55555555";
        String otherFamilyId = "HR-2020-11111111";
        String userUuid = "user-uuid-already-in-fam-1";

        when(familyRepository.findActiveFamilyIdsByMemberUuid(userUuid))
                .thenReturn(Collections.singletonList(otherFamilyId));

        StateFamilyJitProvisionRequest request = StateFamilyJitProvisionRequest.builder()
                .stateFamilyId(stateFamilyId)
                .tenantId("pg.citya")
                .userUuid(userUuid)
                .build();

        org.egov.tracer.model.CustomException ex = assertThrows(
                org.egov.tracer.model.CustomException.class,
                () -> service.jitProvisionFamilyAndMember(request)
        );

        assertTrue(ex.getMessage().contains("already an active member of family"));
        verify(familyRepository, never()).save(any());
        verify(familyRepository, never()).update(any());
    }
}
