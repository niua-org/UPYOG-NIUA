package com.tarento.analytics.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;
import com.tarento.analytics.dto.Plot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SubtractiveComputedFieldTest {

    @InjectMocks
    private SubtractiveComputedField computedField;

    @Test
    void add_subtractsSecondFieldFromFirst() {
        Data data = new Data("Diff", 0.0, "number");
        data.setPlots(new ArrayList<>(List.of(
                new Plot("base", 30.0, "number"),
                new Plot("total", 100.0, "number")
        )));
        computedField.set(new AggregateRequestDto(), null);

        computedField.add(data, List.of("base", "total"), "Balance", new ObjectMapper().createObjectNode());

        assertThat(data.getPlots()).anyMatch(p -> "Balance".equals(p.getName()) && p.getValue() == 70.0);
    }
}
