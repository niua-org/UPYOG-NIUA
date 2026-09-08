package org.upyog.Automation.model;

public class ModuleExecutionResult {

    private String module;
    private String testCase;
    private String status;
    private String message;
    private String failedStep;

    public ModuleExecutionResult() {
    }

    public ModuleExecutionResult(String module,
                                 String status,
                                 String message) {
        this.module = module;
        this.status = status;
        this.message = message;
    }

    public ModuleExecutionResult(String module,
                                 String testCase,
                                 String status,
                                 String message,
                                 String failedStep) {
        this.module = module;
        this.testCase = testCase;
        this.status = status;
        this.message = message;
        this.failedStep = failedStep;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFailedStep() {
        return failedStep;
    }

    public void setFailedStep(String failedStep) {
        this.failedStep = failedStep;
    }
}