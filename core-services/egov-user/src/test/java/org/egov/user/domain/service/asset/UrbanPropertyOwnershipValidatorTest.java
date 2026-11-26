package org.egov.user.domain.service.asset;

import org.egov.tracer.model.CustomException;
import org.egov.user.web.contract.property.UrbanPropertyInfo;
import org.egov.user.web.contract.property.UrbanPropertyOwnerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link UrbanPropertyOwnershipValidator} validating linking eligibility
 * and ownership capacity rules.
 */
class UrbanPropertyOwnershipValidatorTest {

    private UrbanPropertyOwnershipValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UrbanPropertyOwnershipValidator();
    }

    /**
     * Verifies that linking eligibility correctly handles single-owner and multiple-owner capacities.
     */
    @Test
    @DisplayName("isEligibleForLinking should evaluate correctly for individual and multi-owner properties")
    void testIsEligibleForLinking() {
        assertTrue(validator.isEligibleForLinking("INDIVIDUAL.SINGLEOWNER", 1, 0));
        assertFalse(validator.isEligibleForLinking("INDIVIDUAL.SINGLEOWNER", 1, 1));

        assertTrue(validator.isEligibleForLinking("INDIVIDUAL.MULTIPLEOWNERS", 2, 0));
        assertTrue(validator.isEligibleForLinking("INDIVIDUAL.MULTIPLEOWNERS", 2, 1));
        assertFalse(validator.isEligibleForLinking("INDIVIDUAL.MULTIPLEOWNERS", 2, 2));

        assertTrue(validator.isEligibleForLinking("JOINT", 3, 2));
        assertFalse(validator.isEligibleForLinking("JOINT", 3, 3));
    }

    /**
     * Verifies that single-owner unlinked properties resolve the sole owner's registered mobile number.
     */
    @Test
    @DisplayName("Individual property unlinked should resolve primary owner mobile")
    void testValidateIndividual_Success() {
        List<UrbanPropertyOwnerInfo> owners = new ArrayList<>();
        owners.add(UrbanPropertyOwnerInfo.builder().name("John Doe").rawMobileNumber("9876543210").build());

        UrbanPropertyInfo property = UrbanPropertyInfo.builder()
                .propertyId("PT-101")
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(owners)
                .build();

        String mobile = validator.validateAndResolveTargetOwnerMobile(property, Collections.emptyList(), "caller-uuid", null);
        assertEquals("9876543210", mobile);
    }

    /**
     * Verifies that an individual property already linked to any profile throws PROPERTY_ALREADY_LINKED.
     */
    @Test
    @DisplayName("Individual property already linked should throw PROPERTY_ALREADY_LINKED")
    void testValidateIndividual_AlreadyLinked_ThrowsError() {
        List<UrbanPropertyOwnerInfo> owners = new ArrayList<>();
        owners.add(UrbanPropertyOwnerInfo.builder().name("John Doe").rawMobileNumber("9876543210").build());

        UrbanPropertyInfo property = UrbanPropertyInfo.builder()
                .propertyId("PT-101")
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(owners)
                .build();

        Map<String, Object> linked = new HashMap<>();
        linked.put("userUuid", "other-user-uuid");
        linked.put("mobileNumber", "9876543210");

        CustomException ex = assertThrows(CustomException.class, () ->
                validator.validateAndResolveTargetOwnerMobile(property, List.of(linked), "caller-uuid", null));
        assertEquals("PROPERTY_ALREADY_LINKED", ex.getCode());
    }

    /**
     * Verifies that if the caller has already linked their profile to this property, PROPERTY_ALREADY_LINKED_TO_CALLER is thrown.
     */
    @Test
    @DisplayName("Caller already linked should throw PROPERTY_ALREADY_LINKED_TO_CALLER")
    void testCallerAlreadyLinked_ThrowsError() {
        UrbanPropertyInfo property = UrbanPropertyInfo.builder()
                .propertyId("PT-101")
                .ownershipCategory("INDIVIDUAL.MULTIPLEOWNERS")
                .owners(List.of(
                        UrbanPropertyOwnerInfo.builder().rawMobileNumber("9876543210").build(),
                        UrbanPropertyOwnerInfo.builder().rawMobileNumber("9123456780").build()
                ))
                .build();

        Map<String, Object> linked = new HashMap<>();
        linked.put("userUuid", "caller-uuid");
        linked.put("mobileNumber", "9876543210");

        CustomException ex = assertThrows(CustomException.class, () ->
                validator.validateAndResolveTargetOwnerMobile(property, List.of(linked), "caller-uuid", null));
        assertEquals("PROPERTY_ALREADY_LINKED_TO_CALLER", ex.getCode());
    }

    @Test
    @DisplayName("Individual property with masked mobile number should throw OWNER_MOBILE_NOT_FOUND")
    void testValidateIndividual_MaskedMobile_ThrowsError() {
        UrbanPropertyInfo property = UrbanPropertyInfo.builder()
                .propertyId("PT-101")
                .ownershipCategory("INDIVIDUAL.SINGLEOWNER")
                .owners(List.of(
                        UrbanPropertyOwnerInfo.builder().rawMobileNumber("98*****210").build()
                ))
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                validator.validateAndResolveTargetOwnerMobile(property, Collections.emptyList(), "caller-uuid", null));
        assertEquals("OWNER_MOBILE_NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("Multiple-owner property with masked mobile number should throw OWNER_MOBILE_NOT_FOUND")
    void testValidateMultipleOwners_MaskedMobile_ThrowsError() {
        UrbanPropertyInfo property = UrbanPropertyInfo.builder()
                .propertyId("PT-102")
                .ownershipCategory("INDIVIDUAL.MULTIPLEOWNERS")
                .owners(List.of(
                        UrbanPropertyOwnerInfo.builder().rawMobileNumber("98*****210").build(),
                        UrbanPropertyOwnerInfo.builder().rawMobileNumber("9123456780").build()
                ))
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                validator.validateAndResolveTargetOwnerMobile(property, Collections.emptyList(), "caller-uuid", "9123456780"));
        assertEquals("OWNER_MOBILE_NOT_FOUND", ex.getCode());
    }

    @Test
    @DisplayName("Multiple-owner property with requested masked mobile should throw INVALID_OWNER_MOBILE")
    void testValidateMultipleOwners_RequestedMobile_Masked_ThrowsError() {
        UrbanPropertyInfo property = UrbanPropertyInfo.builder()
                .propertyId("PT-102")
                .ownershipCategory("INDIVIDUAL.MULTIPLEOWNERS")
                .owners(List.of(
                        UrbanPropertyOwnerInfo.builder().rawMobileNumber("9876543210").build(),
                        UrbanPropertyOwnerInfo.builder().rawMobileNumber("9123456780").build()
                ))
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                validator.validateAndResolveTargetOwnerMobile(property, Collections.emptyList(), "caller-uuid", "98*****210"));
        assertEquals("INVALID_OWNER_MOBILE", ex.getCode());
    }
}
