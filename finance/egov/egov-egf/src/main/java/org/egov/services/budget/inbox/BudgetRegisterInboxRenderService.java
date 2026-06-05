package org.egov.services.budget.inbox;

import org.egov.infra.workflow.inbox.InboxRenderService;
import org.egov.model.budget.BudgetRegister;
import org.egov.model.repository.BudgetRegisterWorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service("BudgetRegisterInboxRenderService")
public class BudgetRegisterInboxRenderService implements InboxRenderService<BudgetRegister> {

//    @Autowired
//    private BudgetRegisterWorkflowRepository budgetRegisterWorkflowRepository;


    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<BudgetRegister> getAssignedWorkflowItems(List<Long> owners) {
        if (owners == null || owners.isEmpty()) return Collections.emptyList();

        String jpql = "SELECT b FROM BudgetRegister b " +
                "WHERE b.state.ownerPosition IN :owners " +
                "AND b.state.status <> 2";

        return entityManager.createQuery(jpql, BudgetRegister.class)
                .setParameter("owners", owners)
                .getResultList();
    }

    @Override
    public List<BudgetRegister> getDraftWorkflowItems(List<Long> owners) {
        if (owners == null || owners.isEmpty()) return Collections.emptyList();

        String jpql = "SELECT b FROM BudgetRegister b " +
                "WHERE b.state IS NULL " +
                "AND b.createdBy IN :userIds";

        return entityManager.createQuery(jpql, BudgetRegister.class)
                .setParameter("userIds", owners)
                .getResultList();
    }
}
