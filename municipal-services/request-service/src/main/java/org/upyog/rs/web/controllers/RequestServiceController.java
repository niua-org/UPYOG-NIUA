package org.upyog.rs.web.controllers;


import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.upyog.rs.constant.RequestServiceConstants;
import org.upyog.rs.service.MobileToiletService;
import org.upyog.rs.service.WaterTankerService;
import org.upyog.rs.util.RequestServiceUtil;
import org.upyog.rs.validator.ValidatorService;
import org.upyog.rs.web.models.ResponseInfo;
import org.upyog.rs.web.models.mobileToilet.MobileToiletBookingDetail;
import org.upyog.rs.web.models.mobileToilet.MobileToiletBookingRequest;
import org.upyog.rs.web.models.mobileToilet.MobileToiletBookingResponse;
import org.upyog.rs.web.models.mobileToilet.MobileToiletBookingSearchCriteria;
import org.upyog.rs.web.models.mobileToilet.MobileToiletBookingSearchResponse;
import org.upyog.rs.web.models.waterTanker.WaterTankerBookingDetail;
import org.upyog.rs.web.models.waterTanker.WaterTankerBookingRequest;
import org.upyog.rs.web.models.waterTanker.WaterTankerBookingResponse;
import org.upyog.rs.web.models.waterTanker.WaterTankerBookingSearchCriteria;
import org.upyog.rs.web.models.waterTanker.WaterTankerBookingSearchResponse;
import org.upyog.rs.web.models.ResponseInfo.StatusEnum;

import digit.models.coremodels.RequestInfoWrapper;
import lombok.extern.slf4j.Slf4j;

@jakarta.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2025-01-16T15:46:56.897+05:30")

@Controller
@Slf4j
@Tag(name = "Request Service", description = "APIs for Request Service operations")
public class RequestServiceController {
	
	@Autowired
	private WaterTankerService waterTankerService;

	@Autowired
	private MobileToiletService mobileToiletService;

	@Autowired
	private ValidatorService validatorService;

	@PostMapping("/water-tanker/v1/_create")
	@Operation(summary = "Create application details", description = "Details for the water tanker booking time, payment and documents")
	public ResponseEntity<WaterTankerBookingResponse> createWaterTankerBooking(

			@RequestBody WaterTankerBookingRequest waterTankerbookingRequest) {
		log.info("waterTankerbookingRequest : {}" , waterTankerbookingRequest);
        validatorService.validateRequest(waterTankerbookingRequest);
		WaterTankerBookingDetail waterTankerDetail = waterTankerService.createNewWaterTankerBookingRequest(waterTankerbookingRequest);
		ResponseInfo info = RequestServiceUtil.createReponseInfo(waterTankerbookingRequest.getRequestInfo(),
				RequestServiceConstants.BOOKING_CREATED, StatusEnum.SUCCESSFUL);
		WaterTankerBookingResponse response = WaterTankerBookingResponse.builder()
				.waterTankerBookingApplication(waterTankerDetail)
				.responseInfo(info).build();
		return new ResponseEntity<WaterTankerBookingResponse>(response, HttpStatus.OK);
	}

