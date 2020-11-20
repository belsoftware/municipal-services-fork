package org.egov.lams.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.model.SearchCriteria;
import org.egov.lams.model.UserInfo;
import org.egov.lams.repository.LamsRepository;
import org.egov.lams.util.CommonUtils;
import org.egov.lams.util.LRConstants;
import org.egov.lams.validator.LamsValidator;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.web.models.LeaseAgreementRenewalDetail;
import org.egov.lams.web.models.user.UserDetailResponse;
import org.egov.lams.web.models.workflow.BusinessService;
import org.egov.lams.workflow.ActionValidator;
import org.egov.lams.workflow.LamsWorkflowService;
import org.egov.lams.workflow.WorkflowIntegrator;
import org.egov.lams.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
	
	@Autowired
	private LamsConfiguration config;
	
	@Autowired
	private WorkflowService workflowService;
	
	@Autowired
	private ActionValidator actionValidator;
	
	@Autowired
	private CommonUtils util;

	@Autowired
	private LamsWorkflowService lamsWorkflowService;
	
	public List<LeaseAgreementRenewal> create(LamsRequest request) {
		validator.validateFields(request);
		validator.validateBusinessService(request);
		enrichmentService.enrichCreateRequest(request);
		userService.createUser(request);
		repository.save(request);
		wfIntegrator.callWorkFlow(request);
		return request.getLeases();
	}

	public List<LeaseAgreementRenewal> search(SearchCriteria criteria, RequestInfo requestInfo, String servicename,
			HttpHeaders headers) {
		List<LeaseAgreementRenewal> leases = null;
		enrichmentService.enrichSearchCriteriaWithAccountId(requestInfo, criteria);
		
		// demo comment
		//to be uncommented CEMP tries with mob number
		/*if(criteria.getMobileNumber()!=null){
			UserDetailResponse userDetailResponse = userService.getUser(criteria,requestInfo);
	        if(userDetailResponse.getUser().size()==0){
	            return Collections.emptyList();
	        }
	        criteria.setAccountId(userDetailResponse.getUser().get(0).getId().toString());
        }*/
		leases = repository.getLeaseRenewals(criteria);
		leases.forEach(lease -> {
			List<UserInfo> userDetails = new ArrayList<UserInfo>();
			UserDetailResponse userDetailResponse = userService.getUserByUUid(lease.getAccountId(), requestInfo);
			System.out.println(userDetailResponse.getUser().get(0).getName());
			userDetails.add(userDetailResponse.getUser().get(0));
			lease.setUserDetails(userDetails);
		});
		validator.validateUserwithOwnerDetail(requestInfo, leases);
		return leases;
	}

	public List<LeaseAgreementRenewal> update(LamsRequest lamsRequest, String businessServicefromPath) {
        List<LeaseAgreementRenewal> leaseResponse = null;
        if (businessServicefromPath == null)
            businessServicefromPath = LRConstants.businessService_LAMS;
        validator.validateBusinessService(lamsRequest);
        String businessServiceName = config.getLamsBusinessServiceValue();
        BusinessService businessService = workflowService.getBusinessService(lamsRequest.getLeases().get(0).getTenantId(), 
        		lamsRequest.getRequestInfo(), businessServiceName);
        List<LeaseAgreementRenewal> searchResult = getLeasesWithInfo(lamsRequest);
        actionValidator.validateUpdateRequest(lamsRequest, businessService);
        enrichmentService.enrichLamsUpdateRequest(lamsRequest, businessService);
        validator.validateUpdate(lamsRequest, searchResult);
        
        Map<String, Boolean> idToIsStateUpdatableMap = util.getIdToIsStateUpdatableMap(businessService, searchResult);
        
        List<String> endStates = Collections.nCopies(lamsRequest.getLeases().size(),LRConstants.STATUS_APPROVED);
        if (config.getIsExternalWorkFlowEnabled()) {
            wfIntegrator.callWorkFlow(lamsRequest);
        } else {
            lamsWorkflowService.updateStatus(lamsRequest);
        }
        enrichmentService.postStatusEnrichment(lamsRequest,endStates);
        userService.createUser(lamsRequest);
        repository.update(lamsRequest, idToIsStateUpdatableMap );
        leaseResponse=  lamsRequest.getLeases();
		return leaseResponse;
	}
	
	
	public List<LeaseAgreementRenewal> getLeasesWithInfo(LamsRequest request){
        SearchCriteria criteria = new SearchCriteria();
        List<String> ids = new LinkedList<>();
        request.getLeases().forEach(lease -> {ids.add(lease.getId());});

        criteria.setTenantId(request.getLeases().get(0).getTenantId());
        criteria.setIds(ids);
        criteria.setBusinessService(request.getLeases().get(0).getBusinessService());

        List<LeaseAgreementRenewal> leases = repository.getLeaseRenewals(criteria);

        if(leases.isEmpty())
            return Collections.emptyList();
        return leases;
    }

	public List<LeaseAgreementRenewalDetail> getLeaseDetails(SearchCriteria criteria, RequestInfo requestInfo) {
		List<LeaseAgreementRenewalDetail> leases = null;
		//if(criteria.isEmpty())
            //criteria.setTenantId(requestInfo.getUserInfo().getTenantId());
		leases = repository.getLeaseDetails(criteria);
		return leases;
	}
}
