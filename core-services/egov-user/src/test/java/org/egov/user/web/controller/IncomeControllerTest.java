package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.service.asset.IncomeService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.asset.IncomeRequest;
import org.egov.user.web.contract.asset.IncomeResponse;
import org.egov.user.web.contract.asset.IncomeSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying HTTP request handling, response serialization, and service delegation in {@link IncomeController}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class IncomeControllerTest {

    private IncomeService incomeService;
    private IncomeController controller;

    private static final String TEST_INCOME_ID = "inc-001";
    private static final String TEST_USER_UUID = "citizen-uuid-001";

    @BeforeEach
    public void setUp() {
        incomeService = mock(IncomeService.class);
        controller = new IncomeController(incomeService);
    }

    @Test
    @DisplayName("Should return HTTP 201 CREATED when income declaration is created successfully")
    public void testCreateIncome_ReturnsCreated() {
        UserIncome createdIncome = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId("pg.citya")
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(incomeService.createIncome(any(IncomeRequest.class))).thenReturn(createdIncome);

        IncomeRequest request = IncomeRequest.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId("pg.citya")
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .build();

        ResponseEntity<IncomeResponse> response = controller.createIncome(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getIncomes().size());
        assertEquals(TEST_INCOME_ID, response.getBody().getIncomes().get(0).getId());
        assertEquals(UserConstants.STATUS_SUCCESS, response.getBody().getResponseInfo().getStatus());
        verify(incomeService, times(1)).createIncome(any(IncomeRequest.class));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when income declaration is updated successfully")
    public void testUpdateIncome_ReturnsOk() {
        UserIncome updatedIncome = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .annualIncome(new BigDecimal("600000.00"))
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(incomeService.updateIncome(any(IncomeRequest.class))).thenReturn(updatedIncome);

        IncomeRequest request = IncomeRequest.builder()
                .incomeId(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .annualIncome(new BigDecimal("600000.00"))
                .build();

        ResponseEntity<IncomeResponse> response = controller.updateIncome(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getIncomes().size());
        assertEquals(new BigDecimal("600000.00"), response.getBody().getIncomes().get(0).getAnnualIncome());
        verify(incomeService, times(1)).updateIncome(any(IncomeRequest.class));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when searching income declarations")
    public void testSearchIncome_ReturnsOk() {
        UserIncome foundIncome = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        when(incomeService.searchIncome(any(IncomeSearchCriteria.class), any())).thenReturn(Collections.singletonList(foundIncome));

        IncomeSearchCriteria criteria = IncomeSearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId("pg.citya")
                .build();

        ResponseEntity<IncomeResponse> response = controller.searchIncome(criteria);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getIncomes().size());
        assertEquals(1, response.getBody().getTotalCount());
        verify(incomeService, times(1)).searchIncome(any(IncomeSearchCriteria.class), any());
    }

    @Test
    @DisplayName("Should unwrap nested criteria and return HTTP 200 OK")
    public void testSearchIncome_UnwrapsNestedCriteria() {
        UserIncome foundIncome = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        when(incomeService.searchIncome(any(IncomeSearchCriteria.class), any())).thenReturn(Collections.singletonList(foundIncome));

        IncomeSearchCriteria inner = IncomeSearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId("pg.citya")
                .build();
        IncomeSearchCriteria envelope = IncomeSearchCriteria.builder()
                .requestInfo(RequestInfo.builder().build())
                .criteria(inner)
                .build();

        ResponseEntity<IncomeResponse> response = controller.searchIncome(envelope);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getIncomes().size());
        verify(incomeService, times(1)).searchIncome(any(IncomeSearchCriteria.class), any());
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when income declaration is soft-deleted")
    public void testDeleteIncome_ReturnsOk() {
        UserIncome deactivatedIncome = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .status(UserConstants.STATUS_INACTIVE_UPPERCASE)
                .build();

        when(incomeService.deleteIncome(any(IncomeRequest.class))).thenReturn(deactivatedIncome);

        IncomeRequest request = IncomeRequest.builder()
                .incomeId(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        ResponseEntity<IncomeResponse> response = controller.deleteIncome(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(UserConstants.STATUS_INACTIVE_UPPERCASE, response.getBody().getIncomes().get(0).getStatus());
        verify(incomeService, times(1)).deleteIncome(any(IncomeRequest.class));
    }
}
