package com.tarento.analytics.utils;

import com.tarento.analytics.constant.Constants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseCodeTest {

    @Test
    void enumValuesExposeConfiguredCodesAndMessages() {
        assertThat(ResponseCode.Success.getErrorCode()).isEqualTo(Constants.SUCCESS_ID);
        assertThat(ResponseCode.Success.getErrorMessage()).isEqualTo(Constants.SUCCESS);
        assertThat(ResponseCode.getResponseMessage(Constants.FAILURE_ID)).isEqualTo(Constants.PROCESS_FAIL);
    }
}
