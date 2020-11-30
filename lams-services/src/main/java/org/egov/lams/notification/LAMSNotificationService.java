package org.egov.lams.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.model.SMSRequest;
import org.egov.lams.repository.ServiceRequestRepository;
import org.egov.lams.util.*;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LAMSNotificationService {

	private LamsConfiguration config;

	private ServiceRequestRepository serviceRequestRepository;

	private NotificationUtil util;

	@Autowired
	public LAMSNotificationService(LamsConfiguration config, ServiceRequestRepository serviceRequestRepository,
			NotificationUtil util) {
		this.config = config;
		this.serviceRequestRepository = serviceRequestRepository;
		this.util = util;
	}

	public void process(LamsRequest request) {

		String businessService = request.getLeases().get(0).getBusinessService();
		List<SMSRequest> smsRequestsLams = new LinkedList<>();
		if (null != config.getIsSMSEnabled()) {
			if (config.getIsSMSEnabled()) {
				enrichSMSRequest(request, smsRequestsLams);
				if (!CollectionUtils.isEmpty(smsRequestsLams))
					util.sendSMS(smsRequestsLams, true);
			}
		}
	}

	private void enrichSMSRequest(LamsRequest request, List<SMSRequest> smsRequests) {
		String tenantId = request.getLeases().get(0).getTenantId();
		for (LeaseAgreementRenewal leaseRenewal : request.getLeases()) {
			String message = null;
			String localizationMessages = util.getLocalizationMessages(tenantId, request.getRequestInfo());
			message = util.getCustomizedMsg(request.getRequestInfo(), leaseRenewal, localizationMessages);
			if (message == null)
				continue;

			Map<String, String> mobileNumberToOwner = new HashMap<>();
			
			leaseRenewal.getUserDetails().forEach(userdetail->{
				if(userdetail.getMobileNumber()!= null)
					mobileNumberToOwner.put(userdetail.getMobileNumber(),
						userdetail.getName());
			});
			smsRequests.addAll(util.createSMSRequest(message, mobileNumberToOwner));
		}
	}

	/**
	 * Fetches UUIDs of CITIZENs based on the phone number.
	 * 
	 * @param mobileNumbers
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */
	private Map<String, String> fetchUserUUIDs(Set<String> mobileNumbers, RequestInfo requestInfo, String tenantId) {
		Map<String, String> mapOfPhnoAndUUIDs = new HashMap<>();
		StringBuilder uri = new StringBuilder();
		uri.append(config.getUserHost()).append(config.getUserSearchEndpoint());
		Map<String, Object> userSearchRequest = new HashMap<>();
		userSearchRequest.put("RequestInfo", requestInfo);
		userSearchRequest.put("tenantId", tenantId);
		userSearchRequest.put("userType", "CITIZEN");
		for (String mobileNo : mobileNumbers) {
			userSearchRequest.put("userName", mobileNo);
			try {
				Object user = serviceRequestRepository.fetchResult(uri, userSearchRequest);
				if (null != user) {
					String uuid = JsonPath.read(user, "$.user[0].uuid");
					mapOfPhnoAndUUIDs.put(mobileNo, uuid);
				} else {
					log.error("Service returned null while fetching user for username - " + mobileNo);
				}
			} catch (Exception e) {
				log.error("Exception while fetching user for username - " + mobileNo);
				log.error("Exception trace: ", e);
				continue;
			}
		}
		return mapOfPhnoAndUUIDs;
	}

}