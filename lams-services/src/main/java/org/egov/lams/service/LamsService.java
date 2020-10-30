package org.egov.lams.service;

import java.util.List;

import org.egov.lams.repository.LamsRepository;
import org.egov.lams.validator.LamsValidator;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.workflow.WorkflowIntegrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class LamsService {

    @Autowired
    private EnrichmentService enrichmentService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private LamsRepository repository;

    @Autowired
    private LamsValidator validator;
    
    @Autowired
    private WorkflowIntegrator wfIntegrator;
    
    
	public List<LeaseAgreementRenewal> create(LamsRequest request) {
		validator.validateFields(request);
		validator.validateBusinessService(request);
		enrichmentService.enrichCreateRequest(request);
		userService.createUser(request);
		repository.save(request);
		wfIntegrator.callWorkFlow(request);
		return request.getLeases();
	}
	
}