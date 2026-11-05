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
import org.egov.egf.instrument.domain.model.InstrumentAccountCode;
import org.egov.egf.instrument.domain.model.InstrumentAccountCodeSearch;
import org.egov.egf.instrument.domain.model.InstrumentType;
import org.egov.egf.instrument.domain.repository.InstrumentAccountCodeRepository;
import org.egov.egf.instrument.domain.repository.InstrumentTypeRepository;
import org.egov.egf.master.web.contract.ChartOfAccountContract;
import org.egov.egf.master.web.repository.ChartOfAccountContractRepository;
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
public class InstrumentAccountCodeServiceTest {

    private InstrumentAccountCodeService instrumentAccountCodeService;

    @Mock
    private InstrumentAccountCodeRepository instrumentAccountCodeRepository;

    @Mock
    private SmartValidator validator;

    @Mock
    private ChartOfAccountContractRepository chartOfAccountContractRepository;

    @Mock
    private InstrumentTypeRepository instrumentTypeRepository;

    private BindingResult errors = new BeanPropertyBindingResult(null, null);

    private RequestInfo requestInfo = new RequestInfo();

    @BeforeEach
    public void setup() {
        instrumentAccountCodeService = new InstrumentAccountCodeService(validator, instrumentAccountCodeRepository,
                chartOfAccountContractRepository, instrumentTypeRepository);
    }

    @Test
    public final void test_create() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();

        when(instrumentAccountCodeRepository.save(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);
        when(instrumentAccountCodeRepository.uniqueCheck(any(String.class), any(InstrumentAccountCode.class))).thenReturn(true);
        List<InstrumentAccountCode> actualResult = instrumentAccountCodeService.create(expextedResult, errors,
                requestInfo);

        assertEquals(expextedResult, actualResult);

    }

    @Test
    public final void test_create_unique_false() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();

        when(instrumentAccountCodeRepository.save(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);
        when(instrumentAccountCodeRepository.uniqueCheck(any(String.class), any(InstrumentAccountCode.class))).thenReturn(false);
        assertThrows(CustomBindException.class,
                () -> instrumentAccountCodeService.create(expextedResult, errors, requestInfo));


    }

    @Test
    public final void test_create_and_with_null_req() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();

