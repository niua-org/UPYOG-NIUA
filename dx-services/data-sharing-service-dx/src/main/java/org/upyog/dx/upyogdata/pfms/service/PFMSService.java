package org.upyog.dx.upyogdata.pfms.service;

import org.upyog.dx.upyogdata.pfms.models.PFMSCreateTransactionRequest;

public interface PFMSService {

    Object createTransaction(PFMSCreateTransactionRequest request);
}
