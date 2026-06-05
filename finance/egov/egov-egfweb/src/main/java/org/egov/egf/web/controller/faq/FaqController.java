package org.egov.egf.web.controller.faq;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/faq")
public class FaqController {


    @PostMapping(value = "/viewFaqs")
    public String viewFaqs(final Model model) {

        return "view_faqs";

    }


}
