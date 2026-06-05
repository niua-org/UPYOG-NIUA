package org.egov.egf.web.controller.budget.header;


import org.apache.log4j.Logger;
import org.egov.commons.CFinancialYear;
import org.egov.commons.service.CFinancialYearService;
import org.egov.model.budget.header.BudgetHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/budget/header")
public class BudgetHeaderController {

    private static final String BUDGET_HEADER_NEW = "budgetheader-new";

    private static final Logger LOGGER = Logger.getLogger(BudgetHeaderController.class);


    @Autowired
    private CFinancialYearService financialYearService;


    @RequestMapping(value = "/new", method = { RequestMethod.GET, RequestMethod.POST })
    public String newForm(final Model model) {

       Map<String, CFinancialYear> financialYearMap = addFinancialYears(model);
       String name = "Budget_" + financialYearMap.get("nextFy").getFinYearRange();
       BudgetHeader budgetHeader = new BudgetHeader();
       budgetHeader.setName(name);
        model.addAttribute("budgetHeader", budgetHeader);
        return BUDGET_HEADER_NEW;
    }


    private Map<String, CFinancialYear> addFinancialYears(Model model) {
        CFinancialYear financialYear = financialYearService.getCurrentFinancialYear();
        ArrayList<String> errors = new ArrayList<>();

        if (financialYear == null) {
            model.addAttribute("errors", "Financial year not found !");
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(financialYear.getEndingDate());
        calendar.add(Calendar.DATE, 1);
        CFinancialYear nextFinancialYear = financialYearService.getFinancialYearByDate(calendar.getTime());

        if (nextFinancialYear == null) {
            model.addAttribute("errors", "Financial year not found !");
            return null;
        }

        model.addAttribute("currentFy", financialYear);
        model.addAttribute("nextFy", nextFinancialYear);

        Map<String, CFinancialYear> financialYearMap = new HashMap<>();
        financialYearMap.put("currentFy", financialYear);
        financialYearMap.put("nextFy", nextFinancialYear);

        return financialYearMap;
    }

}
