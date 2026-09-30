package org.egov.dx.upyogdata.pfms.service;

import org.egov.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;

public interface PFMSService {

    Object createTransaction(PFMSCreateTransactionRequest request);
}
