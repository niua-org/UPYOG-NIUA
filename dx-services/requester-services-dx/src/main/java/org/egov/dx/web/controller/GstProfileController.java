package org.egov.dx.web.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.service.GstProfileService;
import org.egov.dx.util.GstNumberUtils;
import org.egov.dx.web.models.GstProfileRequest;
import org.egov.dx.web.models.GstProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing GST taxpayer profile and verification endpoints for requester-services-dx.
 *
 * <p>Strictly adheres 100% to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles HTTP transport orchestration. Validation, vendor transformation, and enrichment are delegated to {@link GstProfileService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects and depends purely on {@link GstProfileService} abstraction.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Presentation layer is fully decoupled from vendor schemas and transport protocols.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/gst/v1")
@Slf4j
public class GstProfileController {

    private final GstProfileService gstProfileService;

    /**
     * Constructor injection satisfying Dependency Inversion Principle (DIP).
     *
     * @param gstProfileService abstraction for GST verification business operations
     */
    @Autowired
    public GstProfileController(GstProfileService gstProfileService) {
        this.gstProfileService = gstProfileService;
    }

    /**
     * Searches taxpayer details for the given 15-character GSTIN.
     *
     * @param request {@link GstProfileRequest} containing RequestInfo and 15-character GSTIN
     * @return {@link ResponseEntity} wrapping {@link GstProfileResponse}
     */
    @PostMapping("/_search")
    public ResponseEntity<GstProfileResponse> searchGstin(
            @RequestBody @Valid GstProfileRequest request) {

        String maskedGstin = request != null ? GstNumberUtils.maskGstin(request.getGstin()) : "XX****XX";
        log.info("Received HTTP POST /gst/v1/_search for masked GSTIN: {}", maskedGstin);

        GstProfileResponse response = gstProfileService.searchGstin(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
