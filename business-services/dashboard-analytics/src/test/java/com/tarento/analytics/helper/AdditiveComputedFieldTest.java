package com.tarento.analytics.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;
import com.tarento.analytics.dto.Plot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdditiveComputedFieldTest {

    @Mock
    private ComputeHelperFactory computeHelperFactory;

    @InjectMocks
    private AdditiveComputedField computedField;

    @Test
    void add_sumsSelectedPlots() {
        Data data = new Data("Total", 0.0, "amount");
        data.setPlots(new java.util.ArrayList<>(List.of(
                new Plot("A", 10.0, "amount"),
                new Plot("B", 5.0, "amount")
        )));
        AggregateRequestDto requestDto = new AggregateRequestDto();
        computedField.set(requestDto, null);

        computedField.add(data, List.of("A", "B"), "Sum", new ObjectMapper().createObjectNode());

        assertThat(data.getPlots()).anyMatch(p -> "Sum".equals(p.getName()) && p.getValue() == 15.0);
    }

    @Test
    void add_withPostAggregation_usesComputeHelper() {
        ComputeHelper helper = org.mockito.Mockito.mock(ComputeHelper.class);
        when(helper.compute(any(), eq(4.0))).thenReturn(8.0);
        when(computeHelperFactory.getInstance(eq("double"))).thenReturn(helper);

        Data data = new Data("Total", 0.0, "amount");
        data.setPlots(new java.util.ArrayList<>(List.of(new Plot("A", 4.0, "amount"))));
        AggregateRequestDto requestDto = new AggregateRequestDto();
        computedField.set(requestDto, "double");

        computedField.add(data, List.of("A"), "Doubled", new ObjectMapper().createObjectNode());

        assertThat(data.getPlots()).anyMatch(p -> "Doubled".equals(p.getName()) && p.getValue() == 8.0);
    }
}
