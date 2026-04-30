package org.upyog.chb.seatlock.redis;

import java.time.Duration;
import java.time.Instant;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.upyog.chb.seatlock.api.SeatLockService;
import org.upyog.chb.seatlock.config.SeatLockProperties;
import org.upyog.chb.seatlock.model.ExtendLockResult;
import org.upyog.chb.seatlock.model.LockSeatResult;
import org.upyog.chb.seatlock.model.ReleaseSeatResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisSeatLockService implements SeatLockService {

	private final StringRedisTemplate redis;
	private final SeatLockProperties props;
	private final DefaultRedisScript<Long> acquireScript;
	private final DefaultRedisScript<Long> unlockScript;

	public RedisSeatLockService(StringRedisTemplate redis, SeatLockProperties props) {
		this.redis = redis;
		this.props = props;
		this.acquireScript = new DefaultRedisScript<>();
		this.acquireScript.setScriptText(RedisSeatLockScripts.ACQUIRE_OR_RENEW);
		this.acquireScript.setResultType(Long.class);
		this.unlockScript = new DefaultRedisScript<>();
		this.unlockScript.setScriptText(RedisSeatLockScripts.UNLOCK_IF_OWNER);
		this.unlockScript.setResultType(Long.class);
	}

	@Override
	public LockSeatResult lockSeat(String seatId, String userId, Duration ttl) {
		var v = validate(seatId, userId, ttl);
		if (v.isPresent()) {
			return v.get();
		}
		var key = redisKey(seatId);
		var ttlMs = ttl.toMillis();
		var code = redis.execute(acquireScript, java.util.List.of(key), userId, String.valueOf(ttlMs));
		long resultCode = (code == null) ? 0L : code;
		if (resultCode == 1L || resultCode == 2L) {
			return new LockSeatResult.Acquired(seatId, userId, Instant.now().plusMillis(ttlMs));
		} else if (resultCode == 0L) {
			return new LockSeatResult.AlreadyLocked(seatId, readHolder(key).orElse("unknown"));
		} else {
			return new LockSeatResult.InvalidArgument("Unexpected script result: " + code);
		}
	}

	@Override
	public ReleaseSeatResult releaseSeat(String seatId, String userId) {
		requireIds(seatId, userId);
		var key = redisKey(seatId);
		var deleted = redis.execute(unlockScript, java.util.List.of(key), userId);
		if (deleted > 0) {
			return new ReleaseSeatResult.Released(seatId);
		}
		if (Boolean.FALSE.equals(redis.hasKey(key))) {
			return new ReleaseSeatResult.NotLocked(seatId);
		}
		return new ReleaseSeatResult.NotOwner(seatId, userId);
	}

	@Override
	public boolean isSeatLocked(String seatId) {
		if (seatId == null || seatId.isBlank()) {
			return false;
		}
		return Boolean.TRUE.equals(redis.hasKey(redisKey(seatId)));
	}

	@Override
	public ExtendLockResult extendLock(String seatId, String userId, Duration ttl) {
		var v = validate(seatId, userId, ttl);
		if (v.isPresent()) {
			var validationResult = v.get();
			if (validationResult instanceof LockSeatResult.InvalidArgument) {
				return new ExtendLockResult.InvalidArgument(((LockSeatResult.InvalidArgument) validationResult).message());
			} else if (validationResult instanceof LockSeatResult.RateLimited) {
				return new ExtendLockResult.InvalidArgument(((LockSeatResult.RateLimited) validationResult).message());
			} else {
				return new ExtendLockResult.InvalidArgument("unexpected validation branch");
			}
		}
		var key = redisKey(seatId);
		var ttlMs = ttl.toMillis();
		var code = redis.execute(acquireScript, java.util.List.of(key), userId, String.valueOf(ttlMs));
		long resultCode = (code == null) ? 0L : code;
		if (resultCode == 1L || resultCode == 2L) {
			return new ExtendLockResult.Extended(seatId, Instant.now().plusMillis(ttlMs));
		} else if (resultCode == 0L) {
			return new ExtendLockResult.NotOwner(seatId);
		} else {
			return new ExtendLockResult.InvalidArgument("Unexpected script result: " + code);
		}
	}

	public static boolean isRedisInfrastructureError(Throwable t) {
		return t instanceof RedisConnectionFailureException || t instanceof RedisSystemException
				|| t instanceof QueryTimeoutException;
	}

	private String redisKey(String seatId) {
		return props.redisKeyPrefix() + seatId;
	}

	private java.util.Optional<LockSeatResult> validate(String seatId, String userId, Duration ttl) {
		if (seatId == null || seatId.isBlank() || userId == null || userId.isBlank()) {
			return java.util.Optional.of(new LockSeatResult.InvalidArgument("seatId and userId required"));
		}
		if (ttl == null || ttl.isNegative() || ttl.isZero()) {
			return java.util.Optional.of(new LockSeatResult.InvalidArgument("ttl must be positive"));
		}
		return java.util.Optional.empty();
	}

	private void requireIds(String seatId, String userId) {
		if (seatId == null || seatId.isBlank() || userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("seatId and userId required");
		}
	}

	private java.util.Optional<String> readHolder(String key) {
		return java.util.Optional.ofNullable(redis.opsForValue().get(key));
	}
}