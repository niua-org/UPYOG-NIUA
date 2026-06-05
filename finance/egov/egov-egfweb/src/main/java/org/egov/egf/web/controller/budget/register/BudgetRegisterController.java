package org.egov.egf.web.controller.budget.register;

import org.apache.log4j.Logger;
import org.egov.commons.CFinancialYear;
import org.egov.commons.dao.EgwStatusHibernateDAO;
import org.egov.commons.service.CFinancialYearService;
import org.egov.egf.utils.FinancialUtils;
import org.egov.eis.entity.Employee;
import org.egov.eis.service.OldEmployeeService;
import org.egov.eis.web.contract.WorkflowContainer;
import org.egov.eis.web.controller.workflow.GenericWorkFlowController;
import org.egov.infra.admin.master.entity.User;
import org.egov.infra.microservice.models.Assignment;
import org.egov.infra.microservice.models.Designation;
import org.egov.infra.microservice.models.EmployeeInfo;
import org.egov.infra.microservice.utils.MicroserviceUtils;
import org.egov.infra.security.utils.SecurityUtils;
import org.egov.infra.workflow.entity.StateHistory;
import org.egov.model.budget.BudgetRegister;
import org.egov.model.service.BudgetRegisterWorkflowService;
import org.egov.pims.commons.Position;
import org.egov.utils.FinancialConstants;
import org.hibernate.validator.constraints.SafeHtml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping("/budget/register")
public class BudgetRegisterController extends GenericWorkFlowController {

    private static final String BUDGET_HEADER_NEW = "budgetheader-new";
    private static final String BUDGET_REGISTER_VIEW = "budgetregister-view";
    private static final String BUDGET_REGISTER_WORKFLOW = "budgetregister-workflow";
    private static final String BUDGET_REGISTER_WORKFLOW_FORM = "budgetregister-workflow-form";
    private static final String BUDGET_REGISTER_ERROR = "budgetregister-error";

    private static final Logger LOGGER = Logger.getLogger(BudgetRegisterController.class);

    private static final String STATE_TYPE = "stateType";

    private static final String APPROVAL_POSITION = "approvalPosition";

    private static final String APPROVAL_DESIGNATION = "approvalDesignation";

    private static  final String[] allowedToCreateDesignations = new String[] { "Financial Management Officer", "FMO", "Accounts Officer", "AO" };

    @Autowired
    private CFinancialYearService financialYearService;

    @Autowired
    private BudgetRegisterWorkflowService budgetRegisterWorkflowService;

    @Autowired
    private CFinancialYearService cFinancialYearService;

    @Autowired
    private EgwStatusHibernateDAO egwStatusDAO;

    @Autowired
    private FinancialUtils financialUtils;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private OldEmployeeService employeeService;

    @Autowired
    private MicroserviceUtils microServiceUtil;

    @RequestMapping(value = "/new", method = { RequestMethod.GET, RequestMethod.POST })
    public String newForm(final Model model, @ModelAttribute("budgetRegister") final BudgetRegister budgetRegister,
            RedirectAttributes redirectAttributes) {

        Map<String, CFinancialYear> financialYearMap = addFinancialYears(model);

        if (financialYearMap == null || financialYearMap.get("currentFy") == null || financialYearMap.get("nextFy") == null) {
            return BUDGET_REGISTER_ERROR;
        }

        String name = "Budget-" + financialYearMap.get("nextFy").getFinYearRange();


        BudgetRegister availableBudgetRegister = budgetRegisterWorkflowService
                .findLatestByFinancialYears(financialYearMap.get("currentFy"), financialYearMap.get("nextFy"));

        if (availableBudgetRegister != null
                && availableBudgetRegister.getStatus() != null
                && availableBudgetRegister.getStatus().getCode() != null
                && !(availableBudgetRegister.getStatus().getCode().equalsIgnoreCase("rejected")
        || availableBudgetRegister.getStatus().getCode().equalsIgnoreCase("cancelled"))) {

            model.addAttribute("error", "Budget is already created for the financial year !");
        }

        budgetRegister.setBudgetRegisterName(name);
        budgetRegister.setCurrentFinancialYear(financialYearMap.get("currentFy"));
        budgetRegister.setFinancialYear(financialYearMap.get("nextFy"));

        model.addAttribute("budgetRegister", budgetRegister);


        return BUDGET_HEADER_NEW;
    }


