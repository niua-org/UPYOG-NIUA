package org.upyog.Automation.config;

public class ModuleData {

    private String moduleName;

    private CitizenData citizen;

    private EmployeeData employee;

    private VendorData vendor;

    public ModuleData() {
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public CitizenData getCitizen() {
        return citizen;
    }

    public void setCitizen(CitizenData citizen) {
        this.citizen = citizen;
    }

    public EmployeeData getEmployee() {
        return employee;
    }

    public void setEmployee(EmployeeData employee) {
        this.employee = employee;
    }

    public VendorData getVendor() {
        return vendor;
    }

    public void setVendor(VendorData vendor) {
        this.vendor = vendor;
    }
}