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
import org.egov.lams.model.SearchCriteria;
import org.egov.lams.model.UserInfo;
import org.egov.lams.repository.ServiceRequestRepository;
import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.user.CreateUserRequest;
import org.egov.lams.web.models.user.UserDetailResponse;
import org.egov.lams.web.models.user.UserResponse;
import org.egov.lams.web.models.user.UserSearchRequest;
import org.egov.tracer.model.CustomException;
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
		request.getLeases().forEach(leaseRenewal -> {
			if (roles.contains(LRConstants.LR_COUNTER_EMPLOYEE)) {
				leaseRenewal.getUserDetails().forEach(userDetail -> {

					if (userDetail.getUuid() == null)
						leaseRenewal.setAccountId(createUser(userDetail, requestInfo, leaseRenewal.getTenantId()));
				});
			} else if (requestInfo.getUserInfo().getType().equalsIgnoreCase(LRConstants.ROLE_CITIZEN)) {
				List<UserInfo> userDetails = new ArrayList<UserInfo>();
				UserInfo userInfo = new UserInfo();
				userInfo.addUserDetail(requestInfo.getUserInfo());
				userDetails.add(userInfo);
				leaseRenewal.setUserDetails(userDetails);
			}
		});
	}
	
	/*private String isUserPresent(UserInfo userInfo, RequestInfo requestInfo, String tenantId) {
		UserSearchRequest searchRequest = UserSearchRequest.builder().userName(userInfo.getMobileNumber())
				.tenantId(tenantId).userType(LRConstants.ROLE_CITIZEN).requestInfo(requestInfo).build();
		StringBuilder url = new StringBuilder(userHost+userSearchEndpoint); 
		UserResponse res = mapper.convertValue(serviceRequestRepository.fetchResult(url, searchRequest), UserResponse.class);
		if(CollectionUtils.isEmpty(res.getUser())) {
			throw new CustomException("INVALID USER","UUID does not exists");
		}
		return res.getUser().get(0).getUuid().toString();
	}*/

	private String createUser(UserInfo userInfo, RequestInfo requestInfo, String tenantId) {
		userInfo.setUserName(UUID.randomUUID().toString());
		userInfo.setActive(true);
		userInfo.setTenantId(tenantId);
		userInfo.setType(LRConstants.ROLE_CITIZEN);
		userInfo.setRoles(Collections.singletonList(Role.builder()
				.code(LRConstants.ROLE_CITIZEN)
				.name("Citizen")
				.build()));
		StringBuilder url = new StringBuilder(userHost+userCreateEndpoint); 
		CreateUserRequest req = CreateUserRequest.builder().userInfo(userInfo).requestInfo(requestInfo).build();
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
