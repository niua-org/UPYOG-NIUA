package org.egov.egf.web.controller.scheme;


import org.egov.commons.Scheme;
import org.egov.commons.dao.SchemeHibernateDAO;
import org.egov.services.masters.SchemeService;
import org.hibernate.validator.constraints.SafeHtml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/*
 * REST controller providing AJAX endpoint for scheme search and autocomplete functionality.
 * Used by budget module forms to enable scheme selection via name or code search.
 * Returns JSON list of matching schemes for dropdown/autocomplete components.
 */

@Controller()
@RequestMapping("/scheme")
public class SchemeRestController {


    @Autowired
    private SchemeHibernateDAO schemeHibernateDAO;



    @GetMapping(value = "/ajaxSchemes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Scheme> findSchemesByNameOrCode(@RequestParam @SafeHtml final String query) {
        final List<Scheme> schemes = schemeHibernateDAO.getSchemeByNameOrCode(query);
        return schemes;
    }



}
