package org.upyog.Automation.Service;

import org.springframework.stereotype.Service;
import org.upyog.Automation.model.PerformanceRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Service
public class JMeterRunnerService {

    public String runPerformanceTest(PerformanceRequest request) {
        System.out.println("Script Name : " + request.getScriptName());
        System.out.println("Users : " + request.getUsers());
        System.out.println("Ramp Up : " + request.getRampUp());
        System.out.println("Loop Count : " + request.getLoopCount());

        try {

            String jmeterHome = "/Users/niua-l1232/Downloads/apache-jmeter-5.6.3";

            String script = "/Users/niua-l1232/Desktop/Performance Testing/Scripts/Get Post API.jmx";

            String result = "/Users/niua-l1232/Desktop/Performance Testing/Results/login_result.jtl";

            String report = "/Users/niua-l1232/Desktop/Performance Testing/Reports/Login_Report_From_Java";
            File file = new File(result);

            if (file.exists()) {
                file.delete();
            }

            ProcessBuilder pb = new ProcessBuilder(
                    jmeterHome + "/bin/jmeter",
                    "-n",
                    "-t", script,
                    "-l", result,
                    "-e",
                    "-o", report,

                    "-Jusers=" + request.getUsers(),
                    "-Jrampup=" + request.getRampUp(),
                    "-Jloop=" + request.getLoopCount()
            );
            pb.redirectErrorStream(true);

            pb.inheritIO();

            System.out.println("========== JMeter Command ==========");
            pb.command().forEach(System.out::println);
            System.out.println("====================================");

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();

            return "JMeter Finished. Exit Code = " + exitCode;

        } catch (Exception e) {

            e.printStackTrace();

            return e.getMessage();

        }

    }

}
