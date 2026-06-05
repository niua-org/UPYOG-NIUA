package org.egov.egf.web.actions.report;


import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.apache.struts2.json.annotations.JSON;
import org.codehaus.jackson.JsonGenerator;
import org.egov.commons.CFinancialYear;
import org.egov.commons.dao.FinancialYearDAO;
import org.egov.egf.model.DashboardReport;
import org.egov.infra.web.struts.actions.BaseFormAction;
import org.egov.infra.web.struts.annotation.ValidationErrorPage;
import org.egov.model.masters.Contractor;
import org.egov.services.report.DashboardReportService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@ParentPackage("egov")
@Results({
        @Result(name = "viewReport", location = "dashboardReport-viewReport.jsp"),
        @Result(name = "viewForm", location = "dashboardReport-viewForm.jsp")
})
public class DashboardReportAction extends BaseFormAction {


    private Logger LOGGER = LoggerFactory.getLogger(DashboardReport.class);

    @Autowired
    private DashboardReportService dashboardReportService;

    private final DashboardReport dashboardReport = new DashboardReport();

    @Autowired
    private FinancialYearDAO financialYearDAO;


    @Override
    public Object getModel() {
        return dashboardReport;
    }


    @SkipValidation
    @Action(value = "/report/dashboardReport-viewReport")
    public String viewReport() {

        try {

            Date currentDate = new Date();

            CFinancialYear cFinancialYear = financialYearDAO.getFinancialYearByDate(currentDate);

        } catch (Exception e) {
            e.printStackTrace();
            addActionError("The financial year is not created or not active.");
            return "viewForm";
        }


        dashboardReportService.buildDashboardReport(dashboardReport, null, null);

        return "viewReport";
    }


    @SkipValidation
    @Action(value = "/report/dashboardReport-viewForm")
    public String viewForm() {
        return "viewForm";
    }



    @ValidationErrorPage(value = "viewForm")
    @Action(value = "/report/dashboardReport-viewFilteredReport")
    public String viewFilteredReport() {

        HttpServletRequest request = ServletActionContext.getRequest();

        try {

            String sDate =  request.getParameter("startDate");
            String eDate = request.getParameter("endDate");

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

            Date startDate =  simpleDateFormat.parse(sDate);
            Date endDate =  simpleDateFormat.parse(eDate);

            dashboardReportService.buildDashboardReport(dashboardReport, startDate, endDate);


        } catch (Exception e) {
//            e.printStackTrace();
        }


        return "viewReport";
    }


    @Override
    public void validate() {
        HttpServletRequest request = ServletActionContext.getRequest();

        LOGGER.info("inside validation");

            String startDate = request.getParameter("startDate");
            String endDate = request.getParameter("endDate");

            if (startDate == null || startDate.isEmpty()) {
                addFieldError("startDate", "Start Date is required.");
            }

            if (endDate == null || endDate.isEmpty()) {
                addFieldError("endDate", "End Date is required.");
            }


            if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    Date sDate = sdf.parse(startDate);
                    Date eDate = sdf.parse(endDate);

                    if (sDate.after(eDate)) {
                        addActionError("Start Date cannot be after End Date.");
                    }

//                   Boolean inSameFy =  financialYearDAO.isSameFinancialYear(sDate, eDate);

                    try {
                        CFinancialYear cFinancialYear = financialYearDAO.getFinancialYearByDate(sDate);
                    } catch (Exception e) {
                        throw new Exception("The financial year is not created or not active.");
                    }


                    try {
                        if (!financialYearDAO.isSameFinancialYear(sDate, eDate)) {
                            addActionError("The start and end date must be in a financial year.");
                        }
                    } catch (Exception e) {
                        throw new Exception("The financial year is not created or inactive.");
                    }




                } catch (Exception e) {
//                    e.printStackTrace();
                    addActionError(e.getMessage());
                }
            }




        // Add similar blocks for other forms using different "formName"
    }

}
