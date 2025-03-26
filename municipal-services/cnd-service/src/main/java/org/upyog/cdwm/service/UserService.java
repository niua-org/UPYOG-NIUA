package org.upyog.cdwm.service;

import java.util.*;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.upyog.cdwm.config.CNDConfiguration;
import org.upyog.cdwm.constants.CNDConstants;
import org.upyog.cdwm.repository.ServiceRequestRepository;
import org.upyog.cdwm.util.UserUtil;
import org.upyog.cdwm.web.models.CNDAddressDetail;
import org.upyog.cdwm.web.models.CNDApplicantDetail;
import org.upyog.cdwm.web.models.CNDApplicationDetail;
import org.upyog.cdwm.web.models.CNDApplicationRequest;
import org.upyog.cdwm.web.models.user.*;
import org.upyog.cdwm.web.models.user.enums.AddressType;
import org.upyog.cdwm.web.models.user.enums.UserType;

/**
 * Service class for managing user-related operations such as creating,
 * searching, and retrieving user details.
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private CNDConfiguration config;

    /**
     * Retrieves an existing user or creates a new user if not found.
     *
     * @param bookingRequest The application request containing user details.
     * @return The existing or newly created user.
     */
    public User getExistingOrNewUser(CNDApplicationRequest bookingRequest) {

        CNDApplicationDetail applicationDetail = bookingRequest.getCndApplication();
        RequestInfo requestInfo = bookingRequest.getRequestInfo();
        CNDApplicantDetail applicantDetail = applicationDetail.getApplicantDetail();
        String tenantId = applicationDetail.getTenantId();

        // Fetch existing user details
        UserDetailResponseV2 userDetailResponse = fetchUser(applicantDetail, requestInfo, tenantId);
        List<User> existingUsers = userDetailResponse.getUser();

        // Create a new user if no existing user found
        if (CollectionUtils.isEmpty(existingUsers)) {
            return createUserHandler(requestInfo, applicantDetail, applicationDetail.getAddressDetail(), tenantId);
        }

        return existingUsers.get(0);
    }

    /**
     * Creates a new user and returns the generated user details.
     *
     * @param requestInfo     The request information.
     * @param applicantDetail The applicant details.
     * @param tenantId        The tenant ID.
     * @return The created user.
     */
    private User createUserHandler(RequestInfo requestInfo, CNDApplicantDetail applicantDetail, CNDAddressDetail cndAddressDetail, String tenantId) {
        Role role = getCitizenRole();
        User user = convertApplicantToUserRequest(applicantDetail, role, tenantId);
        Address address = convertApplicantAddressToUserAddress(cndAddressDetail, tenantId);
        user.addAddressItem(address);
        UserDetailResponseV2 userDetailResponse = createUser(requestInfo, user, tenantId);
        String newUuid = userDetailResponse.getUser().get(0).getUuid();
        log.info("New user uuid returned from user service: {}", newUuid);
        return userDetailResponse.getUser().get(0);
    }


    /**
     * Creates a user in the system.
     *
     * @param requestInfo The request information.
     * @param user        The user to be created.
     * @param tenantId    The tenant ID.
     * @return The response containing the created user.
     */
    private UserDetailResponseV2 createUser(RequestInfo requestInfo, User user, String tenantId) {

        StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserCreateEndpoint());
        CreateUserRequestV2 userRequest = CreateUserRequestV2.builder().requestInfo(requestInfo).user(user).build();
        UserDetailResponseV2 userDetailResponse = userServiceCall(userRequest, uri);

        if (ObjectUtils.isEmpty(userDetailResponse)) {
            throw new CustomException("INVALID USER RESPONSE",
                    "The user create has failed for the mobileNumber : " + user.getMobileNumber());
        }
        return userDetailResponse;
    }

    /**
     * Converts an applicant detail to a User object.
     *
     * @param applicant The applicant details.
     * @param role      The user role.
     * @param tenantId  The tenant ID.
     * @return The converted User object.
     */
    private User convertApplicantToUserRequest(CNDApplicantDetail applicant, Role role, String tenantId) {
        if (applicant == null) {
            throw new CustomException("INVALID APPLICANT", "The applicant details are empty or null");
        }

        User userRequest = new User();
        userRequest.setName(applicant.getNameOfApplicant());
        userRequest.setUserName(applicant.getMobileNumber());
        userRequest.setMobileNumber(applicant.getMobileNumber());
        userRequest.setAlternatemobilenumber(applicant.getAlternateMobileNumber());
        userRequest.setEmailId(applicant.getEmailId());
        userRequest.setActive(true);
        userRequest.setTenantId(tenantId);
        userRequest.setRoles(Collections.singletonList(role));
        userRequest.setType(UserType.CITIZEN);
        return userRequest;
    }

    /**
     * Converts an applicant address to a User address object to send in user create call with address object.
     *
     * @param cndAddressDetail The address details.
     * @param tenantId         The tenant ID.
     * @return The converted User address object.
     */
    private Address convertApplicantAddressToUserAddress(CNDAddressDetail cndAddressDetail, String tenantId) {
        if (cndAddressDetail == null) {
            log.info("The address details are empty or null");
        }
        Address address = Address.builder().
                address(cndAddressDetail.getAddressLine1()).
                address2(cndAddressDetail.getAddressLine2()).
                city(cndAddressDetail.getCity()).
                landmark(cndAddressDetail.getLandmark()).
                locality(cndAddressDetail.getLocality()).
                pinCode(cndAddressDetail.getPinCode()).
                houseNumber(cndAddressDetail.getHouseNumber()).
                tenantId(tenantId).
                type(AddressType.PERMANENT).
                build();


        return address;
    }


    /**
     * Retrieves the Citizen role.
     *
     * @return The citizen role.
     */
    private Role getCitizenRole() {

        return Role.builder().code(CNDConstants.CITIZEN).name(CNDConstants.CITIZEN_NAME).build();
    }

    /**
     * Searches if the applicant is already created in user registry with the mobile
     * number entered. Search is based on name of owner, uuid and mobileNumber
     *
     * @param applicant   Owner which is to be searched
     * @param requestInfo RequestInfo from the propertyRequest
     * @return UserDetailResponseV2 containing the user if present and the
     * responseInfo
     */
    private UserDetailResponseV2 fetchUser(CNDApplicantDetail applicant, RequestInfo requestInfo, String tenantId) {

        UserSearchRequestV2 userSearchRequest = getBaseUserSearchRequest(tenantId, requestInfo);
        userSearchRequest.setMobileNumber(applicant.getMobileNumber());
        userSearchRequest.setUserType(CNDConstants.CITIZEN);
        userSearchRequest.setUserName(applicant.getMobileNumber());

        StringBuilder uri = new StringBuilder(config.getUserHost())
                .append(config.getUserSearchEndpoint());
        return userServiceCall(userSearchRequest, uri);
    }

    /**
     * Returns user using user search based on propertyCriteria(owner
     * name,mobileNumber,userName)
     *
     * @param userSearchRequest Request object for user service
     * @return serDetailResponse containing the user if present and the responseInfo
     */
    public UserDetailResponseV2 getUser(UserSearchRequestV2 userSearchRequest) {

        StringBuilder uri = new StringBuilder(config.getUserHost())
                .append(config.getUserSearchEndpoint());
        UserDetailResponseV2 userDetailResponse = userServiceCall(userSearchRequest, uri);
        return userDetailResponse;
    }

    /**
     * Returns UserDetailResponseV2 by calling user service with given uri and object
     *
     * @param userRequest Request object for user service
     * @param url         The address of the endpoint
     * @return Response from user service as parsed as userDetailResponse
     */
    @SuppressWarnings("unchecked")
    private UserDetailResponseV2 userServiceCall(Object userRequest, StringBuilder url) {

        String dobFormat = null;
        if (url.indexOf(config.getUserSearchEndpoint()) != -1
                || url.indexOf(config.getUserUpdateEndpoint()) != -1)
            dobFormat = "yyyy-MM-dd";
        else if (url.indexOf(config.getUserCreateEndpoint()) != -1)
            dobFormat = "dd/MM/yyyy";
        try {
            Object response = serviceRequestRepository.fetchResult(url, userRequest);

            if (response != null) {
                LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) response;
                log.info("Response from user service: {}", responseMap);
                parseResponse(responseMap, dobFormat);
                UserDetailResponseV2 userDetailResponse = mapper.convertValue(responseMap, UserDetailResponseV2.class);
                return userDetailResponse;
            } else {
                return new UserDetailResponseV2();
            }
        } catch (IllegalArgumentException e) {
            throw new CustomException("IllegalArgumentException", "ObjectMapper not able to convertValue in userCall");
        }
    }

    /**
     * Parses date formats to long for all users in responseMap
     *
     * @param responeMap LinkedHashMap got from user api response
     * @param dobFormat  dob format (required because dob is returned in different
     *                   format's in search and create response in user service)
     */
    @SuppressWarnings("unchecked")
    private void parseResponse(LinkedHashMap<String, Object> responeMap, String dobFormat) {

        List<LinkedHashMap<String, Object>> users = (List<LinkedHashMap<String, Object>>) responeMap.get("user");
        String format1 = "dd-MM-yyyy HH:mm:ss";

        if (null != users) {

            users.forEach(map -> {

                map.put("createdDate", UserUtil.dateTolong((String) map.get("createdDate"), format1));
                if ((String) map.get("lastModifiedDate") != null)
                    map.put("lastModifiedDate",
                            UserUtil.dateTolong((String) map.get("lastModifiedDate"), format1));
                if ((String) map.get("dob") != null)
                    map.put("dob", UserUtil.dateTolong((String) map.get("dob"), dobFormat));
                if ((String) map.get("pwdExpiryDate") != null)
                    map.put("pwdExpiryDate", UserUtil.dateTolong((String) map.get("pwdExpiryDate"), format1));
            });
        }
    }

    /**
     * provides a user search request with basic mandatory parameters
     *
     * @param tenantId
     * @param requestInfo
     * @return
     */
    public UserSearchRequestV2 getBaseUserSearchRequest(String tenantId, RequestInfo requestInfo) {

        return UserSearchRequestV2.builder().requestInfo(requestInfo).userType(CNDConstants.CITIZEN)
                .tenantId(tenantId).active(true).build();
    }

    // get User by user uuid
    public User getUser(String uuid, String tenantId, RequestInfo requestInfo) {
        UserSearchRequestV2 userSearchRequest = getBaseUserSearchRequest(tenantId, requestInfo);
        userSearchRequest.setUuid(Collections.singletonList(uuid));
        UserDetailResponseV2 userDetailResponse = getUser(userSearchRequest);
        List<User> users = userDetailResponse.getUser();
        return CollectionUtils.isEmpty(users) ? null : users.get(0);
    }

    /**
     * Converts a user object to an applicant detail object.
     *
     * @param user The user object.
     * @return The converted applicant detail.
     */
    public CNDApplicantDetail convertUserToApplicantDetail(User user) {
        if (user == null) {
            return null;
        }
        // Convert User to CNDApplicantDetail
        return CNDApplicantDetail.builder()
                .nameOfApplicant(user.getName())
                .mobileNumber(user.getMobileNumber())
                .emailId(user.getEmailId())
                .alternateMobileNumber(user.getAlternatemobilenumber())
                .build();
    }

    /**
     * Converts a user address to an address detail object.
     *
     * @param addresses The set of addresses.
     * @return The converted address detail.
     */
    public CNDAddressDetail convertUserAddressToAddressDetail(Set<Address> addresses) {
        if (CollectionUtils.isEmpty(addresses)) {
            return null;
        }
        //Below line will stream addresses to find address which has address type as Permanent
        Address address = addresses.stream().filter(addr -> addr.getType().equals(AddressType.PERMANENT)).findFirst()
                .orElse(null);
        return CNDAddressDetail.builder()
                .addressLine1(address.getAddress())
                .addressLine2(address.getAddress2())
                .city(address.getCity())
                .landmark(address.getLandmark())
                .locality(address.getLocality())
                .pinCode(address.getPinCode())
                .houseNumber(address.getHouseNumber())
                .build();

    }

}
