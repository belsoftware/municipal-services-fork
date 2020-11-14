package org.egov.lams.repository;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
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
import org.springframework.util.CollectionUtils;
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
    
    public void update(LamsRequest lamsRequest, Map<String, Boolean> idToIsStateUpdatableMap) {
    	RequestInfo requestInfo = lamsRequest.getRequestInfo();
        List<LeaseAgreementRenewal> leases = lamsRequest.getLeases();

        List<LeaseAgreementRenewal> leasesForStatusUpdate = new LinkedList<>();
        List<LeaseAgreementRenewal> leasesForUpdate = new LinkedList<>();


        for (LeaseAgreementRenewal lease : leases) {
            if (idToIsStateUpdatableMap.get(lease.getId())) {
                leasesForUpdate.add(lease);
            }
            else {
                leasesForStatusUpdate.add(lease);
            }
        }
        System.out.println("in update "+leasesForUpdate.size() +" - "+leasesForStatusUpdate.size());
        if (!CollectionUtils.isEmpty(leasesForUpdate))
            producer.push(config.getUpdateLamsLRTopic(), new LamsRequest(requestInfo, leasesForUpdate));

        if (!CollectionUtils.isEmpty(leasesForStatusUpdate))
            producer.push(config.getUpdateLamsLRWorkflowTopic(), new LamsRequest(requestInfo, leasesForStatusUpdate));

    }
    
    
    public List<LeaseAgreementRenewal> getLeaseRenewals(SearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getLeaseRenewalSearchQuery(criteria, preparedStmtList);
        List<LeaseAgreementRenewal> leases =  jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
        return leases;
    }
    
}