    @PostMapping(value = "/create")
    public String create(final BudgetRegister budgetRegister, final Model model, RedirectAttributes redirectAttributes,
            final HttpServletRequest request) {

        Map<String, CFinancialYear> financialYearMap = addFinancialYears(model);

        if (financialYearMap == null || financialYearMap.get("currentFy") == null || financialYearMap.get("nextFy") == null) {
            return BUDGET_REGISTER_ERROR;
        }



        BudgetRegister availableBudgetRegister = budgetRegisterWorkflowService
                .findLatestByFinancialYears(financialYearMap.get("currentFy"), financialYearMap.get("nextFy"));

        if (availableBudgetRegister != null
                && availableBudgetRegister.getStatus() != null
                && availableBudgetRegister.getStatus().getCode() != null
                && !(availableBudgetRegister.getStatus().getCode().equalsIgnoreCase("rejected")
                || availableBudgetRegister.getStatus().getCode().equalsIgnoreCase("cancelled"))) {

            model.addAttribute("error", "Budget is already created for the financial year !");
            redirectAttributes.addAttribute("error", "Budget is already created for the financial year !");

//            budgetRegister.setBudgetRegisterName(name);
            budgetRegister.setCurrentFinancialYear(financialYearMap.get("currentFy"));
            budgetRegister.setFinancialYear(financialYearMap.get("nextFy"));

            model.addAttribute("budgetRegister", budgetRegister);
            return BUDGET_HEADER_NEW;
        }

        final CFinancialYear currentFy = cFinancialYearService
                .findOne(budgetRegister.getCurrentFinancialYear().getId());
        final CFinancialYear nextFy = cFinancialYearService.findOne(budgetRegister.getFinancialYear().getId());

        budgetRegister.setCurrentFinancialYear(currentFy);
        budgetRegister.setFinancialYear(nextFy);

        budgetRegister.setBudgetRegisterNumber(
                budgetRegisterWorkflowService.generateBudgetRegisterNumber(nextFy.getFinYearRange()));

        budgetRegister.setStatus(egwStatusDAO.getStatusByModuleAndCode(FinancialConstants.BUDGET_MODULE,
                FinancialConstants.BUDGET_CREATED_NEW));

        budgetRegisterWorkflowService.initiateBudgetRegisterWf(budgetRegister);

        redirectAttributes.addAttribute("message", "Budget Register Created !");
        redirectAttributes.addAttribute("hideError", "true");

        return "redirect:/budget/register/view";
    }

    @RequestMapping(value = "/view", method = { RequestMethod.GET, RequestMethod.POST })
    public String view(final Model model) {
        LOGGER.info("budget register view:");
        List<BudgetRegister> budgetRegisters = budgetRegisterWorkflowService.findBudgetRegisters();
        List<CFinancialYear> financialYears = financialYearService.getAllFinancialYears();


        model.addAttribute("budgetRegisters", budgetRegisters);
        model.addAttribute("financialYears", financialYears);

        return BUDGET_REGISTER_VIEW;
    }

    @RequestMapping(value = "/workflow/view/{budgetRegisterNumber}", method = { RequestMethod.GET, RequestMethod.POST })
    public String workflow(final Model model,
            @PathVariable("budgetRegisterNumber") @SafeHtml String budgetRegisterNumber,
            RedirectAttributes redirectAttributes) {

        BudgetRegister budgetRegister = budgetRegisterWorkflowService
                .findBudgetRegisterByRegisterNumber(budgetRegisterNumber);

        if (budgetRegister == null) {
            redirectAttributes.addAttribute("error", "Selected Budget register not found!");
            return "redirect:/budget/register/view";
        }

        model.addAttribute("budgetRegister", budgetRegister);

        model.addAttribute(STATE_TYPE, budgetRegister.getClass().getSimpleName());

        prepareWorkflow(model, budgetRegister, new WorkflowContainer());

        User currentUser = securityUtils.getCurrentUser();

        List<EmployeeInfo> emplist = microServiceUtil.getEmployee(currentUser.getId(), null, null, null);

        boolean allowCreate = false;

        if (emplist != null && !emplist.isEmpty()) {
            String designation = emplist.get(0).getAssignments().get(0).getDesignation();


            if (Arrays.asList(allowedToCreateDesignations).contains(designation)) {
                allowCreate = true;
            }
        }

        if (budgetRegister.getState() != null) {
            model.addAttribute("currentState", budgetRegister.getState().getValue());
            model.addAttribute("workflowHistory",
                    financialUtils.getHistory(budgetRegister.getState(), budgetRegister.getStateHistory()));
            if (budgetRegister.getCurrentState().getValue().equalsIgnoreCase("reverted") && allowCreate ) {
                model.addAttribute("allowCreate", true);
            }
        } else {
            if (currentUser.getId().equals(budgetRegister.getCreatedBy())) {
                model.addAttribute("showWorkflow", "true");
            }
            if (allowCreate) {
                model.addAttribute("allowCreate", true);
            }
        }

        return BUDGET_REGISTER_WORKFLOW;
    }

