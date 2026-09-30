package org.egov.dx.upyogdata.pfms.service;

public interface PFMSForwardingService {
    void forwardInitiatedTransactions();
    void retryFailedTransactions();
}
