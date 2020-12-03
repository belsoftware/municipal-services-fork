package org.egov.lams.service;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.model.SearchCriteria;
import org.egov.lams.models.Idgen.IdResponse;
import org.egov.lams.repository.IdGenRepository;
import org.egov.lams.repository.LamsRepository;
import org.egov.lams.util.CommonUtils;
import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.AuditDetails;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.web.models.workflow.BusinessService;
import org.egov.lams.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class EnrichmentService {

	@Autowired
    private IdGenRepository idGenRepository;
	@Autowired
    private LamsConfiguration config;
	@Autowired
    private CommonUtils commUtils;
	@Autowired
    private UserService userService;
	@Autowired
    private LamsRepository lamsRepository;
	@Autowired
	private WorkflowService workflowService;

    public void enrichCreateRequest(LamsRequest lamsRequset) {
        RequestInfo requestInfo = lamsRequset.getRequestInfo();
        String uuid = requestInfo.getUserInfo().getUuid();
        AuditDetails auditDetails = commUtils.getAuditDetails(uuid, true);
        lamsRequset.getLeases().forEach(leaseRenewals -> {
	        leaseRenewals.setAuditDetails(auditDetails);
	        leaseRenewals.setId(UUID.randomUUID().toString());
	        leaseRenewals.setStatus(LRConstants.LR_APPLIED);
	        leaseRenewals.setFilestoreid(null);
	        if (requestInfo.getUserInfo().getType().equalsIgnoreCase(LRConstants.ROLE_CITIZEN))
	        	leaseRenewals.setAccountId(requestInfo.getUserInfo().getUuid().toString());
	        leaseRenewals.getLeaseDetails().setId(UUID.randomUUID().toString());
	        leaseRenewals.getLeaseDetails().setSurveyNo(leaseRenewals.getSurveyNo());
	        leaseRenewals.setApplicationDate(auditDetails.getCreatedTime());
	        leaseRenewals.getLeaseDetails().getApplicationDocuments().forEach(document -> {
                document.setId(UUID.randomUUID().toString());
                document.setActive(true);
            });
        });
        setIdgenIds(lamsRequset);
    }

    private List<String> getIdList(RequestInfo requestInfo, String tenantId, String idKey,
                                   String idformat, int count) {
        List<IdResponse> idResponses = idGenRepository.getId(requestInfo, tenantId, idKey, idformat, count).getIdResponses();

        if (CollectionUtils.isEmpty(idResponses))
            throw new CustomException("IDGEN ERROR", "No ids returned from idgen Service");

        return idResponses.stream()
                .map(IdResponse::getId).collect(Collectors.toList());
    }

    private void setIdgenIds(LamsRequest request) {
        RequestInfo requestInfo = request.getRequestInfo();
        String tenantId = request.getLeases().get(0).getTenantId();
        request.getLeases().forEach(lease -> {
        	//System.out.println("requestInfo, tenantId "+requestInfo + tenantId);
        	String applicationNumber = getIdList(requestInfo, tenantId, config.getLamsLRApplNumIdgenName(), config.getLamsLRApplNumIdgenFormat(), 1).get(0);
        	lease.setApplicationNumber(applicationNumber);
        });
    }

	public void enrichSearchCriteriaWithAccountId(RequestInfo requestInfo, SearchCriteria criteria) {
		if(criteria.isEmpty() && requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN")){
            criteria.setAccountId(requestInfo.getUserInfo().getUuid().toString());
            criteria.setMobileNumber(requestInfo.getUserInfo().getUserName());
            criteria.setTenantId(requestInfo.getUserInfo().getTenantId());
        }
	}

	public void enrichLamsUpdateRequest(LamsRequest lamsRequest, BusinessService businessService) {
		RequestInfo requestInfo = lamsRequest.getRequestInfo();
        AuditDetails auditDetails = commUtils.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
        lamsRequest.getLeases().forEach(lease -> {
            lease.setAuditDetails(auditDetails);
            enrichAssignes(lease);
            String nameOfBusinessService = lease.getBusinessService();
            if(nameOfBusinessService==null){
                nameOfBusinessService=LRConstants.businessService_LAMS;
                lease.setBusinessService(nameOfBusinessService);
            }
            if (workflowService.isStateUpdatable(lease.getStatus(), businessService)) {
                lease.getLeaseDetails().setAuditDetails(auditDetails);

                if(!CollectionUtils.isEmpty(lease.getLeaseDetails().getApplicationDocuments())){
                	//List<String> docIdsStored =new ArrayList<String>();
                	List<String> docIdsRecived =new ArrayList<String>();
                	/*SearchCriteria criteria = new SearchCriteria();
                	criteria.setApplicationNumber(lease.getApplicationNumber());
                	criteria.setTenantId(lease.getTenantId());
					List<LeaseAgreementRenewal> leasesStored = lamsRepository.getLeaseRenewals(criteria );
					leasesStored.forEach(leaseStored -> {
						leaseStored.getLeaseDetails().getApplicationDocuments().forEach(documentStored -> {
							docIdsStored.add(documentStored.getId());
						});
					});*/
                    lease.getLeaseDetails().getApplicationDocuments().forEach(document -> {
                        if(document.getId()==null){
                            document.setId(UUID.randomUUID().toString());
                            document.setActive(true);
                        }
                        else if(!document.getActive()){
                        	docIdsRecived.add(document.getId());
                        }
                    });
                    //docIdsStored.removeAll(docIdsRecived);
                    if(docIdsRecived.size()>0)
                    	lamsRepository.deleteApplDocs(docIdsRecived);
                }
            }
            else {
                if(!CollectionUtils.isEmpty(lease.getLeaseDetails().getVerificationDocuments())){
                    lease.getLeaseDetails().getVerificationDocuments().forEach(document -> {
                        if(document.getId()==null){
                            document.setId(UUID.randomUUID().toString());
                            document.setActive(true);
                        }
                    });
                }
            }
        });
       
	}
	
	public void enrichAssignes(LeaseAgreementRenewal lease) {
		if (lease.getAction().equalsIgnoreCase(LRConstants.CITIZEN_SENDBACK_ACTION)) {
			List<String> assignes = new LinkedList<>();
			if (lease.getAccountId() != null)
				assignes.add(lease.getAccountId());
			lease.setAssignee(assignes);
		}
	}

	public void postStatusEnrichment(LamsRequest lamsRequest,List<String>endstates){
        List<LeaseAgreementRenewal> leases = lamsRequest.getLeases();
		for (int i = 0; i < leases.size(); i++) {
            LeaseAgreementRenewal lease = leases.get(i);
            if ((lease.getStatus() != null) && lease.getStatus().equalsIgnoreCase(endstates.get(i))) {
                Long time = System.currentTimeMillis();
                lease.setApprovedDate(time);
            }
        }
    }

}
