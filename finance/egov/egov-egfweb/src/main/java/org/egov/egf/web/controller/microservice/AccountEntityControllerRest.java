package org.egov.egf.web.controller.microservice;

import org.egov.commons.Accountdetailtype;
import org.egov.commons.service.AccountEntityService;
import org.egov.commons.service.AccountdetailtypeService;
import org.egov.egf.contract.model.AccountEntityRequest;
import org.egov.egf.contract.model.AccountEntityResponse;
import org.egov.egf.web.controller.AccountdetailtypeController;
import org.egov.infra.exception.ApplicationValidationException;
import org.egov.infra.microservice.models.ResponseInfo;
import org.egov.infra.validation.exception.ValidationException;
import org.egov.masters.model.AccountEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AccountEntityControllerRest {

    private final Logger LOGGER = LoggerFactory.getLogger(AccountEntityControllerRest.class);


    @Autowired
    private AccountEntityService accountEntityService;

    @Autowired
    private AccountdetailtypeService accountdetailtypeService;


    @PostMapping(value = "/rest/accountentity/create")
    @ResponseBody
    public AccountEntityResponse create(@RequestBody AccountEntityRequest accountEntityRequest) throws Exception {

        try {

            Accountdetailtype accountdetailtype = accountdetailtypeService.findOne(accountEntityRequest.getAccountDetailType());

            if (accountdetailtype == null) {
                throw new Exception("The Accountdetailtype is invalid.");
            }

            AccountEntity accountEntity = getAccountEntity(accountEntityRequest, accountdetailtype);

            accountEntityService.create(accountEntity);

            AccountEntityResponse accountEntityResponse = new AccountEntityResponse();

            ResponseInfo responseInfo = new ResponseInfo();
            responseInfo.setStatus("200");

            accountEntityResponse.setResponseInfo(responseInfo);
            accountEntityResponse.setAccountEntity(accountEntity);


            return accountEntityResponse;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationValidationException(e.getMessage());
        }

    }

    private static AccountEntity getAccountEntity(AccountEntityRequest accountEntityRequest, Accountdetailtype accountdetailtype) {
        AccountEntity accountEntity = new AccountEntity();

        accountEntity.setAccountdetailtype(accountdetailtype);
        accountEntity.setName(accountEntityRequest.getName());
        accountEntity.setCode(accountEntityRequest.getCode());
        accountEntity.setNarration(accountEntityRequest.getNarration());
        accountEntity.setIsactive(accountEntityRequest.getIsActive());
        return accountEntity;
    }

}
