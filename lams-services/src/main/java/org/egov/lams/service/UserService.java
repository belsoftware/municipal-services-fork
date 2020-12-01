package org.egov.lams.service;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.lams.model.SearchCriteria;
import org.egov.lams.model.UserInfo;
import org.egov.lams.repository.ServiceRequestRepository;
import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.user.CreateUserRequest;
import org.egov.lams.web.models.user.UserDetailResponse;
import org.egov.lams.web.models.user.UserSearchRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
		userInfo.setRoles(
				Collections.singletonList(Role.builder().code(LRConstants.ROLE_CITIZEN).name("Citizen").build()));
		StringBuilder uri = new StringBuilder(userHost)
                .append(userContextPath)
                .append(userCreateEndpoint);
		UserDetailResponse userDetailResponse = userCall(new CreateUserRequest(requestInfo, userInfo), uri);
		if (userDetailResponse.getUser().get(0).getUuid() == null) {
			throw new CustomException("INVALID USER RESPONSE", "The user created has uuid as null");
		}
		return userDetailResponse.getUser().get(0).getUuid().toString();
	}

	@SuppressWarnings("unchecked")
	private UserDetailResponse userCall(Object userRequest, StringBuilder url) {

		String dobFormat = null;
		if (url.indexOf(userSearchEndpoint) != -1 || url.indexOf(userUpdateEndpoint) != -1)
			dobFormat = "yyyy-MM-dd";
		else if (url.indexOf(userCreateEndpoint) != -1)
			dobFormat = "dd/MM/yyyy";
		try {
			LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(url, userRequest);
			parseResponse(responseMap, dobFormat);
			UserDetailResponse userDetailResponse = mapper.convertValue(responseMap, UserDetailResponse.class);
			return userDetailResponse;
		} catch (IllegalArgumentException e) {
			throw new CustomException("IllegalArgumentException", "ObjectMapper not able to convertValue in userCall");
		}
	}

	@SuppressWarnings("unchecked")
	private void parseResponse(LinkedHashMap<String, Object> responeMap, String dobFormat) {
		List<LinkedHashMap<String, Object>> users = (List<LinkedHashMap<String, Object>>) responeMap.get("user");
		String format1 = "dd-MM-yyyy HH:mm:ss";

		if (null != users) {

			users.forEach(map -> {

				map.put("createdDate", dateTolong((String) map.get("createdDate"), format1));
				if ((String) map.get("lastModifiedDate") != null)
					map.put("lastModifiedDate", dateTolong((String) map.get("lastModifiedDate"), format1));
				if ((String) map.get("dob") != null)
					map.put("dob", dateTolong((String) map.get("dob"), dobFormat));
				if ((String) map.get("pwdExpiryDate") != null)
					map.put("pwdExpiryDate", dateTolong((String) map.get("pwdExpiryDate"), format1));
			});
		}
	}

	private Long dateTolong(String date, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		Date d = null;
		try {
			d = f.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return d.getTime();
	}

	public UserDetailResponse getUser(SearchCriteria criteria,RequestInfo requestInfo){
        UserSearchRequest userSearchRequest = UserSearchRequest.builder().requestInfo(requestInfo)
        		.tenantId(criteria.getTenantId()).mobileNumber(criteria.getMobileNumber()).active(true)
        		.userType(LRConstants.ROLE_CITIZEN).build();
        StringBuilder url = new StringBuilder(userHost+userContextPath+userSearchEndpoint); 
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
