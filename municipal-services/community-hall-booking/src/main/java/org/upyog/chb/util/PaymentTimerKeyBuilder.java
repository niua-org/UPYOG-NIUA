package org.upyog.chb.util;

import java.time.LocalDate;

import org.apache.commons.lang.StringUtils;
import org.upyog.chb.web.models.BookingPaymentTimerDetails;

/**
 * Keys aligned with {@code eg_chb_payment_timer} columns (tenant, community hall, hall, date, booking id).
 */
public final class PaymentTimerKeyBuilder {

	private static final String REDIS_TIMER_PREFIX = "chb:payment-timer:row:";
	private static final String REDIS_SLOT_PREFIX = "chb:payment-timer:slot:";

	private PaymentTimerKeyBuilder() {
	}

	/** One Redis key per timer row (matches DB uniqueness: hall + booking code + date+time). */
	public static String toRedisTimerRowKey(BookingPaymentTimerDetails detail,String startTime , String endTime) {
		return toRedisTimerRowKey(detail.getTenantId(), detail.getVenuecode(), detail.getCode(),
				detail.getBookingDate(), startTime , endTime,detail.getBookingId());
	}

	public static String toRedisTimerRowKey(String tenantId, String communityHallCode, String hallCode,
			LocalDate bookingDate, String bookingId,String startTime , String endTime) {
		validate(tenantId, communityHallCode, hallCode, bookingDate, bookingId);
		return REDIS_TIMER_PREFIX + String.join(":", tenantId, communityHallCode, hallCode, bookingDate.toString(),
				bookingId,startTime, endTime);
	}

	/** Slot hold key (tenant + hall + date) — prevents double booking on the same physical slot. */
	public static String toRedisSlotKey(BookingPaymentTimerDetails detail,String startTime , String endTime) {
		return toRedisSlotKey(detail.getTenantId(), detail.getVenuecode(), detail.getCode(),
				detail.getBookingDate(),startTime , endTime);
	}

	public static String toRedisSlotKey(String tenantId, String communityHallCode, String hallCode,
			LocalDate bookingDate,String startTime , String endTime) {
		if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(communityHallCode) || StringUtils.isBlank(hallCode)
				|| bookingDate == null || startTime == null || endTime == null) {
			throw new IllegalArgumentException("tenantId, communityHallCode, hallCode and bookingDate are required");
		}
		return REDIS_SLOT_PREFIX + String.join(":", tenantId, communityHallCode, hallCode, bookingDate.toString(),startTime , endTime);
	}

	public static String toRedisValue(String createdBy, String bookingId) {
		return createdBy + "|" + bookingId;
	}

	public static BookingPaymentTimerDetails toTimerDetails(String tenantId, String communityHallCode, String hallCode,
			LocalDate bookingDate, String bookingId, String userId, long createdTime) {
		var details = new BookingPaymentTimerDetails();
		details.setTenantId(tenantId);
		details.setVenuecode(communityHallCode);
		details.setCode(hallCode);
		details.setBookingDate(bookingDate);
		details.setBookingId(bookingId);
		details.setCreatedBy(userId);
		details.setCreatedTime(createdTime);
		details.setStatus("ACTIVE");
		return details;
	}

	private static void validate(String tenantId, String communityHallCode, String hallCode, LocalDate bookingDate,
			String bookingId) {
		if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(communityHallCode) || StringUtils.isBlank(hallCode)
				|| bookingDate == null || StringUtils.isBlank(bookingId)) {
			throw new IllegalArgumentException(
					"tenantId, communityHallCode, hallCode, bookingDate and bookingId are required");
		}
	}
}
