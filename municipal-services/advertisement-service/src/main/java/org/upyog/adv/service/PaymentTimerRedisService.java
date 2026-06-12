package org.upyog.adv.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.util.PaymentTimerKeyBuilder;
import org.upyog.adv.web.models.BookingPaymentTimerDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Optional Redis mirror of {@code eg_adv_payment_timer} rows — keys use the same slot + date dimensions.
 * Not used when {@code adv.payment.timer.redis.enabled=false} (DB is the source of truth).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "adv.payment.timer.redis.enabled", havingValue = "true")
public class PaymentTimerRedisService {

	@Autowired
	private StringRedisTemplate redis;

	@Autowired
	private BookingConfiguration bookingConfiguration;

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
		return Duration.ofMillis(bookingConfiguration.getPaymentTimer());
	}
}
