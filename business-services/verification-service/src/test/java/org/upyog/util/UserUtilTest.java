package org.upyog.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import digit.models.coremodels.UserDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.upyog.repository.ServiceRequestRepository;
import org.upyog.web.models.User;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUtilTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private UserUtil userUtil;

    @BeforeEach
    void setUp() {
        userUtil = new UserUtil(new ObjectMapper(), serviceRequestRepository);
        ReflectionTestUtils.setField(userUtil, "userCreateEndpoint", "/user/_create");
        ReflectionTestUtils.setField(userUtil, "userSearchEndpoint", "/user/_search");
        ReflectionTestUtils.setField(userUtil, "userUpdateEndpoint", "/user/_update");
    }

    @Test
    void parseResponse_convertsDateStringsToLong() {
        Map<String, Object> user = new HashMap<>();
        user.put("createdDate", "01-01-2020 00:00:00");
        user.put("dob", "1990-01-01");
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("user", List.of(user));

        userUtil.parseResponse(responseMap, "yyyy-MM-dd");

        assertThat(user.get("createdDate")).isInstanceOf(Long.class);
        assertThat(user.get("dob")).isInstanceOf(Long.class);
    }

    @Test
    void addUserDefaultFields_setsCitizenRole() {
        User user = new User();
        userUtil.addUserDefaultFields("9999999999", "pb.amritsar", user);

        assertThat(user.getType()).isEqualTo("CITIZEN");
        assertThat(user.getUserName()).isEqualTo("9999999999");
        assertThat(user.getTenantId()).isEqualTo("pb");
        assertThat(user.getRoles()).hasSize(1);
    }

    @Test
    void getStateLevelTenant_returnsStateCode() {
        assertThat(userUtil.getStateLevelTenant("pb.amritsar")).isEqualTo("pb");
    }
}
