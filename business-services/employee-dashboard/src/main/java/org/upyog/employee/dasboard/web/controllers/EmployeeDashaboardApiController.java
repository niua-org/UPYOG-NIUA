package org.upyog.employee.dasboard.web.controllers;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.upyog.employee.dasboard.service.EmployeeDashboardService;
import org.upyog.employee.dasboard.web.models.EmployeeDashboardRequest;
import org.upyog.employee.dasboard.web.models.EmployeeDashboardResponse;
import org.upyog.employee.dasboard.web.models.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;


@RestController
@Api(value = "Employee Dashboard Controller", description = "Operations related to Employee Dashboard")
@Slf4j
public class EmployeeDashaboardApiController {

	@Autowired
	private EmployeeDashboardService dashboardService;
	

	@PostMapping("/_search")
	public ResponseEntity<EmployeeDashboardResponse> getDashboardData(
	@Parameter(description = "Details of the Employee Dasboard for All the modules", required = true)
	@Valid @RequestBody EmployeeDashboardRequest employeeDashboardRequest
	) {

		EmployeeDashboardResponse response = dashboardService.getEmployeeDashboardData(employeeDashboardRequest);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/// This new endpoint will get the data as per roles
	@PostMapping("/v2/_search")
	public ResponseEntity<RoleBasedDashboardResponse> getRoleBasedDashboardData(
			@ApiParam(value = "Details of the Employee Dasboard for All the modules", required = true)
			@Valid @RequestBody RoleBasedDashboardRequest request) {

		log.info("Received role-based dashboard request for user: {}",
				request.getRequestInfo().getUserInfo().getUserName());
		RoleBasedDashboardResponse response = dashboardService.getRoleBasedDashboardData(request);
		log.info("Role-based dashboard response generated successfully");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
