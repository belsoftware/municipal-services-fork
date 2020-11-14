package org.egov.lams.workflow;

import java.util.HashMap;
import java.util.Map;

import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.workflow.BusinessService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Component
public class ActionValidator {

	@Autowired
    private WorkflowService workflowService;


    public void validateUpdateRequest(LamsRequest request,BusinessService businessService){
        validateDocumentsForUpdate(request);
        validateIds(request,businessService);
    }


    private void validateDocumentsForUpdate(LamsRequest request){
        Map<String,String> errorMap = new HashMap<>();
        request.getLeases().forEach(lease -> {
            if(LRConstants.ACTION_APPLY.equalsIgnoreCase(lease.getAction())){
                if(lease.getLeaseDetails().getApplicationDocuments()==null)
                    errorMap.put("INVALID STATUS","Status cannot be APPLY when application document are not provided");
            }
        });

        if(!errorMap.isEmpty())
            throw new CustomException(errorMap);
    }
    
    private void validateIds(LamsRequest request,BusinessService businessService){
        Map<String,String> errorMap = new HashMap<>();
        request.getLeases().forEach(lease -> {

            String namefBusinessService=lease.getBusinessService();
            if((namefBusinessService==null) || (namefBusinessService.equals(LRConstants.businessService_LAMS)))
            {
                if(!workflowService.isStateUpdatable(lease.getStatus(), businessService)) {
                    if (lease.getId() == null)
                        errorMap.put("INVALID UPDATE", "Id of LeaseRenewal cannot be null");
                    if(lease.getLeaseDetails().getId()==null)
                        errorMap.put("INVALID UPDATE", "Id of LeaseRenewalDetail cannot be null");
                    if(!CollectionUtils.isEmpty(lease.getLeaseDetails().getApplicationDocuments())){
                        lease.getLeaseDetails().getApplicationDocuments().forEach(document -> {
                            if(document.getId()==null)
                                errorMap.put("INVALID UPDATE", "Id of applicationDocument cannot be null");
                        });
                    }
                }
            }
        });
        if(!errorMap.isEmpty())
            throw new CustomException(errorMap);
    }


}
