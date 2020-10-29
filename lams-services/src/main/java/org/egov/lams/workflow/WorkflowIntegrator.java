package org.egov.lams.workflow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
@Service
@Slf4j
public class WorkflowIntegrator {

	private static final String TENANTIDKEY = "tenantId";

	private static final String BUSINESSSERVICEKEY = "businessService";

	private static final String ACTIONKEY = "action";

	private static final String COMMENTKEY = "comment";

	private static final String MODULENAMEKEY = "moduleName";

	private static final String BUSINESSIDKEY = "businessId";

	private static final String DOCUMENTSKEY = "documents";

	private static final String ASSIGNEEKEY = "assignes";

	private static final String UUIDKEY = "uuid";

	private static final String LRMODULENAMEVALUE = "LAMS";

	private static final String WORKFLOWREQUESTARRAYKEY = "ProcessInstances";

	private static final String REQUESTINFOKEY = "RequestInfo";

	private static final String PROCESSINSTANCESJOSNKEY = "$.ProcessInstances";

	private static final String BUSINESSIDJOSNKEY = "$.businessId";

	private static final String STATUSJSONKEY = "$.state.applicationStatus";

	private RestTemplate rest;

	private LamsConfiguration config;

	@Autowired
	public WorkflowIntegrator(RestTemplate rest, LamsConfiguration config) {
		this.rest = rest;
		this.config = config;
	}

	public void callWorkFlow(LamsRequest lamsRequest) {
		LeaseAgreementRenewal currentLeaseRenewal = lamsRequest.getLeases().get(0);
		String wfTenantId = currentLeaseRenewal.getTenantId();
		String businessServiceFromMDMS = lamsRequest.getLeases().isEmpty()?null:currentLeaseRenewal.getBusinessService();
		if (businessServiceFromMDMS == null)
			businessServiceFromMDMS = LRConstants.businessService_LAMS;
		JSONArray array = new JSONArray();
		for (LeaseAgreementRenewal leaseRenewal : lamsRequest.getLeases()) {
				JSONObject obj = new JSONObject();
				List<Map<String, String>> uuidmaps = new LinkedList<>();
				if(!CollectionUtils.isEmpty(leaseRenewal.getAssignee())){

					leaseRenewal.getAssignee().forEach(assignee -> {
						Map<String, String> uuidMap = new HashMap<>();
						uuidMap.put(UUIDKEY, assignee);
						uuidmaps.add(uuidMap);
					});
				}
				obj.put(BUSINESSIDKEY, leaseRenewal.getApplicationNumber());
				obj.put(TENANTIDKEY, wfTenantId);
				obj.put(BUSINESSSERVICEKEY, currentLeaseRenewal.getWorkflowCode());
				obj.put(MODULENAMEKEY, LRMODULENAMEVALUE);

				obj.put(ACTIONKEY, leaseRenewal.getAction());
				obj.put(COMMENTKEY, leaseRenewal.getComment());
				if (!CollectionUtils.isEmpty(leaseRenewal.getAssignee()))
					obj.put(ASSIGNEEKEY, uuidmaps);
				obj.put(DOCUMENTSKEY, leaseRenewal.getWfDocuments());
				array.add(obj);
		}
		if(!array.isEmpty())
		{
			JSONObject workFlowRequest = new JSONObject();
			workFlowRequest.put(REQUESTINFOKEY, lamsRequest.getRequestInfo());
			workFlowRequest.put(WORKFLOWREQUESTARRAYKEY, array);
			String response = null;
			try {
				response = rest.postForObject(config.getWfHost().concat(config.getWfTransitionPath()), workFlowRequest, String.class);
			} catch (HttpClientErrorException e) {

				
				DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
				List<Object> errros = null;
				try {
					errros = responseContext.read("$.Errors");
				} catch (PathNotFoundException pnfe) {
					log.error("EG_TL_WF_ERROR_KEY_NOT_FOUND",
							" Unable to read the json path in error object : " + pnfe.getMessage());
					throw new CustomException("EG_TL_WF_ERROR_KEY_NOT_FOUND",
							" Unable to read the json path in error object : " + pnfe.getMessage());
				}
				throw new CustomException("EG_WF_ERROR", errros.toString());
			} catch (Exception e) {
				throw new CustomException("EG_WF_ERROR",
						" Exception occured while integrating with workflow : " + e.getMessage());
			}

			/*
			 * on success result from work-flow read the data and set the status back to LR
			 * object
			 */
			DocumentContext responseContext = JsonPath.parse(response);
			List<Map<String, Object>> responseArray = responseContext.read(PROCESSINSTANCESJOSNKEY);
			Map<String, String> idStatusMap = new HashMap<>();
			responseArray.forEach(
					object -> {

						DocumentContext instanceContext = JsonPath.parse(object);
						idStatusMap.put(instanceContext.read(BUSINESSIDJOSNKEY), instanceContext.read(STATUSJSONKEY));
					});

			lamsRequest.getLeases()
					.forEach(tlObj -> tlObj.setStatus(idStatusMap.get(tlObj.getApplicationNumber())));
		}
	}
}