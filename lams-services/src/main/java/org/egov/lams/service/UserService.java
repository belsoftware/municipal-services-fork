package org.egov.lams.service;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.lams.model.Citizen;
import org.egov.lams.model.SearchCriteria;
import org.egov.lams.repository.ServiceRequestRepository;
import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.user.CreateUserRequest;
import org.egov.lams.web.models.user.UserDetailResponse;
import org.egov.lams.web.models.user.UserResponse;
import org.egov.lams.web.models.user.UserSearchRequest;
import org.egov.lams.web.models.user.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserService {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Value("${egov.user.host}")
	private String userHost;

	@Value("${egov.user.context.path}")
	private String userContextPath;

	@Value("${egov.user.create.path}")
	private String userCreateEndpoint;

	@Value("${egov.user.search.path}")
	private String userSearchEndpoint;

	@Value("${egov.user.update.path}")
	private String userUpdateEndpoint;

	public void createUser(LamsRequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		List<String> roles = requestInfo.getUserInfo().getRoles().stream().map(Role::getCode)
				.collect(Collectors.toList());
		if(roles.contains(LRConstants.LR_COUNTER_EMPLOYEE)) {
			request.getLeases().stream().forEach(leaseRenewal -> {
				String accId = null;
				if (null != leaseRenewal.getCitizen()) {
					accId = isUserPresent(leaseRenewal.getCitizen(),requestInfo,leaseRenewal.getTenantId());
					if (StringUtils.isEmpty(accId)) {
						accId = createUser(leaseRenewal.getCitizen(),requestInfo,leaseRenewal.getTenantId());
					}
					leaseRenewal.setAccountId(accId);
				}
			});	
		}
	}
	
	private String isUserPresent(Citizen citizen, RequestInfo requestInfo, String tenantId) {
		UserSearchRequest searchRequest = UserSearchRequest.builder().userName(citizen.getMobileNumber())
				.tenantId(tenantId).userType(LRConstants.ROLE_CITIZEN).requestInfo(requestInfo).build();
		StringBuilder url = new StringBuilder(userHost+userSearchEndpoint); 
		UserResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, searchRequest), UserResponse.class);
		if(CollectionUtils.isEmpty(res.getUser())) {
			return null;
		}
		return res.getUser().get(0).getUuid().toString();
	}

	private String createUser(Citizen citizen, RequestInfo requestInfo, String tenantId) {
		citizen.setUserName(UUID.randomUUID().toString());
		citizen.setActive(true);
		citizen.setTenantId(tenantId);
		citizen.setType(UserType.CITIZEN);
		citizen.setRoles(Collections.singletonList(Role.builder()
				.code("CITIZEN")
				.name("Citizen")
				.build()));
		StringBuilder url = new StringBuilder(userHost+userCreateEndpoint); 
		CreateUserRequest req = CreateUserRequest.builder().citizen(citizen).requestInfo(requestInfo).build();
		UserResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, req), UserResponse.class);
		return res.getUser().get(0).getUuid().toString();
	}
	
	public UserDetailResponse getUser(SearchCriteria criteria,RequestInfo requestInfo){
        UserSearchRequest userSearchRequest = UserSearchRequest.builder().requestInfo(requestInfo)
        		.tenantId(criteria.getTenantId()).mobileNumber(criteria.getMobileNumber()).active(true)
        		.userType(LRConstants.ROLE_CITIZEN).build();
        StringBuilder url = new StringBuilder(userHost+userSearchEndpoint); 
        UserDetailResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, userSearchRequest), UserDetailResponse.class);
        return res;
    }
	
	public UserDetailResponse getUserByUUid(String accountId,RequestInfo requestInfo){
		UserSearchRequest userSearchRequest = UserSearchRequest.builder().requestInfo(requestInfo)
        		.uuid(Arrays.asList(accountId))
        		.userType(LRConstants.ROLE_CITIZEN).build();
        StringBuilder url = new StringBuilder(userHost+userSearchEndpoint); 
        UserDetailResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, userSearchRequest), UserDetailResponse.class);
        return res;
    }
    
}
