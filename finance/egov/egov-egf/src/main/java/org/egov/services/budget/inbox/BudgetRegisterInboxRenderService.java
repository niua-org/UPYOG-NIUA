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


/*
 * Inbox rendering service for BudgetRegister workflow items.
 *
 * This service implements the InboxRenderService interface to integrate BudgetRegister entities
 * with the eGov workflow inbox system. It provides functionality to fetch and display budget
 * register items in the user's workflow inbox.
 *
 * Key Features:
 * - Retrieves budget registers assigned to specific positions/users for workflow action
 * - Fetches draft budget registers that have not yet been submitted to workflow
 * - Filters out closed/completed workflow items from the inbox
 * - Handles empty owner lists gracefully by returning empty results
 *
 * Workflow Integration:
 * - Assigned items: Budget registers with active workflow states assigned to user positions
 * - Draft items: Budget registers created by users but not yet submitted (state is NULL)
 *
 * Used by the workflow inbox UI to populate the user's pending tasks and draft items.
 */
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
