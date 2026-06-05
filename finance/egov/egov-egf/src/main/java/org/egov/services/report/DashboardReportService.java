package org.egov.services.report;

import org.apache.log4j.Logger;
import org.egov.commons.dao.FinancialYearHibernateDAO;
import org.egov.commons.repository.FundRepository;
import org.egov.egf.commons.bankaccount.repository.BankAccountRepository;
import org.egov.egf.expensebill.repository.ExpenseBillRepository;
import org.egov.egf.masters.repository.ContractorRepository;
import org.egov.egf.masters.repository.PurchaseOrderRepository;
import org.egov.egf.masters.repository.SupplierRepository;
import org.egov.egf.masters.repository.WorkOrderRepository;
import org.egov.egf.model.DashboardReport;
import org.egov.egf.voucher.repository.JournalVoucherRepository;
import org.egov.infstr.services.PersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class DashboardReportService {


    @Autowired
    @Qualifier("persistenceService")
    private PersistenceService persistenceService;

    @Autowired
    private ContractorRepository contractorRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ExpenseBillRepository expenseBillRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private JournalVoucherRepository journalVoucherRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private FinancialYearHibernateDAO financialYearHibernateDAO;


    private static final Logger LOGGER = Logger.getLogger(DashboardReportService.class);


    public Long getContractorsCount() {
        return contractorRepository.count();
    }

    public Long getSuppliersCount() {
       return supplierRepository.count();
    }


    public Long getTotalBillsCreated(String type) {
        Long count = 0L;
       if (type == null || type.isEmpty()) {
           count =  expenseBillRepository.count();
        } else {
           try {

               final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

               Date startDate =  formatter.parse(financialYearHibernateDAO.getCurrYearStartDate());
               Date endDate = formatter.parse(financialYearHibernateDAO.getCurrFinancialYearEndDate());


               count = expenseBillRepository.countByExpendituretypeAndBilldateBetween(type, startDate, endDate);

           } catch (Exception e) {
               e.printStackTrace();
               count = 0L;
           }

        }
        return  count;
    }


    public Long getTotalBillsCreated(String type, Date startDate, Date endDate) {
        Long count = 0L;
        if (type == null || type.isEmpty()) {
            count =  expenseBillRepository.count();
        } else {
            try {

                count = expenseBillRepository.countByExpendituretypeAndBilldateBetween(type, startDate, endDate);

            } catch (Exception e) {
                e.printStackTrace();
                count = 0L;
            }

        }
        return  count;
    }




    public Long getTotalWorkOrdersCount() {
        return workOrderRepository.count();
    }


    public Long getTotalPurchaseOrderCount() {
        return purchaseOrderRepository.count();
    }

    public Long getTotalJournalVoucherCount(Date startDate, Date endDate) {
        return journalVoucherRepository.countByVoucherDateBetween(startDate, endDate);
    }

    public Long getTotalsFundsCount() {
        return fundRepository.count();
    }


    public Long getTotalBankAccountCount() {
        return bankAccountRepository.count();
    }

    public Long getTotalContractorCount() {
        return contractorRepository.count();
    }

    public Long getTotalSupplierCount() {
        return supplierRepository.count();
    }


    public Long getTotalPaymentCount(Date startDate, Date endDate) {
        return journalVoucherRepository.getPaymentsCount(startDate, endDate);
    }



    @Transactional(readOnly = true)
    public void buildDashboardReport(DashboardReport dashboardReport, Date startDate, Date endDate) {

        try {

        if (startDate == null || endDate == null) {

            LOGGER.info("dates are null!");

            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

            startDate =  formatter.parse(financialYearHibernateDAO.getCurrYearStartDate());
            endDate = formatter.parse(financialYearHibernateDAO.getCurrFinancialYearEndDate());

            try {
                startDate =  formatter.parse(financialYearHibernateDAO.getCurrYearStartDate());
                endDate = formatter.parse(financialYearHibernateDAO.getCurrFinancialYearEndDate());
            } catch (Exception e) {
                e.printStackTrace();

            }


        }

        SimpleDateFormat formatterToShow = new SimpleDateFormat("dd/MM/yyyy");

        dashboardReport.setStartDate(formatterToShow.format(startDate));
        dashboardReport.setEndDate(formatterToShow.format(endDate));



        dashboardReport.setTotalExpenseBills(getTotalBillsCreated("Expense", startDate, endDate));
        dashboardReport.setTotalContractorBills(getTotalBillsCreated("Works", startDate, endDate));
        dashboardReport.setTotalSupplierBills(getTotalBillsCreated("Purchase", startDate, endDate));
        dashboardReport.setTotalWorkOrders(getTotalWorkOrdersCount());
        dashboardReport.setTotalPurchaseOrders(getTotalPurchaseOrderCount());
        dashboardReport.setTotalJournalVouchers(getTotalJournalVoucherCount(startDate, endDate));
        dashboardReport.setTotalFunds(getTotalsFundsCount());
        dashboardReport.setTotalBankAccounts(getTotalBankAccountCount());
        dashboardReport.setTotalContractors(getContractorsCount());
        dashboardReport.setTotalSuppliers(getTotalSupplierCount());
        dashboardReport.setTotalBillsPayment(getTotalPaymentCount(startDate, endDate));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }





}
