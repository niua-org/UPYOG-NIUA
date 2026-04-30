package org.upyog.chb.seatlock.web;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.upyog.chb.seatlock.model.BookingConfirmationResult;
import org.upyog.chb.seatlock.model.LockSeatResult;
import org.upyog.chb.seatlock.model.ReleaseSeatResult;
import org.upyog.chb.seatlock.service.TimerSeatBookingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Thin HTTP adapter — delegates to {@link TimerSeatBookingService} (no business rules here).
 */
@RestController
@RequestMapping("/seat/timer/v1")
public class SeatTimerBookingController {

	private final TimerSeatBookingService timerSeatBookingService;

	public SeatTimerBookingController(TimerSeatBookingService timerSeatBookingService) {
		this.timerSeatBookingService = timerSeatBookingService;
	}

	@PostMapping("/lock")
	public ResponseEntity<LockSeatResult> lock(@Valid @RequestBody SeatAcquireBody body) {
		var result = timerSeatBookingService.acquireLock(body.seatId(), body.userId(), Optional.ofNullable(body.ttl()));
		return ResponseEntity.ok(result);
	}

	@PostMapping("/release")
	public ResponseEntity<ReleaseSeatResult> release(@Valid @RequestBody SeatUserBody body) {
		return ResponseEntity.ok(timerSeatBookingService.releaseLock(body.seatId(), body.userId()));
	}

	@PostMapping("/confirm")
	public ResponseEntity<BookingConfirmationResult> confirm(@Valid @RequestBody SeatConfirmBody body) {
		return ResponseEntity.ok(timerSeatBookingService.confirmAfterPayment(body.seatId(), body.userId(),
				body.idempotencyKey()));
	}

	@PostMapping("/locked")
	public ResponseEntity<SeatLockedResponse> locked(@RequestParam @NotBlank String seatId) {
		return ResponseEntity.ok(new SeatLockedResponse(seatId, timerSeatBookingService.isLocked(seatId)));
	}

	public record SeatAcquireBody(@NotBlank String seatId, @NotBlank String userId, Duration ttl) {
	}

	public record SeatUserBody(@NotBlank String seatId, @NotBlank String userId) {
	}

	public record SeatConfirmBody(@NotBlank String seatId, @NotBlank String userId, @NotBlank String idempotencyKey) {
	}

	public record SeatLockedResponse(String seatId, boolean locked) {
	}
}
