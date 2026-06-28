package org.upyog.Automation.Reports;

import java.io.File;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentManager {

    private static ExtentReports extent;

    public static synchronized ExtentReports getInstance(
            String reportName
    ) {

        if (extent == null) {

            String timestamp = new SimpleDateFormat(
                    "yyyyMMdd_HHmmss_SSS"
            )
                    .format(new Date());

            String reportsDir =
                    System.getProperty("user.dir")
                            + "/target/reports";

            new File(reportsDir).mkdirs();

            String reportPath =
                    reportsDir
                            + "/Execution_"
                            + reportName
                            + "_"
                            + timestamp
                            + ".html";
            System.out.println(
                    "Creating Report At : "
                            + reportPath
            );

            ExtentSparkReporter spark =
                    new ExtentSparkReporter(reportPath);

            spark.config().setReportName(
                    "UPYOG Automation Report"
            );

            spark.config().setDocumentTitle(
                    "UPYOG Execution Report"
            );

            extent = new ExtentReports();
            extent.attachReporter(spark);

            extent.setSystemInfo(
                    "Framework",
                    "Selenium WebDriver"
            );

            extent.setSystemInfo(
                    "Project",
                    "UPYOG Automation"
            );

            extent.setSystemInfo(
                    "Environment",
                    "UAT"
            );
        }

        return extent;
    }
    public static synchronized void reset() {
        extent = null;
    }
}