        when(instrumentAccountCodeRepository.save(any(List.class), any(RequestInfo.class))).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentAccountCodeService.create(null, errors, requestInfo));


    }

    @Test
    public final void test_update_() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();

        when(instrumentAccountCodeRepository.update(any(List.class), any(RequestInfo.class)))
                .thenReturn(expextedResult);
        when(instrumentAccountCodeRepository.uniqueCheck(any(String.class), any(InstrumentAccountCode.class))).thenReturn(true);
        List<InstrumentAccountCode> actualResult = instrumentAccountCodeService.update(expextedResult, errors,
                requestInfo);

        assertEquals(expextedResult, actualResult);

    }

    @Test
    public final void test_update_null_id() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();
        expextedResult.get(0).setId(null);

        when(instrumentAccountCodeRepository.update(any(List.class), any(RequestInfo.class)))
                .thenReturn(expextedResult);
        when(instrumentAccountCodeRepository.uniqueCheck(any(String.class), any(InstrumentAccountCode.class))).thenReturn(true);
        assertThrows(InvalidDataException.class,
                () -> instrumentAccountCodeService.update(expextedResult, errors, requestInfo));


    }

    @Test
    public final void test_update_unique_false() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();

        when(instrumentAccountCodeRepository.update(any(List.class), any(RequestInfo.class)))
                .thenReturn(expextedResult);
        when(instrumentAccountCodeRepository.uniqueCheck(any(String.class), any(InstrumentAccountCode.class))).thenReturn(false);
        assertThrows(CustomBindException.class,
                () -> instrumentAccountCodeService.update(expextedResult, errors, requestInfo));


    }

    @Test
    public final void test_delete_() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();

        when(instrumentAccountCodeRepository.delete(any(List.class), any(RequestInfo.class)))
                .thenReturn(expextedResult);

        List<InstrumentAccountCode> actualResult = instrumentAccountCodeService.delete(expextedResult, errors,
                requestInfo);

        assertEquals(expextedResult, actualResult);

    }

    @Test
    public final void test_delete_null_req() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();

        when(instrumentAccountCodeRepository.delete(any(List.class), any(RequestInfo.class)))
                .thenReturn(expextedResult);

        assertThrows(InvalidDataException.class,
                () -> instrumentAccountCodeService.delete(null, errors, requestInfo));


    }

    @Test
    public final void test_delete_null_id() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();
        expextedResult.get(0).setId(null);

        when(instrumentAccountCodeRepository.delete(any(List.class), any(RequestInfo.class)))
                .thenReturn(expextedResult);

        assertThrows(InvalidDataException.class,
                () -> instrumentAccountCodeService.delete(expextedResult, errors, requestInfo));


    }

    @Test
    public final void test_update_with_null_req() {

        List<InstrumentAccountCode> expextedResult = getInstrumentAccountCodes();

        when(instrumentAccountCodeRepository.update(any(List.class), any(RequestInfo.class)))
                .thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentAccountCodeService.update(null, errors, requestInfo));


    }

    @Test
    public final void test_search() {

        List<InstrumentAccountCode> instrumentAccountCodes = getInstrumentAccountCodes();
        InstrumentAccountCodeSearch instrumentAccountCodeSearch = new InstrumentAccountCodeSearch();
        Pagination<InstrumentAccountCode> expextedResult = new Pagination<>();

        expextedResult.setPagedData(instrumentAccountCodes);

        when(instrumentAccountCodeRepository.search(instrumentAccountCodeSearch)).thenReturn(expextedResult);

        Pagination<InstrumentAccountCode> actualResult = instrumentAccountCodeService
                .search(instrumentAccountCodeSearch);

        assertEquals(expextedResult, actualResult);
    }

    @Test
    public final void test_save() {

        InstrumentAccountCode expextedResult = getInstrumentAccountCodes().get(0);

        when(instrumentAccountCodeRepository.save(any(InstrumentAccountCode.class))).thenReturn(expextedResult);

        InstrumentAccountCode actualResult = instrumentAccountCodeService.save(expextedResult);

        assertEquals(expextedResult, actualResult);
    }

    @Test
    public final void test_update() {

        InstrumentAccountCode expextedResult = getInstrumentAccountCodes().get(0);

        when(instrumentAccountCodeRepository.update(any(InstrumentAccountCode.class))).thenReturn(expextedResult);

        InstrumentAccountCode actualResult = instrumentAccountCodeService.update(expextedResult);

        assertEquals(expextedResult, actualResult);
    }

    @Test
    public final void test_delete() {

        InstrumentAccountCode expextedResult = getInstrumentAccountCodes().get(0);

        when(instrumentAccountCodeRepository.delete(any(InstrumentAccountCode.class))).thenReturn(expextedResult);

        InstrumentAccountCode actualResult = instrumentAccountCodeService.delete(expextedResult);

        assertEquals(expextedResult, actualResult);
    }

    @Test
    public final void test_fetch_instrumenttype() {

        List<InstrumentAccountCode> instrumentAccountCodes = getInstrumentAccountCodes();

        InstrumentType expextedResult = InstrumentType.builder().name("name").description("description").active(true)
                .id("1").build();

        instrumentAccountCodes.get(0).setInstrumentType(expextedResult);

        when(instrumentTypeRepository.findById(any(InstrumentType.class))).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentAccountCodeService.fetchRelated(instrumentAccountCodes,
                new RequestInfo()));
    }

    @Test
    public final void test_fetch_accountcode() {

        List<InstrumentAccountCode> instrumentAccountCodes = getInstrumentAccountCodes();

        ChartOfAccountContract expextedResult = ChartOfAccountContract.builder().glcode("glcode").id("1").build();

        instrumentAccountCodes.get(0).setAccountCode(expextedResult);

        when(chartOfAccountContractRepository.findByGlcode(any(ChartOfAccountContract.class), any()))
                .thenReturn(expextedResult);

        List<InstrumentAccountCode> actualResult = instrumentAccountCodeService.fetchRelated(instrumentAccountCodes,
                new RequestInfo());

        assertEquals(expextedResult, actualResult.get(0).getAccountCode());
    }

    @Test
    public final void test_fetch_instrumenttype_null() {

        List<InstrumentAccountCode> instrumentAccountCodes = getInstrumentAccountCodes();

        InstrumentType expextedResult = InstrumentType.builder().name("name").description("description").active(true)
                .id("1").build();

        instrumentAccountCodes.get(0).setInstrumentType(expextedResult);

        when(instrumentTypeRepository.findById(null)).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentAccountCodeService.fetchRelated(instrumentAccountCodes,
                new RequestInfo()));
    }

    @Test
    public final void test_fetch_accountcode_null() {

        List<InstrumentAccountCode> instrumentAccountCodes = getInstrumentAccountCodes();

        ChartOfAccountContract expextedResult = ChartOfAccountContract.builder().glcode("glcode").id("1").build();

        instrumentAccountCodes.get(0).setAccountCode(expextedResult);

        when(chartOfAccountContractRepository.findByGlcode(null, new RequestInfo())).thenReturn(expextedResult);

        assertThrows(InvalidDataException.class, () -> instrumentAccountCodeService.fetchRelated(instrumentAccountCodes,
                new RequestInfo()));
    }

    private List<InstrumentAccountCode> getInstrumentAccountCodes() {
        List<InstrumentAccountCode> instrumentAccountCodes = new ArrayList<InstrumentAccountCode>();
        InstrumentAccountCode instrumentAccountCode = InstrumentAccountCode.builder().id("1").build();
        instrumentAccountCode.setTenantId("default");
        instrumentAccountCodes.add(instrumentAccountCode);
        return instrumentAccountCodes;
    }

}
