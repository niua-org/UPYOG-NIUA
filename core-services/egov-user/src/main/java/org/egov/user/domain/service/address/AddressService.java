package org.egov.user.domain.service.address;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.web.contract.AddressRequest;

import java.util.List;

/**
 * Service contract for address management supporting creation, soft-versioned updates,
 * criteria-based search, and soft deletion.
 *
 * Adheres to Dependency Inversion Principle (DIP).
 */
public interface AddressService {

    /**
     * Creates a new address record with door no, street, locality, landmark, and pincode.
     * Enforces single permanent address constraint and validates mandatory fields.
     *
     * @param request the address creation request envelope
     * @return created address entity with generated ID and audit timestamps
     */
    Address createAddress(AddressRequest request);

    /**
     * Updates an address using SCD Type 2 soft-versioning (marking the previous version inactive and creating an active replacement).
     * Validates immutability constraints on non-editable fields (City, Locality, House Number, Pin Code).
     *
     * @param request the address update request containing target ID and updated fields
     * @return newly inserted active address record representing the latest version
     */
    Address updateAddress(AddressRequest request);

    /**
     * Searches address records based on filter criteria such as userUuid, tenantId, type, and status.
     *
     * @param criteria    the filter criteria for address search
     * @param requestInfo standard UPYOG request metadata
     * @return list of matching decrypted address records
     */
    List<Address> searchAddress(AddressSearchCriteria criteria, RequestInfo requestInfo);

    /**
     * Soft-deletes / deactivates an address by updating its status to inactive.
     *
     * @param request the deletion request containing the address ID to deactivate
     * @return deactivated address record with status updated
     */
    Address deleteAddress(AddressRequest request);
}
