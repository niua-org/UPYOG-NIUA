package org.upyog.Automation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Data Transfer Object representing a performance test request configuration.
 * Contains execution parameters for running JMeter/performance test scripts.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PerformanceRequest {

    /**
     * Name of the performance test script to be executed.
     */
    private String scriptName;

    /**
     * Number of concurrent virtual users (threads) simulated in the test.
     */
    private int users;

    /**
     * Ramp-up period in seconds to reach the target number of users.
     */
    private int rampUp;

    /**
     * Number of iterations/loops each user will perform.
     */
    private int loopCount;

}
