package org.upyog.family.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.upyog.family.domain.service.FamilyDashboardService;
import org.upyog.family.domain.service.FamilyHeadService;
import org.upyog.family.domain.service.FamilyMemberService;
import org.upyog.family.domain.service.FamilyService;
import org.upyog.family.domain.service.StateFamilySyncService;
import org.upyog.family.util.ResponseInfoFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test verifying that all Family controllers register with Spring MVC without
 * AmbiguousMappingException or overlapping route conflicts.
 */
@WebMvcTest(controllers = {
        FamilyController.class,
        FamilyMemberController.class,
        FamilyHeadController.class,
        FamilyDashboardController.class,
        StateFamilySyncController.class
})
@ContextConfiguration(classes = FamilyControllersMappingSmokeTest.TestConfig.class)
class FamilyControllersMappingSmokeTest {

    @Configuration
    @Import({
            FamilyController.class,
            FamilyMemberController.class,
            FamilyHeadController.class,
            FamilyDashboardController.class,
            StateFamilySyncController.class
    })
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FamilyService familyService;

    @MockBean
    private FamilyMemberService memberService;

    @MockBean
    private FamilyHeadService headService;

    @MockBean
    private FamilyDashboardService dashboardService;

    @MockBean
    private StateFamilySyncService stateFamilySyncService;

    @MockBean
    private ResponseInfoFactory responseInfoFactory;

    @Test
    @DisplayName("Verify Spring MVC context loads with all domain-scoped controller routes without ambiguous mapping")
    void contextLoads_AndControllersRegistered() {
        assertNotNull(mockMvc);
    }
}

