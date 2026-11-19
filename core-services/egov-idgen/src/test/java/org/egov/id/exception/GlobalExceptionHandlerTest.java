package org.egov.id.exception;

import org.egov.id.config.PropertiesManager;
import org.egov.id.model.Error;
import org.egov.id.model.ErrorRes;
import org.egov.id.model.IDSeqNotFoundException;
import org.egov.id.model.IDSeqOverflowException;
import org.egov.id.model.InvalidIDFormatException;
import org.egov.id.model.RequestInfo;
import org.egov.id.model.ResponseInfo;
import org.egov.id.model.ResponseStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private PropertiesManager propertiesManager;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void processValidationError_returnsFieldErrors() {
        when(propertiesManager.getInvalidInput()).thenReturn("Invalid input");
        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("idRequest", "tenantId", "required")));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ErrorRes result = globalExceptionHandler.processValidationError(ex);

        assertThat(result.getResponseInfo().getStatus()).isEqualTo(ResponseStatusEnum.FAILED);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFileds()).containsEntry("tenantId", "required");
    }

    @Test
    void unknownException_genericException_returnsBadRequestWithMessage() {
        ErrorRes result = globalExceptionHandler.unknownException(
                new Exception("unexpected"),
                new ServletWebRequest(new MockHttpServletRequest()));

        assertSingleError(result, "unexpected");
        assertThat(result.getResponseInfo().getApiId()).isNull();
    }

    @Test
    void unknownException_invalidIdFormat_populatesRequestInfo() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setApiId("idgen");
        requestInfo.setVer("1.0");
        requestInfo.setMsgId("msg-1");
        InvalidIDFormatException ex = new InvalidIDFormatException("bad format", requestInfo);

        ErrorRes result = globalExceptionHandler.unknownException(ex, new ServletWebRequest(new MockHttpServletRequest()));

        ResponseInfo responseInfo = result.getResponseInfo();
        assertThat(responseInfo.getApiId()).isEqualTo("idgen");
        assertThat(responseInfo.getVer()).isEqualTo("1.0");
        assertThat(responseInfo.getMsgId()).isEqualTo("msg-1");
        assertThat(responseInfo.getTs()).isNotNull();
        assertSingleError(result, "bad format");
    }

    @Test
    void unknownException_idSeqOverflow_populatesRequestInfo() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setApiId("idgen");
        IDSeqOverflowException ex = new IDSeqOverflowException("overflow", requestInfo);

        ErrorRes result = globalExceptionHandler.unknownException(ex, new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(result.getResponseInfo().getApiId()).isEqualTo("idgen");
        assertSingleError(result, "overflow");
    }

    @Test
    void unknownException_idSeqNotFound_populatesRequestInfo() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setApiId("idgen");
        IDSeqNotFoundException ex = new IDSeqNotFoundException("missing seq", requestInfo);

        ErrorRes result = globalExceptionHandler.unknownException(ex, new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(result.getResponseInfo().getApiId()).isEqualTo("idgen");
        assertSingleError(result, "missing seq");
    }

    private static void assertSingleError(ErrorRes result, String message) {
        List<Error> errors = result.getErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getMessage()).isEqualTo(message);
        assertThat(errors.get(0).getCode()).isEqualTo("400 BAD_REQUEST");
        assertThat(result.getResponseInfo().getStatus()).isEqualTo(ResponseStatusEnum.FAILED);
    }
}
