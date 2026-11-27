package com.ingestpipeline.service;

import com.ingestpipeline.model.DigressionPoint;
import com.ingestpipeline.model.IncomingData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DigressServiceImplTest {

    private final DigressServiceImpl digressService = new DigressServiceImpl();

    @Test
    void digressData_currentImplementationReturnsNull() {
        IncomingData incomingData = new IncomingData();
        incomingData.setDataContext("collection");

        assertThat(digressService.digressData(incomingData, new DigressionPoint())).isNull();
    }
}
