package org.egov.lams.workflow;

import java.util.Map;

import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.LamsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class LamsWorkflowService {

	@Autowired
    private ActionValidator actionValidator;
	@Autowired
    private WorkflowConfig workflowConfig;

    public void updateStatus(LamsRequest request){
        actionValidator.validateUpdateRequest(request,null);
        changeStatus(request);
    }


    private void changeStatus(LamsRequest request){
       Map<String,String> actionToStatus =  workflowConfig.getActionStatusMap();
       request.getLeases().forEach(lease -> {
             lease.setStatus(actionToStatus.get(lease.getAction()));
             if(lease.getAction().equalsIgnoreCase(LRConstants.ACTION_APPROVE)){
                 Long time = System.currentTimeMillis();
                 lease.setApprovedDate(time);
             }
       });
    }

}
