package org.egov.lams.util;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.web.models.AuditDetails;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.web.models.workflow.BusinessService;
import org.egov.lams.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
@Component
@Getter
public class CommonUtils {

	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private LamsConfiguration configs;
	
	@Autowired
	private WorkflowService workflowService;
	

  
    /**
     * Method to return auditDetails for create/update flows
     *
     * @param by
     * @param isCreate
     * @return AuditDetails
     */
    public AuditDetails getAuditDetails(String by, Boolean isCreate) {
    	
        Long time = System.currentTimeMillis();
        
        if(isCreate)
            return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time).build();
        else
            return AuditDetails.builder().lastModifiedBy(by).lastModifiedTime(time).build();
    }
    
    public Map<String, Boolean> getIdToIsStateUpdatableMap(BusinessService businessService, List<LeaseAgreementRenewal> searchresult) {
        Map<String, Boolean> idToIsStateUpdatableMap = new HashMap<>();
        searchresult.forEach(result -> {
            idToIsStateUpdatableMap.put(result.getId(), workflowService.isStateUpdatable(result.getStatus(), businessService));
        });
        return idToIsStateUpdatableMap;
    }
	
}
