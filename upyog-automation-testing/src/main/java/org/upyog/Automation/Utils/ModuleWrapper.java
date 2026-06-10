package org.upyog.Automation.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.Automation.Reports.ReportManager;

import java.util.List;

public class ModuleWrapper {

    private static final Logger logger =
            LoggerFactory.getLogger(ModuleWrapper.class);

    // ==============================
    // SINGLE MODULE
    // ==============================
    public static void execute(String moduleName,
                               Runnable moduleLogic) {

        runModule(moduleName, moduleLogic);
    }

    // ==============================
    // MULTIPLE MODULES
    // ==============================
    public static void executeBatch(List<ModuleTask> modules) {

        for (ModuleTask module : modules) {

            runModule(
                    module.getModuleName(),
                    module.getModuleLogic()
            );
        }
    }

    // ==============================
    // COMMON EXECUTION
    // ==============================
    private static void runModule(String moduleName,
                                  Runnable moduleLogic) {

        if (!ReportManager.hasActiveTest()) {
            ReportManager.startTest(moduleName);
        }

        logger.info("============================");
        logger.info("STARTING: {}", moduleName);
        logger.info("============================");

        long start = System.currentTimeMillis();

        try {

            moduleLogic.run();

            logger.info("PASSED: {}", moduleName);

            ReportManager.getTest()
                    .pass("Module Executed Successfully");

        } catch (Exception e) {

            logger.error("FAILED: " + moduleName, e);

            ReportManager.getTest()
                    .fail(e);

        } finally {

            long duration =
                    (System.currentTimeMillis() - start) / 1000;

            logger.info("Time Taken: {} sec", duration);

            logger.info("============================");
            logger.info("FINISHED: {}", moduleName);
            logger.info("============================");

            //ReportManager.flush();
        }
    }
}