package org.upyog.Automation.Reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;

public class ReportManager {

    private static final ExtentReports extent =
            ExtentManager.getInstance();

    private static final ThreadLocal<ExtentTest> test =
            new ThreadLocal<>();

    public static void startTest(String moduleName) {

        ExtentTest extentTest =
                extent.createTest(moduleName);

        test.set(extentTest);
    }

    public static ExtentTest getTest() {
        return test.get();
    }

    public static boolean hasActiveTest() {
        return test.get() != null;
    }

    public static void flush() {
        extent.flush();
    }

    public static void logStep(String stepName) {

        ExtentTest extentTest = getTest();

        if (extentTest != null) {
            extentTest.pass(stepName);
        }
    }
}