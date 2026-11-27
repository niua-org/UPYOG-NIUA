package org.upyog.smoke.employee;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class EmployeeDashboardSmokeTest extends ServiceSmokeTestTemplate {

    public EmployeeDashboardSmokeTest() {
        super("employee-dashboard");
    }
}
