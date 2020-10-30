package org.egov.lams.validator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.LamsRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;

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
}
