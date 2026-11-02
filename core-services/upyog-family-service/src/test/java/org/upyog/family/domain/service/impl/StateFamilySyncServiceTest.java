package org.upyog.family.domain.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.model.MemberStatus;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.StateFamilySyncRepository;
import org.upyog.family.util.ResponseInfoFactory;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.state.StateFamily;
import org.upyog.family.web.contract.state.StateFamilyJitProvisionRequest;
import org.upyog.family.web.contract.state.StateFamilyMember;
import org.upyog.family.web.contract.state.StateFamilySyncRequest;
import org.upyog.family.web.contract.state.StateFamilySyncResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StateFamilySyncServiceTest {

    @Mock
    private StateFamilySyncRepository stateFamilySyncRepository;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private ResponseInfoFactory responseInfoFactory;

    private StateFamilySyncServiceImpl stateFamilySyncService;

    @BeforeEach
    public void setUp() {
        stateFamilySyncService = new StateFamilySyncServiceImpl(
                stateFamilySyncRepository, familyRepository, responseInfoFactory
        );
    }

    private StateFamily buildStateFamily() {
        List<StateFamilyMember> members = new ArrayList<>();
        members.add(StateFamilyMember.builder()
                .memberId("MEM-1")
                .name("Arpit Rao")
                .relationshipWithHead("HEAD")
                .isHead(true)
                .mobileNumber("7906413996")
                .aadhaarNumber("548796521458")
                .gender("MALE")
                .dob("1996-06-11")
                .build());
        members.add(StateFamilyMember.builder()
                .memberId("MEM-2")
                .name("Priya Rao")
                .relationshipWithHead("SPOUSE")
                .isHead(false)
                .mobileNumber("9876543210")
                .aadhaarNumber("548796521459")
                .gender("FEMALE")
                .dob("1998-08-20")
                .build());

        return StateFamily.builder()
                .stateCode("HR")
                .stateFamilyId("HR-2020-12342356")
                .familyName("Rao Family")
                .tenantId("pg.citya")
                .category("GENERAL")
                .members(members)
                .build();
    }

    @Test
    @DisplayName("Should sync state family and return referenceId with masked identifiers")
    public void testSyncStateFamily_Success() {
        StateFamily stateFamily = buildStateFamily();
        StateFamilySyncRequest request = StateFamilySyncRequest.builder()
                .requestInfo(new RequestInfo())
                .stateFamily(stateFamily)
                .build();

        StateFamilySyncResponse response = stateFamilySyncService.syncStateFamily(request);

        assertNotNull(response);
        assertNotNull(response.getReferenceId());
        assertTrue(response.getReferenceId().startsWith("REF-"));
        assertEquals("STAGED", response.getStatus());

        // Verify masking
        assertNotNull(response.getStateFamily());
        assertEquals("79******96", response.getStateFamily().getMembers().get(0).getMaskedMobile());
        assertEquals("XXXXXXXX1458", response.getStateFamily().getMembers().get(0).getMaskedAadhaar());

        verify(stateFamilySyncRepository, times(1)).save(anyString(), any(StateFamily.class));
    }

    @Test
    @DisplayName("Should retrieve staged state family for Family Found screen")
    public void testGetStateFamily_Success() {
        StateFamily stateFamily = buildStateFamily();
        when(stateFamilySyncRepository.findByReferenceId("REF-123")).thenReturn(stateFamily);

        StateFamilySyncResponse response = stateFamilySyncService.getStateFamily("REF-123", null, new RequestInfo());

        assertNotNull(response);
        assertEquals("FOUND", response.getStatus());
        assertEquals("Rao Family", response.getStateFamily().getFamilyName());
        assertEquals("79******96", response.getStateFamily().getMembers().get(0).getMaskedMobile());
    }

    @Test
    @DisplayName("JIT: Should create new Family when family does not exist in UPYOG")
    public void testJitProvision_WhenFamilyDoesNotExist_CreatesFamily() {
        StateFamily stateFamily = buildStateFamily();
        when(familyRepository.search(any())).thenReturn(Collections.emptyList());
        when(stateFamilySyncRepository.findByReferenceId("REF-123")).thenReturn(stateFamily);

        when(familyRepository.save(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StateFamilyJitProvisionRequest request = StateFamilyJitProvisionRequest.builder()
                .referenceId("REF-123")
                .stateFamilyId("HR-2020-12342356")
                .tenantId("pg.citya")
                .userUuid("uuid-arpit-101")
                .selectedMemberId("MEM-1")
                .build();

        Family provisioned = stateFamilySyncService.jitProvisionFamilyAndMember(request);

        assertNotNull(provisioned);
        assertEquals("HR-2020-12342356", provisioned.getFamilyId());
        assertEquals("uuid-arpit-101", provisioned.getHeadUserUuid());
        assertEquals(FamilyStatus.ACTIVE, provisioned.getStatus());
        assertEquals(2, provisioned.getMembers().size());

        // Selected member is mapped to userUuid
        assertTrue(provisioned.getMembers().stream().anyMatch(m -> "uuid-arpit-101".equals(m.getUserUuid())));

        verify(familyRepository, times(1)).save(any(Family.class));
    }

    @Test
    @DisplayName("JIT: Should map member to existing Family when family already exists in UPYOG")
    public void testJitProvision_WhenFamilyExists_MapsMember() {
        Family existingFamily = Family.builder()
                .id("fam-uuid-1")
                .familyId("HR-2020-12342356")
                .tenantId("pg.citya")
                .headUserUuid("uuid-hof")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Collections.singletonList(
                        FamilyMember.builder()
                                .id("mem-1")
                                .familyId("HR-2020-12342356")
                                .userUuid("uuid-hof")
                                .isHead(true)
                                .isVerified(true)
                                .active(true)
                                .build()
                )))
                .build();

        when(familyRepository.search(any())).thenReturn(Collections.singletonList(existingFamily));
        when(familyRepository.update(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StateFamilyJitProvisionRequest request = StateFamilyJitProvisionRequest.builder()
                .stateFamilyId("HR-2020-12342356")
                .tenantId("pg.citya")
                .userUuid("uuid-member-202")
                .build();

        Family updated = stateFamilySyncService.jitProvisionFamilyAndMember(request);

        assertNotNull(updated);
        assertEquals(2, updated.getMembers().size());
        assertTrue(updated.getMembers().stream().anyMatch(m -> "uuid-member-202".equals(m.getUserUuid())));
        verify(familyRepository, times(1)).update(any(Family.class));
    }
}
