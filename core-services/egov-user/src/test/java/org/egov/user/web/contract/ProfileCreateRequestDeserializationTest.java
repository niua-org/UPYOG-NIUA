package org.egov.user.web.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProfileCreateRequestDeserializationTest {

    @Test
    void testDeserializationWithNestedUser() throws Exception {
        String json = "{\n" +
                "    \"RequestInfo\": {\n" +
                "        \"apiId\": \"org.egov.user\",\n" +
                "        \"ver\": \"1.0\",\n" +
                "        \"ts\": 1728470440000,\n" +
                "        \"authToken\": \"e158f381-81d3-463e-908d-937d1d234567\"\n" +
                "    },\n" +
                "    \"user\": {\n" +
                "        \"name\": \"Aarav Sharma\",\n" +
                "        \"fatherName\": \"Rajesh Sharma\",\n" +
                "        \"motherName\": \"Sunita Sharma\",\n" +
                "        \"spouseName\": \"Pooja Sharma\",\n" +
                "        \"mobileNumber\": \"8588070881\",\n" +
                "        \"alternateMobileNumber\": \"9876543219\",\n" +
                "        \"dob\": \"1992-07-14\",\n" +
                "        \"emailId\": \"aarav.sharma@example.com\",\n" +
                "        \"gender\": \"MALE\",\n" +
                "        \"tenantId\": \"pg.citya\",\n" +
                "        \"type\": \"CITIZEN\",\n" +
                "        \"aadhaarNumber\": \"XXXXXXXX1458\",\n" +
                "        \"addresses\": [\n" +
                "            {\n" +
                "                \"type\": \"PERMANENT\",\n" +
                "                \"city\": \"Amritsar\",\n" +
                "                \"locality\": \"Sector 14\",\n" +
                "                \"streetName\": \"MG Road\",\n" +
                "                \"houseNumber\": \"Flat 402\",\n" +
                "                \"houseName\": \"Royal Palms\",\n" +
                "                \"landmark\": \"Near Civil Hospital\",\n" +
                "                \"pinCode\": \"143001\",\n" +
                "                \"tenantId\": \"pg.citya\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"documents\": [\n" +
                "            {\n" +
                "                \"documentType\": \"IDENTITY_PROOF\",\n" +
                "                \"filestoreId\": \"f784d1e2-5678-4321-9876-0123456789ab\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        ProfileCreateRequest request = mapper.readValue(json, ProfileCreateRequest.class);

        System.out.println("DEBUG: tenantId=" + request.getTenantId());
        System.out.println("DEBUG: name=" + request.getName());
        System.out.println("DEBUG: mobileNumber=" + request.getMobileNumber());
        System.out.println("DEBUG: address=" + request.getAddress());

        assertNotNull(request.getTenantId());
        assertEquals("pg.citya", request.getTenantId());
        assertEquals("Aarav Sharma", request.getName());
        assertEquals("8588070881", request.getMobileNumber());
        assertNotNull(request.getAddress());
        assertEquals("Amritsar", request.getAddress().getCity());
    }

    @Test
    void testAddressProofInsideAddressObject() throws Exception {
        String json = "{\n" +
                "    \"RequestInfo\": {\"apiId\": \"org.egov.user\"},\n" +
                "    \"user\": {\n" +
                "        \"name\": \"Aarav Sharma\",\n" +
                "        \"fatherName\": \"Rajesh Sharma\",\n" +
                "        \"motherName\": \"Sunita Sharma\",\n" +
                "        \"mobileNumber\": \"8588070881\",\n" +
                "        \"dob\": \"1992-07-14\",\n" +
                "        \"emailId\": \"aarav@test.com\",\n" +
                "        \"gender\": \"MALE\",\n" +
                "        \"tenantId\": \"pg.citya\",\n" +
                "        \"addresses\": [\n" +
                "            {\n" +
                "                \"type\": \"PERMANENT\",\n" +
                "                \"city\": \"Amritsar\",\n" +
                "                \"locality\": \"Sector 14\",\n" +
                "                \"streetName\": \"MG Road\",\n" +
                "                \"houseNumber\": \"Flat 402\",\n" +
                "                \"landmark\": \"Near Hospital\",\n" +
                "                \"pinCode\": \"143001\",\n" +
                "                \"filestoreId\": \"addr-proof-filestore-001\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"type\": \"CORRESPONDENCE\",\n" +
                "                \"city\": \"Jalandhar\",\n" +
                "                \"locality\": \"Model Town\",\n" +
                "                \"streetName\": \"GT Road\",\n" +
                "                \"houseNumber\": \"House 12\",\n" +
                "                \"landmark\": \"Clock Tower\",\n" +
                "                \"pinCode\": \"144001\",\n" +
                "                \"addressProofFileStoreId\": \"addr-proof-filestore-002\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"documents\": [\n" +
                "            {\"documentType\": \"IDENTITY_PROOF\", \"filestoreId\": \"ident-001\"},\n" +
                "            {\"documentType\": \"DOB_PROOF\", \"filestoreId\": \"dob-001\"}\n" +
                "        ]\n" +
                "    }\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        ProfileCreateRequest request = mapper.readValue(json, ProfileCreateRequest.class);

        assertNotNull(request.getEffectiveAddresses());
        assertEquals(2, request.getEffectiveAddresses().size());
        assertEquals("addr-proof-filestore-001", request.getEffectiveAddresses().get(0).getFilestoreId());
        assertEquals("addr-proof-filestore-002", request.getEffectiveAddresses().get(1).getFilestoreId());
    }

    @Test
    void testProfileResyncRequestAliases() throws Exception {
        String json = "{\n" +
                "  \"RequestInfo\": {\n" +
                "    \"apiId\": \"org.egov.user\",\n" +
                "    \"userInfo\": {\n" +
                "      \"uuid\": \"de1dd139-2603-4392-b7fa-73b4133a4e3b\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"userUuid\": \"de1dd139-2603-4392-b7fa-73b4133a4e3b\",\n" +
                "  \"tenantId\": \"pg.citya\",\n" +
                "  \"aadhaarNumber\": \"548796521458\",\n" +
                "  \"referenceId\": \"784512\",\n" +
                "  \"otp\": \"123456\"\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        org.egov.user.web.contract.profile.ProfileResyncRequest request =
                mapper.readValue(json, org.egov.user.web.contract.profile.ProfileResyncRequest.class);

        assertEquals("de1dd139-2603-4392-b7fa-73b4133a4e3b", request.getUuid());
        assertEquals("784512", request.getTxnId());
        assertEquals("pg.citya", request.getTenantId());
        assertEquals("548796521458", request.getAadhaarNumber());
        assertEquals("123456", request.getOtp());
    }
}
