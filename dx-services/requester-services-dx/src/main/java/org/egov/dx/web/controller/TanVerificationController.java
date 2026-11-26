package org.egov.dx.web.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.service.TanVerificationService;
import org.egov.dx.util.TanNumberUtils;
import org.egov.dx.web.models.TanVerificationRequest;
import org.egov.dx.web.models.TanVerificationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing TAN verification endpoints for requester-services-dx.
 *
 * <p>Strictly adheres 100% to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles HTTP transport orchestration. Validation, vendor transformation, and enrichment are delegated to {@link TanVerificationService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects and depends purely on {@link TanVerificationService} abstraction.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Presentation layer is fully decoupled from vendor schemas and transport protocols.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/tan/v1")
@Slf4j
public class TanVerificationController {

    private final TanVerificationService tanVerificationService;

    /**
     * Constructor injection satisfying Dependency Inversion Principle (DIP).
     *
     * @param tanVerificationService abstraction for TAN verification business operations
     */
    @Autowired
    public TanVerificationController(TanVerificationService tanVerificationService) {
        this.tanVerificationService = tanVerificationService;
    }

    /**
     * Verifies the given TAN with the Income Tax Department database or mock provider.
     *
     * @param request {@link TanVerificationRequest} containing RequestInfo and 10-character TAN
     * @return {@link ResponseEntity} wrapping {@link TanVerificationResponse}
     */
    @PostMapping("/_verify")
    public ResponseEntity<TanVerificationResponse> verifyTan(
            @RequestBody @Valid TanVerificationRequest request) {

        String maskedTan = request != null ? TanNumberUtils.maskTan(request.getTan()) : "XXXX****XX";
        log.info("Received HTTP POST /tan/v1/_verify for masked TAN: {}", maskedTan);

        TanVerificationResponse response = tanVerificationService.verifyTan(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
