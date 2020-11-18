package org.egov.lams.validator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class LamsValidator {

	public void validateFields(LamsRequest request) {
		Map<String, String> errorMap = new HashMap<>();
		request.getLeases().forEach(lease -> {
			if (lease.getSurveyNo() == null || lease.getSurveyNo().isEmpty())
				errorMap.put("NULL_SurveyNo", " Survey Number cannot be empty");
			//if (!lease.getTenantId().equalsIgnoreCase(request.getRequestInfo().getUserInfo().getTenantId()))
				//errorMap.put("Invalid Tenant", "Invalid tenant id");
			if (!errorMap.isEmpty())
				throw new CustomException(errorMap);
		});
	}

	public void validateBusinessService(LamsRequest request) {
		request.getLeases().forEach(lease -> {
			if (!StringUtils.equals(lease.getBusinessService(), LRConstants.businessService_LAMS))
				throw new CustomException("BUSINESSSERVICE_NOTMATCHING",
						" The business service inside license not matching with the one sent in path variable");
		});
	}
	
	public void validateUserwithOwnerDetail(RequestInfo request,List<LeaseAgreementRenewal> leases){
    	try {
        Map<String,String> errorMap = new HashMap<>();
        if(request.getUserInfo().getType().equals("CITIZEN") ) {
        	String uuid = request.getUserInfo().getUuid();
            leases.forEach(lease -> {
                Boolean flag = false;
                if(flag || lease.getLeaseDetails().getAuditDetails().getCreatedBy().equals(uuid)) {
                    flag=true;
                }
                
                if(!flag)
                    errorMap.put("UNAUTHORIZED USER","Unauthorized user to access the application:  "+lease.getApplicationNumber());
            });

            if(!errorMap.isEmpty())
                throw new CustomException(errorMap);
            
        }
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
        
    }
	
	public void validateUpdate(LamsRequest request, List<LeaseAgreementRenewal> searchResult) {
        List<LeaseAgreementRenewal> leases = request.getLeases();
        if (searchResult.size() != leases.size())
            throw new CustomException("INVALID UPDATE", "The lease to be updated is not in database");
        validateAllIds(searchResult, leases);
        //validateFields(request);
        validateDuplicateDocuments(request);
    }
	
	private void validateAllIds(List<LeaseAgreementRenewal> searchResult,List<LeaseAgreementRenewal> leases){

        Map<String,LeaseAgreementRenewal> idToLeaseRenewalFromSearch = new HashMap<>();
        searchResult.forEach(lease -> {
            idToLeaseRenewalFromSearch.put(lease.getId(),lease);
        });

        Map<String,String> errorMap = new HashMap<>();
        leases.forEach(license -> {
            LeaseAgreementRenewal searchedLicense = idToLeaseRenewalFromSearch.get(license.getId());

            if(!searchedLicense.getApplicationNumber().equalsIgnoreCase(license.getApplicationNumber()))
                errorMap.put("INVALID UPDATE","The application number from search: "+searchedLicense.getApplicationNumber()
                        +" and from update: "+license.getApplicationNumber()+" does not match");

            if(!searchedLicense.getLeaseDetails().getId().
                    equalsIgnoreCase(license.getLeaseDetails().getId()))
                errorMap.put("INVALID UPDATE","The id "+license.getLeaseDetails().getId()+" does not exist");

            compareIdList(getApplicationDocIds(searchedLicense),getApplicationDocIds(license),errorMap);
            compareIdList(getVerficationDocIds(searchedLicense),getVerficationDocIds(license),errorMap);
        });

        if(!CollectionUtils.isEmpty(errorMap))
            throw new CustomException(errorMap);
    }
	
	private List<String> getApplicationDocIds(LeaseAgreementRenewal lease){
        List<String> applicationDocIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(lease.getLeaseDetails().getApplicationDocuments())){
            lease.getLeaseDetails().getApplicationDocuments().forEach(document -> {
                applicationDocIds.add(document.getId());
            });
        }
        return applicationDocIds;
    }
	
	private List<String> getVerficationDocIds(LeaseAgreementRenewal lease){
        List<String> verficationDocIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(lease.getLeaseDetails().getVerificationDocuments())) {
            lease.getLeaseDetails().getVerificationDocuments().forEach(document -> {
                verficationDocIds.add(document.getId());
            });
        }
        return verficationDocIds;
    }
	
	private void compareIdList(List<String> searchIds, List<String> updateIds, Map<String, String> errorMap) {
		if (!CollectionUtils.isEmpty(searchIds))
			searchIds.forEach(searchId -> {
				if (!updateIds.contains(searchId))
					errorMap.put("INVALID UPDATE", "The id: " + searchId + " was not present in update request");
			});
	}
	
	private void validateDuplicateDocuments(LamsRequest request){
        List<String> documentFileStoreIds = new LinkedList();
        request.getLeases().forEach(license -> {
            if(license.getLeaseDetails().getApplicationDocuments()!=null){
                license.getLeaseDetails().getApplicationDocuments().forEach(
                        document -> {
                                if(documentFileStoreIds.contains(document.getFileStoreId()))
                                    throw new CustomException("DUPLICATE_DOCUMENT ERROR","Same document cannot be used multiple times");
                                else documentFileStoreIds.add(document.getFileStoreId());
                        }
                );
            }
        });
    }
}
