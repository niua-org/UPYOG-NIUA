package org.upyog.adv.repository;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.adv.web.models.AdvertisementSearchCriteria;
import org.upyog.adv.web.models.AdvertisementSlotAvailabilityDetail;
import org.upyog.adv.web.models.AdvertisementSlotSearchCriteria;
import org.upyog.adv.web.models.BookingDetail;
import org.upyog.adv.web.models.BookingRequest;

public interface BookingRepository {

	void saveBooking(BookingRequest bookingRequest);

	Integer getBookingCount (@Valid AdvertisementSearchCriteria criteria);

	List<BookingDetail> getBookingDetails(AdvertisementSearchCriteria bookingSearchCriteria);
	List<AdvertisementSlotAvailabilityDetail> getAdvertisementSlotAvailability(
			AdvertisementSlotSearchCriteria criteria);
	
	void updateBooking(@Valid BookingRequest bookingRequest);
	
	void deleteBookingIdForTimer(String bookingId);

	Map<String, Long> getRemainingTimerValues(List<BookingDetail> bookingDetails);
	
	void updateBookingSynchronously(@Valid BookingRequest advertisementBookingRequest);

	void insertBookingIdForTimer(AdvertisementSlotSearchCriteria criteria, RequestInfo requestInfo,
			AdvertisementSlotAvailabilityDetail availabiltityDetailsResponse);

	Map<String, Long> getRemainingTimerValues(String bookingId);
	
}
