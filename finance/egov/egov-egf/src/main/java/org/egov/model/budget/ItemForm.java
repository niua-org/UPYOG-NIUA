package org.egov.model.budget;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Wrapper Form DTO for the whole table

@Setter
@Getter
public class ItemForm {
    private List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }
    public void setItems(List<Item> items) {
        this.items = items;
    }
}
