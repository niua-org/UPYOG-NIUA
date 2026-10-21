package org.egov.egf.instrument.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.domain.exception.CustomBindException;
import org.egov.common.domain.exception.InvalidDataException;
import org.egov.common.domain.model.Pagination;
import org.egov.egf.instrument.TestConfiguration;
import org.egov.egf.instrument.domain.model.InstrumentType;
import org.egov.egf.instrument.domain.model.InstrumentTypeSearch;
import org.egov.egf.instrument.domain.repository.InstrumentTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;

@Import(TestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class InstrumentTypeServiceTest {

    private InstrumentTypeService instrumentTypeService;

    @Mock
    private SmartValidator validator;

    @Mock
    private InstrumentTypeRepository instrumentTypeRepository;

    private RequestInfo requestInfo = new RequestInfo();

    private BindingResult errors = new BeanPropertyBindingResult(null, null);

    @BeforeEach
    public void setup() {
        instrumentTypeService = new InstrumentTypeService(validator, instrumentTypeRepository);
    }

    @Test
    public final void test_save_with_out_kafka() {

        List<InstrumentType> expextedResult = getInstrumentTypes();

        when(instrumentTypeRepository.uniqueCheck(any(String.class), any(InstrumentType.class))).thenReturn(true);
        when(instrumentTypeRepository.save(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        List<InstrumentType> actualResult = instrumentTypeService.create(expextedResult, errors, requestInfo);

        assertEquals(expextedResult, actualResult);

    }

    @Test
    public final void test_save_with_out_kafka_unique_false() {

        List<InstrumentType> expextedResult = getInstrumentTypes();

        when(instrumentTypeRepository.uniqueCheck(any(String.class), any(InstrumentType.class))).thenReturn(false);
        when(instrumentTypeRepository.save(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        assertThrows(CustomBindException.class, () -> instrumentTypeService.create(expextedResult, errors, requestInfo));


    }

    @Test
    public final void test_save_with_out_kafka_and_with_null_req() {

        List<InstrumentType> expextedResult = getInstrumentTypes();

        when(instrumentTypeRepository.save(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentTypeService.create(null, errors, requestInfo));


    }

    @Test
    public final void test_update_with_out_kafka() {

        List<InstrumentType> expextedResult = getInstrumentTypes();

        when(instrumentTypeRepository.uniqueCheck(any(String.class), any(InstrumentType.class))).thenReturn(true);
        when(instrumentTypeRepository.update(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        List<InstrumentType> actualResult = instrumentTypeService.update(expextedResult, errors, requestInfo);

        assertEquals(expextedResult, actualResult);

    }

    @Test
    public final void test_update_with_out_kafka_null_id() {

        List<InstrumentType> expextedResult = getInstrumentTypes();
        expextedResult.get(0).setId(null);

        when(instrumentTypeRepository.uniqueCheck(any(String.class), any(InstrumentType.class))).thenReturn(true);
        when(instrumentTypeRepository.update(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentTypeService.update(expextedResult, errors, requestInfo));


    }

    @Test
    public final void test_update_with_out_kafka_unique_false() {

        List<InstrumentType> expextedResult = getInstrumentTypes();

        when(instrumentTypeRepository.uniqueCheck(any(String.class), any(InstrumentType.class))).thenReturn(false);
        when(instrumentTypeRepository.update(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        assertThrows(CustomBindException.class, () -> instrumentTypeService.update(expextedResult, errors, requestInfo));


    }

    @Test
    public final void test_delete_with_out_kafka() {

        List<InstrumentType> expextedResult = getInstrumentTypes();

        when(instrumentTypeRepository.delete(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        List<InstrumentType> actualResult = instrumentTypeService.delete(expextedResult, errors, requestInfo);

        assertEquals(expextedResult, actualResult);

    }

    @Test
    public final void test_delete_with_out_kafka_nullrequest() {

        List<InstrumentType> expextedResult = getInstrumentTypes();

        when(instrumentTypeRepository.delete(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentTypeService.delete(null, errors, requestInfo));


    }

    @Test
    public final void test_delete_with_out_kafka_null_id() {

        List<InstrumentType> expextedResult = getInstrumentTypes();
        expextedResult.get(0).setId(null);

        when(instrumentTypeRepository.delete(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentTypeService.delete(expextedResult, errors, requestInfo));


    }

    @Test
    public final void test_update_with_out_kafka_and_with_null_req() {

        List<InstrumentType> expextedResult = getInstrumentTypes();

        when(instrumentTypeRepository.update(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentTypeService.update(null, errors, requestInfo));


    }

    @Test
    public final void test_search() {

        List<InstrumentType> instrumentTypes = getInstrumentTypes();
        InstrumentTypeSearch instrumentTypeSearch = new InstrumentTypeSearch();
        Pagination<InstrumentType> expextedResult = new Pagination<>();

        expextedResult.setPagedData(instrumentTypes);

        when(instrumentTypeRepository.search(instrumentTypeSearch)).thenReturn(expextedResult);

        Pagination<InstrumentType> actualResult = instrumentTypeService.search(instrumentTypeSearch);

        assertEquals(expextedResult, actualResult);
    }

    @Test
    public final void test_save() {

        InstrumentType expextedResult = getInstrumentTypes().get(0);

        when(instrumentTypeRepository.save(any(InstrumentType.class))).thenReturn(expextedResult);

        InstrumentType actualResult = instrumentTypeService.save(expextedResult);

        assertEquals(expextedResult, actualResult);
    }

    @Test
    public final void test_update() {

        InstrumentType expextedResult = getInstrumentTypes().get(0);

        when(instrumentTypeRepository.update(any(InstrumentType.class))).thenReturn(expextedResult);

        InstrumentType actualResult = instrumentTypeService.update(expextedResult);

        assertEquals(expextedResult, actualResult);
    }

    @Test
    public final void test_delete() {

        InstrumentType expextedResult = getInstrumentTypes().get(0);

        when(instrumentTypeRepository.delete(any(InstrumentType.class))).thenReturn(expextedResult);

        InstrumentType actualResult = instrumentTypeService.delete(expextedResult);

        assertEquals(expextedResult, actualResult);
    }

    private List<InstrumentType> getInstrumentTypes() {
        List<InstrumentType> instrumentTypes = new ArrayList<InstrumentType>();
        InstrumentType instrumentType = InstrumentType.builder().id("1").build();
        instrumentType.setTenantId("default");
        instrumentTypes.add(instrumentType);
        return instrumentTypes;
    }

}
