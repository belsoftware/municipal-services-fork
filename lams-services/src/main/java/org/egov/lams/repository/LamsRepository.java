package org.egov.lams.repository;


import java.util.ArrayList;
import java.util.List;

import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.model.SearchCriteria;
import org.egov.lams.producer.Producer;
import org.egov.lams.repository.builder.LamsQueryBuilder;
import org.egov.lams.rowmapper.LamsRowMapper;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
public class LamsRepository {

    private Producer producer;
    
    private LamsConfiguration config;

    private JdbcTemplate jdbcTemplate;

    private LamsQueryBuilder queryBuilder;

    private LamsRowMapper rowMapper;
    
    private RestTemplate restTemplate;

    @Autowired
    public LamsRepository(Producer producer, LamsConfiguration config,LamsQueryBuilder queryBuilder,
    		JdbcTemplate jdbcTemplate,LamsRowMapper rowMapper,RestTemplate restTemplate) {
        this.producer = producer;
        this.config = config;
        this.jdbcTemplate = jdbcTemplate;
        this.queryBuilder = queryBuilder ; 
        this.rowMapper = rowMapper;
        this.restTemplate = restTemplate;
    }

    public void save(LamsRequest lamsRequest) {
    	
        producer.push(config.getSaveLamsLRTopic(), lamsRequest);
    }
    
    public void update(LamsRequest lamsRequest) {
    	
        producer.push(config.getUpdateLamsLRTopic(), lamsRequest);
    }
    
    
    public List<LeaseAgreementRenewal> getLeaseRenewals(SearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getLeaseRenewalSearchQuery(criteria, preparedStmtList);
        List<LeaseAgreementRenewal> challans =  jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
        return challans;
    }
    
}
