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

	public boolean tryAcquireSlot(BookingPaymentTimerDetails detail) {
		var key = PaymentTimerKeyBuilder.toRedisSlotKey(detail);
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

	public void syncTimerRows(List<BookingPaymentTimerDetails> details) {
		var ttl = paymentTimerDuration();
		for (var detail : details) {
			var rowKey = PaymentTimerKeyBuilder.toRedisTimerRowKey(detail);
			var value = PaymentTimerKeyBuilder.toRedisValue(detail.getCreatedBy(), detail.getBookingId());
			redis.opsForValue().set(rowKey, value, ttl);
			log.debug("Redis timer row key synced: {}", rowKey);
		}
	}

	public void removeTimerRows(List<BookingPaymentTimerDetails> details) {
		for (var detail : details) {
			redis.delete(PaymentTimerKeyBuilder.toRedisTimerRowKey(detail));
			redis.delete(PaymentTimerKeyBuilder.toRedisSlotKey(detail));
		}
	}

	private Duration paymentTimerDuration() {
		return Duration.ofMinutes(Integer.parseInt(bookingConfiguration.getBookingPaymentTimerValue()));
	}
}
