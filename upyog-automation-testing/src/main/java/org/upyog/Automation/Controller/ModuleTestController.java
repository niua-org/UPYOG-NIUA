package org.upyog.Automation.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.upyog.Automation.Service.ModuleTestService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/module")
@CrossOrigin(origins = "*")
public class ModuleTestController {

    @Autowired
    private ModuleTestService moduleTestService;

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runModule(
            @RequestBody ModuleRequest request) {

        String result =
                moduleTestService.runModule(
                        request
                );

        Map<String, String> response =
                new HashMap<>();

        response.put("message", result);

        return ResponseEntity.ok(response);
    }

    public static class ModuleRequest {

        private String moduleName;

        private String baseUrl;

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}