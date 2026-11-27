package org.egov.dx.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.egov.dx.web.models.Transaction;
import org.egov.dx.web.models.TransactionCriteria;
import org.egov.dx.web.models.TransactionQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class TransactionRepository {
	
    private static final TransactionRowMapper rowMapper = new TransactionRowMapper();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionQueryBuilder transactionQueryBuilder;

    @Autowired
    public TransactionRepository(JdbcTemplate jdbcTemplate, TransactionQueryBuilder transactionQueryBuilder) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionQueryBuilder = transactionQueryBuilder;
    }
   
 
    public List<Transaction> fetchTransactions(TransactionCriteria transactionCriteria) {
        List<Object> params = new ArrayList<>();
        String query = transactionQueryBuilder.getTransactionSearchQueryByTxnId(transactionCriteria, params);
        log.debug(query); 
        return jdbcTemplate.query(query, params.toArray(), rowMapper);
    }
    
    public List<Transaction> fetchSignedFile(Transaction transaction) {
        List<Object> params = new ArrayList<>();
        String consumerCode = transaction.getConsumerCode();
        String module = transaction.getModule();
        TransactionCriteria transactionCriteria = new TransactionCriteria();
        transactionCriteria.setModule(module);
        transactionCriteria.setConsumerCode(consumerCode);
        String query = transactionQueryBuilder.getEsignedFileQueryByConsumerCode(transactionCriteria, params);
        log.debug(query); 
        return jdbcTemplate.query(query, params.toArray(), rowMapper);
    }

    
}
