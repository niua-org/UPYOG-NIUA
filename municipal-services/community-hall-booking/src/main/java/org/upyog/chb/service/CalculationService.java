package org.upyog.chb.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.constants.CommunityHallBookingConstants;
import org.upyog.chb.util.CalculationTypeCache;
import org.upyog.chb.util.CommunityHallBookingUtil;
import org.upyog.chb.util.MdmsUtil;
import org.upyog.chb.web.models.BookingSlotDetail;
import org.upyog.chb.web.models.CalculationType;
import org.upyog.chb.web.models.VenueBookingRequest;
import org.upyog.chb.web.models.billing.DemandDetail;
import org.upyog.chb.web.models.billing.TaxHeadMaster;

import lombok.extern.slf4j.Slf4j;

/**
 * This service class handles the calculation logic for the Community Hall Booking module.
 * 
 * Purpose:
 * - To calculate charges, taxes, and other financial details for community hall bookings.
 * - To ensure accurate and consistent calculations based on predefined rules and configurations.
 * 
 * Dependencies:
 * - MdmsUtil: Fetches and processes master data from MDMS for calculation purposes.
 * - CalculationTypeCache: Caches calculation types to improve performance and reduce redundant lookups.
 * - CommunityHallBookingConfiguration: Provides configuration properties for calculation operations.
 * - CommunityHallBookingUtil: Utility class for common operations related to bookings.
 * 
 * Features:
 * - Calculates demand details for bookings, including tax and charge breakdowns.
 * - Retrieves and applies calculation types from MDMS or cache.
 * - Handles rounding and precision for financial calculations.
 * - Logs important operations and errors for debugging and monitoring purposes.
 * 
 * Methods:
 * 1. calculateDemandDetails:
 *    - Calculates the demand details for a booking based on the provided request.
 *    - Generates a list of DemandDetail objects representing the charges and taxes.
 * 
 * 2. getCalculationType:
 *    - Retrieves the calculation type for a booking from MDMS or cache.
 *    - Ensures that the correct calculation rules are applied for the booking.
 * 
 * Usage:
 * - This class is automatically managed by Spring and injected wherever calculation logic is required.
 * - It ensures consistent and reusable logic for financial calculations in the module.
 */
@Slf4j
@Service
public class CalculationService {
	
	@Autowired
	private MdmsUtil mdmsUtil;
	
	@Autowired
	private CalculationTypeCache calculationTypeCache;
	
	@Autowired
	private CommunityHallBookingConfiguration config;

	/**
	 * Calculates the list of demands (charges + applicable taxes) for a community hall booking.
	 * <p>
	 * The method:
	 * <ul>
	 *   <li>Fetches tax head master records and tax calculation rules from MDMS/cache</li>
	 *   <li>Computes charge amount based on booking slot duration (hours)</li>
	 *   <li>Derives tax amounts using configured tax rates</li>
	 * </ul>
	 * </p>
	 *
	 * @param bookingRequest booking request containing booking application details
	 * @return list of demand details (including tax breakdowns)
	 */
	public List<DemandDetail> calculateDemand(VenueBookingRequest bookingRequest) {

		String tenantId = CommunityHallBookingUtil.getTenantId(bookingRequest.getVenueBookingApplication().getTenantId());
		
		List<TaxHeadMaster> headMasters = mdmsUtil.getTaxHeadMasterList(bookingRequest.getRequestInfo(), tenantId , CommunityHallBookingConstants.BILLING_SERVICE);
		
		List<CalculationType> calculationTypes = calculationTypeCache.getcalculationType(bookingRequest.getRequestInfo(), bookingRequest.getVenueBookingApplication().getTenantId() , config.getModuleName(), bookingRequest.getVenueBookingApplication());

		log.info("Retrieved calculation types: {}", calculationTypes);

		List<DemandDetail> demandDetails = processCalculationForDemandGeneration(tenantId, calculationTypes,
				bookingRequest, headMasters);

		return demandDetails;

	}

