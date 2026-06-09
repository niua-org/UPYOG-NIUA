package org.egov.egf.web.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.validation.Valid;

import org.egov.model.budget.BudgetHead;
import org.egov.model.budget.FunctionBudgetHead;
import org.egov.model.service.BudgetHeadService;
import org.egov.model.service.FunctionBudgetHeadService;
import org.egov.utils.BudgetAccountType;
import org.hibernate.validator.constraints.SafeHtml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * Controller for budget head master data management in the budget module.
 * Handles CRUD operations for budget heads and provides AJAX endpoints for autocomplete.
 * Supports function-wise budget head mapping and search by name/code or function.
 * Enables creation and viewing of budget heads with account type classification.
 */

@Controller
@RequestMapping("/budgethead")
public class BudgetHeadController {
	private static final String BUDGETHEAD_NEW = "budgethead-new";
	private static final String BUDGET_HEAD = "budgetHead";
	private static final String BUDGET_HEAD_VIEW = "budgethead-view";
	private static final String BUDGETHEAD_FUNCTION_VIEW = "budgethead-function-view";
	
	@Autowired
	private BudgetHeadService budgetHeadService;
	@Autowired
	private MessageSource messageSource;

	@Autowired
	private FunctionBudgetHeadService functionBudgetHeadService;

	private void prepareNewForm(final Model model) {
		model.addAttribute("budgetAccountTypes", Arrays.asList(BudgetAccountType.values()));
	}

	@RequestMapping(value = "/new", method = { RequestMethod.GET, RequestMethod.POST })
	public String newForm(final Model model) {
		prepareNewForm(model);
		model.addAttribute(BUDGET_HEAD, new BudgetHead());
//		budgetHeadService.getBudgetHeadList(model);
		return BUDGETHEAD_NEW;
	}

	@PostMapping(value = "/create")
	public String create(@Valid @ModelAttribute final BudgetHead budgetHead, final BindingResult errors,
			final RedirectAttributes redirectAttrs, final Model model) {

		budgetHeadService.create(budgetHead);
		redirectAttrs.addFlashAttribute("message",
				messageSource.getMessage("msg.budgetGroup.success", null, Locale.ENGLISH));
		return "redirect:/budgethead/new";
	}

	@PostMapping(value = "/view")
	public String view(final Model model) {
		budgetHeadService.getBudgetHeadList(model);

		return BUDGET_HEAD_VIEW;
	}


	@GetMapping(value = "/ajaxBudgetHead", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<BudgetHead> findBudgetHead(@RequestParam @SafeHtml final String query) {
		final List<BudgetHead> budgetHeads = budgetHeadService.findBudgetHeadByNameOrCode(query);
		return budgetHeads;
	}

	@GetMapping(value = "/ajaxBudgetHead/{functionId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<BudgetHead> findFunctionBudgetHeads(@PathVariable("functionId") Long functionId, @RequestParam @SafeHtml final String query) {
		final List<BudgetHead> budgetHeads = budgetHeadService.searchBudgetHeadsByFunctionNative(functionId, query);
		return budgetHeads;
	}

	@PostMapping(value = "/function-wise-view")
	public String functionwiseview(final Model model) {
		final List<FunctionBudgetHead> functionBudgetHead = functionBudgetHeadService.findAll();
		model.addAttribute("functionBudgetHead", functionBudgetHead);
		return BUDGETHEAD_FUNCTION_VIEW;
	}

}
