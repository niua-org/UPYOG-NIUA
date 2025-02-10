package com.gis.property.controller;

import com.gis.property.model.Feature;
import com.gis.property.repository.dto.FeatureRequest;
import com.gis.property.service.FeatureService;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@lombok.extern.slf4j.Slf4j
@Slf4j
@RestController
@RequestMapping("/maps/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveFeature(@RequestBody FeatureRequest request) throws ParseException {
        //WKTReader reader = new WKTReader();
        log.info("payload: {}" , request.toString());
        Feature savedFeature = featureService.saveFeature(
                request.getType(),
                request.getName(),
                request.getWktGeometry()
        );

        // ✅ Create a response JSON to send back
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Feature saved successfully");
        response.put("id", savedFeature.getId());

        return ResponseEntity.ok(response);

        //return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Feature>> getAllFeatures() {
        return ResponseEntity.ok(featureService.getAllFeatures());
    }
}