	private List<DemandDetail> processCalculationForDemandGeneration(String tenantId,
			List<CalculationType> calculationTypes, VenueBookingRequest bookingRequest, List<TaxHeadMaster> headMasters) {

		// Calculate total booking hours instead of days
		Map<String, BigDecimal> hallCodeBookingHoursMap = bookingRequest.getVenueBookingApplication().getBookingSlotDetails()
				.stream().collect(Collectors.groupingBy(BookingSlotDetail::getUnitCode, 
						Collectors.mapping(this::calculateHours, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
		
		Map<String, Long> hallCodeBookingDaysMap = bookingRequest.getVenueBookingApplication().getBookingSlotDetails()
				.stream()
				.collect(Collectors.groupingBy(BookingSlotDetail::getUnitCode,
						Collectors.mapping(BookingSlotDetail::getBookingDate,
								Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));
		
		//Demand will be generated using billing service for booking
		final List<DemandDetail> demandDetails = new LinkedList<>();
		
        List<String> taxHeadCodes = headMasters.stream().map(head -> head.getCode()).collect(Collectors.toList());
		
		log.info("tax head codes  : " + taxHeadCodes);

		//Demand for which tax is applicable is stored
		List<CalculationType> taxableFeeType = new ArrayList<>();
		
		BigDecimal hallCodeBookingHours = hallCodeBookingHoursMap.get(bookingRequest.getVenueBookingApplication().getBookingSlotDetails().get(0).getUnitCode());
		Long venueBookingDates = hallCodeBookingDaysMap.get(bookingRequest.getVenueBookingApplication().getBookingSlotDetails().get(0).getUnitCode()); 
		//We have two type of fee 1.taxable(Booking fee, electricity fee etc) and 2.fixed( Security deposit)
		for (CalculationType type : calculationTypes) {
			if (taxHeadCodes.contains(type.getFeeType())) {
				if (type.isTaxApplicable()) {
					//Add taxable fee 
					taxableFeeType.add(type);
				} else {
					DemandDetail data =  DemandDetail.builder().taxAmount(type.getAmount())
					.taxHeadMasterCode(type.getFeeType()).tenantId(tenantId).build();
					//Add fixed fee for which tax is not applicable like security deposit
					demandDetails.add(data);
				}
			}
		}
		
		log.info("taxable fee type : " + taxableFeeType);

		//Calculating taxable demand as per no of hours/day for taxable fee
		List<DemandDetail> taxableDemands = taxableFeeType.stream().map(data -> {

			BigDecimal multiplier;

			if ("HOURLY".equalsIgnoreCase(data.getCalculationDurationType())) {
				multiplier = hallCodeBookingHours;
			} else if ("DAY".equalsIgnoreCase(data.getCalculationDurationType())) {
				multiplier = BigDecimal.valueOf(venueBookingDates);
			} else {
				multiplier = BigDecimal.ONE; // fallback
			}

			return DemandDetail.builder().taxAmount(data.getAmount().multiply(multiplier))
					.taxHeadMasterCode(data.getFeeType()).tenantId(tenantId).build();
		}).collect(Collectors.toList());
		
		
		log.info("taxableDemands : " + taxableDemands);
		
		//Adding taxable demands to demand details
		demandDetails.addAll(taxableDemands);
		
		BigDecimal totalTaxableAmount = taxableDemands.stream()
				.map(DemandDetail::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

		log.info("Total Taxable amount for the booking : " + totalTaxableAmount);
		
		calculateTaxDemands(bookingRequest, tenantId, demandDetails, totalTaxableAmount);

		return demandDetails;
	}
	
	private void calculateTaxDemands(VenueBookingRequest bookingRequest, String tenantId,
			List<DemandDetail> demandDetails, BigDecimal totalTaxableAmount) {
		List<CalculationType> taxRates = mdmsUtil.getTaxRatesMasterList(bookingRequest.getRequestInfo(), tenantId,
				config.getModuleName(), bookingRequest.getVenueBookingApplication());
		taxRates.stream().forEach(tax -> {
			DemandDetail demandDetail = DemandDetail.builder()
					.taxAmount(calculateAmount(totalTaxableAmount, tax.getAmount())).taxHeadMasterCode(tax.getFeeType())
					.tenantId(tenantId).build();
			demandDetails.add(demandDetail);
		});
	}

	//Tax is in percentage
	private BigDecimal calculateAmount(BigDecimal amount, BigDecimal tax) {
		return amount.multiply(tax).divide(CommunityHallBookingConstants.ONE_HUNDRED, RoundingMode.FLOOR);
	}

	/**
	 * Calculates the number of hours between {@code bookingFromTime} and {@code bookingToTime}.
	 *
	 * @param bookingSlotDetail booking slot containing from/to times
	 * @return duration in hours as {@link BigDecimal}
	 */
	private BigDecimal calculateHours(BookingSlotDetail bookingSlotDetail) {
		long minutesDifference = java.time.temporal.ChronoUnit.MINUTES.between(
				bookingSlotDetail.getBookingFromTime(), 
				bookingSlotDetail.getBookingToTime());
		// Convert minutes to hours with decimal precision
		return BigDecimal.valueOf(minutesDifference).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
	}

}