    // to show from workflow
    @RequestMapping(value = "/workflow/form/{id}", method = { RequestMethod.GET, RequestMethod.POST })
    public String workflowUpdateForm(final Model model, @PathVariable("id") @SafeHtml Long id,
            RedirectAttributes redirectAttributes) {

        // BudgetRegister budgetRegister =
        // budgetRegisterWorkflowService.findBudgetRegisterByRegisterNumber(budgetRegisterNumber);
        BudgetRegister budgetRegister = budgetRegisterWorkflowService.findOne(id);

        if (budgetRegister == null) {
            redirectAttributes.addAttribute("error", "Selected Budget register not found!");
            return "redirect:/budget/register/view";
        }

        model.addAttribute("budgetRegister", budgetRegister);

        model.addAttribute(STATE_TYPE, budgetRegister.getClass().getSimpleName());

        prepareWorkflow(model, budgetRegister, new WorkflowContainer());

        if (budgetRegister.getState() != null) {
            model.addAttribute("currentState", budgetRegister.getState().getValue());

            model.addAttribute("workflowHistory",
                    financialUtils.getHistory(budgetRegister.getState(), budgetRegister.getStateHistory()));
        }

        User user = securityUtils.getCurrentUser();

        List<EmployeeInfo> emplist = microServiceUtil.getEmployee(user.getId(), null, null, null);

        if (emplist != null && !emplist.isEmpty()) {
            EmployeeInfo currentEmployee = emplist.get(0);
           Assignment currentAssignment =  currentEmployee.getAssignments().get(currentEmployee.getAssignments().size() - 1);
           if (budgetRegister.currentAssignee().equals(currentAssignment.getPosition())) {
               model.addAttribute("showWorkflow", "true");
           }
        }

        return BUDGET_REGISTER_WORKFLOW_FORM;
    }

    @PostMapping(value = "/workflow/update")
    public String workflowUpdate(@ModelAttribute BudgetRegister budgetRegister, final Model model,
            final BindingResult resultBinder, final HttpServletRequest request,
            @RequestParam @SafeHtml final String workFlowAction) {

        // @PathVariable("budgetRegisterNumber") @SafeHtml String budgetRegisterNumber,

        LOGGER.info("work flow update !");
        LOGGER.info(budgetRegister.getBudgetRegisterNumber());

        final BudgetRegister currentBudgetRegister = budgetRegisterWorkflowService
                .findBudgetRegisterByRegisterNumber(budgetRegister.getBudgetRegisterNumber());

        // final BudgetRegister currentBudgetRegister =
        // budgetRegisterWorkflowService.findBudgetRegisterByRegisterNumber(budgetRegisterNumber);

        if (currentBudgetRegister == null) {
            return "redirect:/budget/register/workflow/view/" + budgetRegister.getBudgetRegisterNumber();
        }

        Long approvalPosition = 0l;
        String approvalComment = "";
        String approvalDesignation = "";
        if (request.getParameter("approvalComent") != null)
            approvalComment = request.getParameter("approvalComent");
        if (request.getParameter(APPROVAL_POSITION) != null && !request.getParameter(APPROVAL_POSITION).isEmpty())
            approvalPosition = Long.valueOf(request.getParameter(APPROVAL_POSITION));
        if (request.getParameter(APPROVAL_DESIGNATION) != null
                && !request.getParameter(APPROVAL_DESIGNATION).isEmpty())
            approvalDesignation = String.valueOf(request.getParameter(APPROVAL_DESIGNATION));

        LOGGER.info("comment:" + approvalComment + ", position: " + approvalPosition + ", designation: "
                + approvalDesignation + ", Action:" + workFlowAction);

        // budgetRegisterWorkflowService.create(budgetRegister, approvalPosition,
        // approvalComment, null, "FORWARD", approvalDesignation);

        budgetRegisterWorkflowService.createBudgetRegisterWorkFlowTransitionNew(currentBudgetRegister, approvalPosition,
                approvalComment, null, workFlowAction, approvalDesignation);

//        budgetRegisterWorkflowService.save(currentBudgetRegister);

        // redirectAttributes.addAttribute("message", "Budget register forwarded !");

        return "redirect:/budget/register/workflow/view/" + currentBudgetRegister.getBudgetRegisterNumber();
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
