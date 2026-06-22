package org.upyog.chb.validator;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.upyog.chb.constants.CommunityHallBookingConstants;
import org.upyog.chb.web.models.BookingSlotDetail;
import org.upyog.chb.web.models.VenueBookingRequest;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MDMSValidator {

	/**
	 * method to validate the mdms data in the request
	 *
	 * @param venueBookingRequest
	 */
	public void validateMdmsData(VenueBookingRequest venueBookingRequest, Object mdmsData,Object venueTypeMasterData) {

		Map<String, List<String>> masterData = getAttributeValues(mdmsData);
		/*
		 * if(MdmsUtil.getMDMSDataMap().isEmpty()) {
		 * MdmsUtil.setMDMSDataMap(masterData); }
		 */
		String[] masterArray = { CommunityHallBookingConstants.CHB_PURPOSE,
				CommunityHallBookingConstants.CHB_SPECIAL_CATEGORY, CommunityHallBookingConstants.CHB_COMMNUITY_HALLS,
				CommunityHallBookingConstants.CHB_HALL_CODES, CommunityHallBookingConstants.CHB_PARKS,
				CommunityHallBookingConstants.CHB_PARK_CODES, CommunityHallBookingConstants.CHB_STADIUMS,
				CommunityHallBookingConstants.CHB_STADIUM_CODES, CommunityHallBookingConstants.CHB_CREMATORIUMS,
				CommunityHallBookingConstants.CHB_CREMATORIUM_CODES,
				CommunityHallBookingConstants.CHB_GUEST_HOUSE_CODES, CommunityHallBookingConstants.CHB_GUEST_HOUSES,
				CommunityHallBookingConstants.CHB_DOCUMENTS };

		log.info("Validating master data from MDMS for : " + venueBookingRequest.getVenueBookingApplication().getBookingNo());

		validateIfMasterPresent(masterArray, masterData);
		
		validateTimeSlot(venueBookingRequest.getVenueBookingApplication().getBookingSlotDetails(), venueTypeMasterData,
				venueBookingRequest.getVenueBookingApplication().getVenueType());
	}


	/**
	 * Validates the provided list of booking slots against business rules and
	 * master data constraints. Checks for valid time bounds, ensures start time
	 * precedes end time, and dynamically enforces the minimum and maximum duration
	 * limits defined in the MDMS venue type master data. * @param
	 * bookingSlotDetails The list of slot details containing times and venue type
	 * codes to validate.
	 * 
	 * @param venueTypeMasterData The raw master data object (expected as a List of
	 *                            Maps) containing venue rules.
	 * @return {@code true} if all slots are valid, {@code false} otherwise.
	 */
	@SuppressWarnings("unchecked")
	private Boolean validateTimeSlot(List<BookingSlotDetail> bookingSlotDetails, Object venueTypeMasterData,
			String venueType) {

		if (bookingSlotDetails == null || bookingSlotDetails.isEmpty()) {
			return true;
		}

		// Safely cast the raw MDMS master data to a List of Maps
		// 1. Cast the top-level object to a Map
		Map<String, Object> masterDataMap = (Map<String, Object>) venueTypeMasterData;

		// 2. Fetch the "venues" list out of the map using its key
		List<Map<String, Object>> venueMasters = (List<Map<String, Object>>) masterDataMap.get("Venues");
		if (venueMasters == null || venueMasters.isEmpty()) {
			log.error("Venue Type Master Data is empty or invalid.");
			return false;
		}

		for (BookingSlotDetail slot : bookingSlotDetails) {

			LocalTime fromTime = slot.getBookingFromTime();
			LocalTime toTime = slot.getBookingToTime();

			if (fromTime == null || toTime == null) {
				return false;
			}

			// 1. Basic sanity check: Start time must be before end time
			if (!fromTime.isBefore(toTime)) {
				return false;
			}

			// 2. Find the matching master data for this slot's venue code
			// (Assuming your BookingSlotDetail object has a getVenueCode() or similar
			// method matching "PARKS", "STADIUMS", etc.)
			Map<String, Object> matchingVenue = venueMasters.stream().filter(
					master -> master.get("code") != null && master.get("code").toString().equalsIgnoreCase(venueType))
					.findFirst().orElse(null);

			if (matchingVenue == null) {
				log.warn("No matching venue configuration found for code: " + venueType);
				return false;
			}

			// 3. Extract time slot configurations
			Map<String, String> timeSlotConfig = (Map<String, String>) matchingVenue.get("timeSlot");
			if (timeSlotConfig == null) {
				return false;
			}

			long durationInMinutes = Duration.between(fromTime, toTime).toMinutes();

			// 4. Parse min and max duration bounds from "H:mm" format into minutes
			long minMinutes = parseDurationToMinutes(timeSlotConfig.get("minDuration"));
			long maxMinutes = parseDurationToMinutes(timeSlotConfig.get("maxDuration"));

			// 5. Dynamic range validation
			if (durationInMinutes < minMinutes || durationInMinutes > maxMinutes) {
				log.info("Validation failed. Requested: " + durationInMinutes + " mins. Allowed: " + minMinutes + " to "
						+ maxMinutes + " mins.");
				return false;
			}
		}

		return true;
	}

	/**
	 * Helper method to parse a duration string in "H:mm" format (e.g., "23:59",
	 * "1:00") into total minutes. * @param durationStr The duration string to
	 * parse.
	 * 
	 * @return Total duration in minutes, or a fallback constraint value if string
	 *         is invalid.
	 */
	private long parseDurationToMinutes(String durationStr) {
		if (durationStr == null || !durationStr.contains(":")) {
			return 0;
		}
		try {
			String[] parts = durationStr.split(":");
			long hours = Long.parseLong(parts[0].trim());
			long minutes = Long.parseLong(parts[1].trim());
			return (hours * 60) + minutes;
		} catch (NumberFormatException e) {
			log.error("Failed to parse duration limit string: " + durationStr, e);
			return 0;
		}
	}

	/**
	 * Fetches all the values of particular attribute as map of field name to
	 * list
	 *
	 * takes all the masters from each module and adds them in to a single map
	 *
	 * note : if two masters from different modules have the same name then it
	 *
	 * will lead to overriding of the earlier one by the latest one added to the
	 * map
	 *
	 * @return Map of MasterData name to the list of code in the MasterData
	 *
	 */
	public Map<String, List<String>> getAttributeValues(Object mdmsData) {

		List<String> modulepaths = Arrays.asList(CommunityHallBookingConstants.CHB_JSONPATH_CODE);
		final Map<String, List<String>> mdmsResMap = new HashMap<>();
		modulepaths.forEach(modulepath -> {
			try {
				mdmsResMap.putAll(JsonPath.read(mdmsData, modulepath));
			} catch (Exception e) {
				throw new CustomException(CommunityHallBookingConstants.INVALID_TENANT_ID_MDMS_KEY,
						CommunityHallBookingConstants.INVALID_TENANT_ID_MDMS_MSG);
			}
		});
		return mdmsResMap;
	}

	/**
	 * Validates if MasterData is properly fetched for the given MasterData
	 * names
	 * 
	 * @param masterNames
	 * @param codes
	 */
	private void validateIfMasterPresent(String[] masterNames, Map<String, List<String>> codes) {
		log.info("Master names in validation : " + masterNames);
		log.info("Mdms data map : " + codes);
		Map<String, String> errorMap = new HashMap<>();
		for (String masterName : masterNames) {
			if (CollectionUtils.isEmpty(codes.get(masterName))) {
				errorMap.put("MDMS DATA ERROR ", "Unable to fetch " + masterName + " codes from MDMS");
			}
		}
		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}

}
