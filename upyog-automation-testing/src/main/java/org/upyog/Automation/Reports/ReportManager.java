package org.upyog.Automation.Reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;

public class ReportManager {

    private static ExtentReports extent;

    private static ExtentTest test;

    public static void startTest(
            String reportName,
            String testName
    ) {

        extent =
                ExtentManager.getInstance(
                        reportName
                );

        test =
                extent.createTest(
                        testName
                );

        System.out.println(
                "REPORT INSTANCE = " + extent
        );

        System.out.println(
                "TEST INSTANCE = " + test
        );
    }

    public static ExtentTest getTest() {
        return test;
    }

    public static boolean hasActiveTest() {
        return test != null;
    }

    public static void clearTest() {
        test = null;
    }

    public static void flush() {

        if (extent != null) {
            extent.flush();
        }
    }

    public static void logStep(String stepName) {

        System.out.println(
                "LOG STEP CALLED = " + stepName
        );

        ExtentTest extentTest = getTest();

        System.out.println(
                "CURRENT TEST = " + extentTest
        );

        if (extentTest != null) {
            extentTest.pass(stepName);
        }
    }
    public static void logFlow(String flowName) {

        ExtentTest extentTest = getTest();

        if (extentTest != null) {

            extentTest.info(
                    "===================="
            );

            extentTest.info(flowName);

            extentTest.info(
                    "===================="
            );
        }
    }
    public static void logFailure(String stepName) {

        ExtentTest extentTest = getTest();

        if (extentTest != null) {
            extentTest.fail(stepName);
        } else {
            System.out.println(
                    "NO ACTIVE TEST : " + stepName
            );
        }
    }
}