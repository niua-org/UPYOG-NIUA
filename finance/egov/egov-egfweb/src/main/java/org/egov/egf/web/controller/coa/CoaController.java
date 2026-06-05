package org.egov.egf.web.controller.coa;


import org.egov.infra.microservice.utils.MicroserviceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/coa")
public class CoaController {

    @Autowired
    private MicroserviceUtils microserviceUtils;

    @PostMapping(value = "/chartofaccounts")
    public String showCoa(final Model model) {
        model.addAttribute("tutorial", microserviceUtils.getTutorial("master.chartOfAccounts"));
        return "coa";
    }

    @PostMapping(value = "/eGov_COA.jsp")
    public String showCoaJsp(final Model model) {
        model.addAttribute("tutorial", microserviceUtils.getTutorial("master.chartOfAccounts"));
        return "coa";
    }

    @PostMapping(value = "/sdashboard")
    public String sdashboard(final Model model) {
        return "sdashboard";
    }

}