	@PostMapping("/water-tanker/v1/_search")
	@Operation(summary = "Search application details", description = "Details for the water tanker booking time, payment and documents")
	public ResponseEntity<WaterTankerBookingSearchResponse> searchWaterTankerBookingDetails(
			 @Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@ModelAttribute WaterTankerBookingSearchCriteria waterTankerBookingSearchCriteria) {

		List<WaterTankerBookingDetail> applications = null;
		Integer count = 0;

		applications = waterTankerService.getWaterTankerBookingDetails(requestInfoWrapper.getRequestInfo(),
				waterTankerBookingSearchCriteria);

		count = waterTankerService.getApplicationsCount(waterTankerBookingSearchCriteria,
				requestInfoWrapper.getRequestInfo());

		/*
		 * Create Response Info with success status and used utilize method to generate
		 * standardized response
		 */
		ResponseInfo responseInfo = RequestServiceUtil.createReponseInfo(requestInfoWrapper.getRequestInfo(),
				RequestServiceConstants.BOOKING_DETAIL_FOUND, StatusEnum.SUCCESSFUL);
		/*
		 * Build search response using builder and retrieve booking details and response
		 * metadata
		 */
		WaterTankerBookingSearchResponse response = WaterTankerBookingSearchResponse.builder()
				.waterTankerBookingDetails(applications).responseInfo(responseInfo).count(count).build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping("/water-tanker/v1/_update")
	@Operation(summary = "Update application details", description = "Updated water tanker details and RequestInfo meta data.")
	public ResponseEntity<WaterTankerBookingResponse> waterTankerUpdate(

			@RequestBody WaterTankerBookingRequest waterTankerRequest) {
		
		WaterTankerBookingDetail waterTankerDetail = waterTankerService.updateWaterTankerBooking(waterTankerRequest, null);

		WaterTankerBookingResponse response = WaterTankerBookingResponse.builder().waterTankerBookingApplication(waterTankerDetail)
				.responseInfo(RequestServiceUtil.createReponseInfo(waterTankerRequest.getRequestInfo(),
						RequestServiceConstants.APPLICATION_UPDATED, StatusEnum.SUCCESSFUL))
				.build();
		return new ResponseEntity<WaterTankerBookingResponse>(response, HttpStatus.OK);
	}

	@PostMapping("/mobile-toilet/v1/_create")
	@Operation(summary = "Create application details",description = "Create Mobile Toilet Application with all the suitable details")
	public ResponseEntity<MobileToiletBookingResponse> createMobileToiletBooking(

			@RequestBody MobileToiletBookingRequest mobileToiletbookingRequest) {
		log.info("mobileToiletbookingRequest : {}" , mobileToiletbookingRequest);
		validatorService.validateRequest(mobileToiletbookingRequest);
		MobileToiletBookingDetail mobileToiletDetail = mobileToiletService.createNewMobileToiletBookingRequest(mobileToiletbookingRequest);
		ResponseInfo info = RequestServiceUtil.createReponseInfo(mobileToiletbookingRequest.getRequestInfo(),
				RequestServiceConstants.MT_BOOKING_CREATED, StatusEnum.SUCCESSFUL);
		MobileToiletBookingResponse response = MobileToiletBookingResponse.builder()
				.mobileToiletBookingApplication(mobileToiletDetail)
				.responseInfo(info).build();
		return new ResponseEntity<MobileToiletBookingResponse>(response, HttpStatus.OK);
	}

	@PostMapping("/mobile-toilet/v1/_search")
	@Operation(summary = "Search application details",description = "Search Mobile Toilet Application details")
	public ResponseEntity<MobileToiletBookingSearchResponse> searchMobileToiletBookingDetails(
			 @Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@ModelAttribute MobileToiletBookingSearchCriteria mobileToiletBookingSearchCriteria) {

		List<MobileToiletBookingDetail> applications = null;
		Integer count = 0;

		applications =mobileToiletService.getMobileToiletBookingDetails(requestInfoWrapper.getRequestInfo(),
				mobileToiletBookingSearchCriteria);

		count = mobileToiletService.getApplicationsCount(mobileToiletBookingSearchCriteria,
				requestInfoWrapper.getRequestInfo());

		/*
		 * Create Response Info with success status and used utilize method to generate
		 * standardized response
		 */
		ResponseInfo responseInfo = RequestServiceUtil.createReponseInfo(requestInfoWrapper.getRequestInfo(),
				RequestServiceConstants.MOBILE_TOILET_BOOKING_DETAIL_FOUND, StatusEnum.SUCCESSFUL);
		/*
		 * Build search response using builder and retrieve booking details and response
		 * metadata
		 */
		MobileToiletBookingSearchResponse response = MobileToiletBookingSearchResponse.builder()
				.mobileToiletBookingDetails(applications).responseInfo(responseInfo).count(count).build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/mobile-toilet/v1/_update")
	@Operation(summary = "Update application details",description = "Update Mobile Toilet details and RequestInfo meta data.")
	public ResponseEntity<MobileToiletBookingResponse> mobileToiletUpdate(
			@RequestBody MobileToiletBookingRequest mobileToiletRequest) {

		MobileToiletBookingDetail mobileToiletDetail = mobileToiletService.updateMobileToiletBooking(mobileToiletRequest, null);

		MobileToiletBookingResponse response = MobileToiletBookingResponse.builder().mobileToiletBookingApplication(mobileToiletDetail)
				.responseInfo(RequestServiceUtil.createReponseInfo(mobileToiletRequest.getRequestInfo(),
						RequestServiceConstants.APPLICATION_UPDATED, StatusEnum.SUCCESSFUL))
				.build();
		return new ResponseEntity<MobileToiletBookingResponse>(response, HttpStatus.OK);
	}
}
