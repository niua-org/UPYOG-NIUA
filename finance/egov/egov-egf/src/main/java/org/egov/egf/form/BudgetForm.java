package org.egov.egf.form;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.egov.model.budget.BudgetItem;

@Getter
@Setter
public class BudgetForm {

    private Long functionid;             // Selected Function ID

    private BudgetItem opening = new BudgetItem();          // Opening Balance row

    private BudgetItem closing = new BudgetItem();          // Opening Balance row

    private List<BudgetItem> items;      // Revenue/Capital rows (multiple)

    private Long financialYear;

    private Long currentFinancialYear;

    private Long stateBudgetCode;

    public BudgetForm() {
        // Ensure the list is never null
        this.items = new ArrayList<>();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Function Id: ").append(functionid);
//        stringBuilder.append("\n Opening Balance: ").append(opening.toString());
//        stringBuilder.append("\n Closing Balance: ").append(closing.toString());
        stringBuilder.append("\n\n");

//        items.forEach(budgetItem -> {
//            stringBuilder.append("\n ").append(budgetItem.toString());
//        });

        return stringBuilder.toString();

    }

}
