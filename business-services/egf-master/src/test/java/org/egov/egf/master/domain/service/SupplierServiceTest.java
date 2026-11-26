package org.egov.egf.master.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.domain.exception.InvalidDataException;
import org.egov.common.domain.model.Pagination;
import org.egov.common.util.ApplicationThreadLocals;
import org.egov.egf.master.domain.model.Bank;
import org.egov.egf.master.domain.model.BankAccount;
import org.egov.egf.master.domain.model.Supplier;
import org.egov.egf.master.domain.model.SupplierSearch;
import org.egov.egf.master.domain.repository.BankAccountRepository;
import org.egov.egf.master.domain.repository.BankRepository;
import org.egov.egf.master.domain.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @InjectMocks
    private SupplierService supplierService;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private BankRepository bankRepository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "supplier");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        Supplier supplier = sample(null, null);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);
        assertThat(supplierService.save(supplier)).isSameAs(supplier);
    }

    @Test
    void search_returnsPagination() {
        SupplierSearch search = new SupplierSearch();
        search.setTenantId("default");
        Pagination<Supplier> pagination = new Pagination<>();
        pagination.setPagedData(List.of(sample(null, null)));
        when(supplierRepository.search(any(SupplierSearch.class))).thenReturn(pagination);
        assertThat(supplierService.search(search, errors)).isSameAs(pagination);
    }

    @Test
    void fetchRelated_resolvesBankAndAccount() {
        Bank bank = Bank.builder().id("bank").build();
        BankAccount account = BankAccount.builder().id("acct").build();
        Supplier supplier = sample(bank, account);
        when(bankAccountRepository.findById(account)).thenReturn(account);
        when(bankRepository.findById(bank)).thenReturn(bank);

        List<Supplier> result = supplierService.fetchRelated(new ArrayList<>(List.of(supplier)));

        assertThat(result.get(0).getBank()).isSameAs(bank);
        assertThat(result.get(0).getBankAccount()).isSameAs(account);
    }

    @Test
    void fetchRelated_throwsWhenBankInvalid() {
        Bank bank = Bank.builder().id("missing").build();
        Supplier supplier = sample(bank, null);
        when(bankRepository.findById(bank)).thenReturn(null);

        assertThatThrownBy(() -> supplierService.fetchRelated(new ArrayList<>(List.of(supplier))))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void add_assignsSequenceWhenValid() {
        Supplier supplier = sample(null, null);
        when(supplierRepository.uniqueCheck(eq("code"), any(Supplier.class))).thenReturn(true);
        when(supplierRepository.getNextSequence()).thenReturn("sup-1");

        List<Supplier> result = supplierService.add(new ArrayList<>(List.of(supplier)), errors);

        assertThat(result.get(0).getId()).isEqualTo("sup-1");
    }

    private Supplier sample(Bank bank, BankAccount account) {
        Supplier supplier = new Supplier();
        supplier.setId("1");
        supplier.setCode("code");
        supplier.setName("supplier");
        supplier.setBank(bank);
        supplier.setBankAccount(account);
        return supplier;
    }
}
