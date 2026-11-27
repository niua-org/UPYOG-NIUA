package org.egov.dx.web.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.service.PanVerificationService;
import org.egov.dx.util.PanNumberUtils;
import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.dx.web.models.PanVerificationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing PAN verification endpoints for requester-services-dx.
 *
 * <p>Strictly adheres 100% to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles HTTP transport orchestration. Validation, vendor transformation, and enrichment are delegated to {@link PanVerificationService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects and depends purely on {@link PanVerificationService} abstraction.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Presentation layer is fully decoupled from vendor schemas and transport protocols.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/pan/v1")
@Slf4j
public class PanVerificationController {

    private final PanVerificationService panVerificationService;

    /**
     * Constructor injection satisfying Dependency Inversion Principle (DIP).
     *
     * @param panVerificationService abstraction for PAN verification business operations
     */
    @Autowired
    public PanVerificationController(PanVerificationService panVerificationService) {
        this.panVerificationService = panVerificationService;
    }

    /**
     * Verifies the given PAN with the Income Tax Department database or mock provider.
     *
     * @param request {@link PanVerificationRequest} containing RequestInfo, PAN, fullName, and dob
     * @return {@link ResponseEntity} wrapping {@link PanVerificationResponse}
     */
    @PostMapping("/_verify")
    public ResponseEntity<PanVerificationResponse> verifyPan(
            @RequestBody @Valid PanVerificationRequest request) {

        String maskedPan = request != null ? PanNumberUtils.maskPan(request.getPan()) : "XXXXX****X";
        log.info("Received HTTP POST /pan/v1/_verify for masked PAN: {}", maskedPan);

        PanVerificationResponse response = panVerificationService.verifyPan(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
