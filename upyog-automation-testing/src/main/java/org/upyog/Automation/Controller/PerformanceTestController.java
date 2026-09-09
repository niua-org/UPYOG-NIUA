package org.upyog.Automation.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.upyog.Automation.Service.JMeterRunnerService;
import org.upyog.Automation.model.PerformanceRequest;

@RestController
@RequestMapping("/api/performance")
public class PerformanceTestController {

    @Autowired
    private JMeterRunnerService runnerService;

    @PostMapping("/run")
    public ResponseEntity<String> runTest(
            @RequestBody PerformanceRequest request){

        return ResponseEntity.ok(
                runnerService.runPerformanceTest(request));

    }

}
