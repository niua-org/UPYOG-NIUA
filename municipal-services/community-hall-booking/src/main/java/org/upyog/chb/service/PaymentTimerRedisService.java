package org.upyog.chb.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.util.PaymentTimerKeyBuilder;
import org.upyog.chb.web.models.BookingPaymentTimerDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Optional Redis mirror of {@code eg_chb_payment_timer} rows — keys use the same hall + booking + date dimensions.
 * Not used when {@code chb.payment-timer.redis.enabled=false} (DB is the source of truth).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "chb.payment.timer.redis.enabled", havingValue = "true")
public class PaymentTimerRedisService {

	@Autowired
	private StringRedisTemplate redis;

	@Autowired
	private CommunityHallBookingConfiguration bookingConfiguration;

	/**
	 * Attempts to acquire a Redis slot hold for a payment timer detail.
	 *
	 * @param detail timer details identifying the slot and booking reference
	 * @return true when the Redis slot hold was acquired or renewed; false when held by another booking
	 */
	public boolean tryAcquireSlot(BookingPaymentTimerDetails detail,String startTime , String endTime) {
		var key = PaymentTimerKeyBuilder.toRedisSlotKey(detail,startTime , endTime);
		var value = PaymentTimerKeyBuilder.toRedisValue(detail.getCreatedBy(), detail.getBookingId());
		var ttl = paymentTimerDuration();
		var acquired = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl));
		if (acquired) {
			return true;
		}
		var existing = redis.opsForValue().get(key);
		if (value.equals(existing)) {
			redis.expire(key, ttl);
			return true;
		}
		log.info("Redis slot hold conflict key={} existingHolder={}", key, existing);
		return false;
	}

	/**
	 * Synchronizes persisted payment timer rows into the Redis mirror store.
	 *
	 * @param details list of timer rows to mirror in Redis
	 * @param toTime 
	 * @param fromTime 
	 */
	public void syncTimerRows(List<BookingPaymentTimerDetails> details, String fromTime, String toTime) {
		var ttl = paymentTimerDuration();
		for (var detail : details) {
			var rowKey = PaymentTimerKeyBuilder.toRedisTimerRowKey(detail,fromTime,toTime);
			var value = PaymentTimerKeyBuilder.toRedisValue(detail.getCreatedBy(), detail.getBookingId());
			redis.opsForValue().set(rowKey, value, ttl);
			log.debug("Redis timer row key synced: {}", rowKey);
		}
	}

	/**
	 * Removes Redis mirror rows for the provided timer details.
	 *
	 * @param details list of timer rows to remove from Redis
	 * @param toTime 
	 * @param fromTime 
	 */
	public void removeTimerRows(List<BookingPaymentTimerDetails> details, String fromTime, String toTime) {
		for (var detail : details) {
			redis.delete(PaymentTimerKeyBuilder.toRedisTimerRowKey(detail,fromTime , toTime));
			redis.delete(PaymentTimerKeyBuilder.toRedisSlotKey(detail,fromTime,toTime));
		}
	}

	private Duration paymentTimerDuration() {
		return Duration.ofMinutes(Integer.parseInt(bookingConfiguration.getBookingPaymentTimerValue()));
	}
}
