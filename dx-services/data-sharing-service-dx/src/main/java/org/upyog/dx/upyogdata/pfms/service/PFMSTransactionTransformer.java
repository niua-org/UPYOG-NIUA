package org.upyog.dx.upyogdata.pfms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.upyog.dx.upyogdata.pfms.models.PFMSTransaction;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transforms a  PFMSTransaction by extracting all non-flat fields
 * and serializing them into a JSON string suitable for JSONB storage.
 *
 * Fields serialized into  additionalDetails:
 * voucherType, accountHeadCode, debitAmount, creditAmount, narrationOrDescription,
 * voucherStatus, ulbBankAccountNumber, ulbIFSCCode, instrumentReference,
 * modeOfTransaction, beneficiary details, challanNumber, account transfer fields,
 * adjustment fields, and periodCovered.
 */

@Component
@RequiredArgsConstructor
public class PFMSTransactionTransformer {

    private final ObjectMapper objectMapper;

    public String toAdditionalDetailsJson(PFMSTransaction t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("accountHeadCode", t.getAccountHeadCode());
        map.put("functionCode", t.getFunctionCode());
        map.put("schemeCode", t.getSchemeCode());
        map.put("debitAmount", t.getDebitAmount());
        map.put("creditAmount", t.getCreditAmount());
        map.put("narrationOrDescription", t.getNarrationOrDescription());
        map.put("voucherStatus", t.getVoucherStatus());
        map.put("ulbBankAccountNumber", t.getUlbBankAccountNumber());
        map.put("ulbIFSCCode", t.getUlbIFSCCode());
        map.put("instrumentReference", t.getInstrumentReference());
        map.put("modeOfTransaction", t.getModeOfTransaction());
        map.put("beneficiaryOrPayeeName", t.getBeneficiaryOrPayeeName());
        map.put("beneficiaryAccountNumber", t.getBeneficiaryAccountNumber());
        map.put("beneficiaryIFSCCode", t.getBeneficiaryIFSCCode());
        map.put("beneficiaryType", t.getBeneficiaryType());
        map.put("challanNumber", t.getChallanNumber());
        map.put("fromAccount", t.getFromAccount());
        map.put("toAccount", t.getToAccount());
        map.put("contraNature", t.getContraNature());
        map.put("transferInstructionReference", t.getTransferInstructionReference());
        map.put("referenceVoucherNumber", t.getReferenceVoucherNumber());
        map.put("adjustmentType", t.getAdjustmentType());
        map.put("relatedAssetOrLiabilityCode", t.getRelatedAssetOrLiabilityCode());
        map.put("debtorOrCreditorReference", t.getDebtorOrCreditorReference());
        map.put("basisOfAdjustment", t.getBasisOfAdjustment());
        map.put("periodCovered", t.getPeriodCovered());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize additionalDetails", e);
        }
    }
}

