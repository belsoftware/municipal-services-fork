package org.egov.lams.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.model.SearchCriteria;
import org.egov.lams.model.UserInfo;
import org.egov.lams.repository.LamsRepository;
import org.egov.lams.util.CommonUtils;
import org.egov.lams.util.LRConstants;
import org.egov.lams.validator.LamsValidator;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.web.models.LeaseAgreementRenewalDetail;
import org.egov.lams.web.models.RequestInfoWrapper;
import org.egov.lams.web.models.user.UserDetailResponse;
import org.egov.lams.web.models.workflow.BusinessService;
import org.egov.lams.workflow.ActionValidator;
import org.egov.lams.workflow.LamsWorkflowService;
import org.egov.lams.workflow.WorkflowIntegrator;
import org.egov.lams.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

@Service
public class LamsService {

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private UserService userService;

	@Autowired
	private LamsRepository repository;

	@Autowired
	private LamsValidator validator;

	@Autowired
	private WorkflowIntegrator wfIntegrator;
	
	@Autowired
	private LamsConfiguration config;
	
	@Autowired
	private WorkflowService workflowService;
	
	@Autowired
	private ActionValidator actionValidator;
	
	@Autowired
	private CommonUtils util;

	@Autowired
	private LamsWorkflowService lamsWorkflowService;
	
	@Autowired
	private RestTemplate rest;
	
	public List<LeaseAgreementRenewal> create(LamsRequest request) {
		validator.validateFields(request);
		validator.validateBusinessService(request);
		enrichmentService.enrichCreateRequest(request);
		userService.createUser(request);
		repository.save(request);
		wfIntegrator.callWorkFlow(request);
		return request.getLeases();
		
		
	}

	public List<LeaseAgreementRenewal> search(SearchCriteria criteria, RequestInfo requestInfo, String servicename,
			HttpHeaders headers) {
		List<LeaseAgreementRenewal> leases = null;
		enrichmentService.enrichSearchCriteriaWithAccountId(requestInfo, criteria);
		
		if(criteria.getMobileNumber()!=null){
			UserDetailResponse userDetailResponse = userService.getUser(criteria,requestInfo);
	        if(userDetailResponse.getUser().size()==0){
	            return Collections.emptyList();
	        }
	        criteria.setAccountId(userDetailResponse.getUser().get(0).getUuid().toString());
        }
		
		if(requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN"))
			criteria.setTenantId(null);
		leases = repository.getLeaseRenewals(criteria);
		leases.forEach(lease -> {
			List<UserInfo> userDetails = new ArrayList<UserInfo>();
			UserDetailResponse userDetailResponse = userService.getUserByUUid(lease.getAccountId(), requestInfo);
			userDetails.add(userDetailResponse.getUser().get(0));
			lease.setUserDetails(userDetails);
		});
		validator.validateUserwithOwnerDetail(requestInfo, leases);
		return leases;
	}

	public List<LeaseAgreementRenewal> update(LamsRequest lamsRequest, String businessServicefromPath) {
        List<LeaseAgreementRenewal> leaseResponse = null;
        if (businessServicefromPath == null)
            businessServicefromPath = LRConstants.businessService_LAMS;
        validator.validateBusinessService(lamsRequest);
        String businessServiceName = config.getLamsBusinessServiceValue();
        BusinessService businessService = workflowService.getBusinessService(lamsRequest.getLeases().get(0).getTenantId(), 
        		lamsRequest.getRequestInfo(), businessServiceName);
        actionValidator.validateUpdateRequest(lamsRequest, businessService);
        enrichmentService.enrichLamsUpdateRequest(lamsRequest, businessService);
        List<LeaseAgreementRenewal> searchResult = getLeasesWithInfo(lamsRequest);
        validator.validateUpdate(lamsRequest, searchResult);
        
        Map<String, Boolean> idToIsStateUpdatableMap = util.getIdToIsStateUpdatableMap(businessService, searchResult);
        
        List<String> endStates = Collections.nCopies(lamsRequest.getLeases().size(),LRConstants.STATUS_APPROVED);
        if (config.getIsExternalWorkFlowEnabled()) {
            wfIntegrator.callWorkFlow(lamsRequest);
        } else {
            lamsWorkflowService.updateStatus(lamsRequest);
        }
        enrichmentService.postStatusEnrichment(lamsRequest,endStates);
        //userService.createUser(lamsRequest);
        repository.update(lamsRequest, idToIsStateUpdatableMap );
        leaseResponse=  lamsRequest.getLeases();
		return leaseResponse;
	}
	
	
	public List<LeaseAgreementRenewal> getLeasesWithInfo(LamsRequest request){
        SearchCriteria criteria = new SearchCriteria();
        List<String> ids = new LinkedList<>();
        request.getLeases().forEach(lease -> {ids.add(lease.getId());});

        criteria.setTenantId(request.getLeases().get(0).getTenantId());
        criteria.setIds(ids);
        criteria.setBusinessService(request.getLeases().get(0).getBusinessService());

        List<LeaseAgreementRenewal> leases = repository.getLeaseRenewals(criteria);

        if(leases.isEmpty())
            return Collections.emptyList();
        return leases;
    }

	public List<LeaseAgreementRenewalDetail> getLeaseDetails(SearchCriteria criteria, RequestInfo requestInfo) {
		List<LeaseAgreementRenewalDetail> leases = null;
		//if(criteria.isEmpty())
            //criteria.setTenantId(requestInfo.getUserInfo().getTenantId());
		leases = repository.getLeaseDetails(criteria);
		return leases;
	}
	
	final List<String> allTenants1 = Arrays.asList(new String[]{"pb.agra","pb.delhi","pb","pb.lucknow","pb.pune","pb.secunderabad","pb.testing","pb.ambala","pb.bareilly","pb.mathura","pb.allahabad","pb.meerut","pb.mhow","pb.kasauli","pb.lebong","pb.jammu","pb.jalandhar","pb.danapur","pb.dagshai","pb.roorkee","pb.pachmarhi","pb.nasirabad","pb.deolali","pb.dehuroad","pb.ahmednagar","pb.amritsar","pb.ramgarh","pb.jalapahar","pb.wellington","pb.subathu","pb.almora","pb.chakrata","pb.clementtown","pb.dehradun","pb.faizabad","pb.fatehgarh","pb.jabalpur","pb.kanpur","pb.landour","pb.lansdowne","pb.khasyol","pb.jutogh","pb.shahjahanpur","pb.varanasi","pb.ferozepur","pb.dalhousie","pb.shillong","pb.badamibagh","pb.ajmer","pb.aurangabad","pb.babina","pb.belgaum","pb.cannanore","pb.bakloh","pb.stm","pb.saugor","pb.jhansi","pb.kamptee","pb.kirkee","pb.morar","pb.ahmedabad","pb.barrackpore","pb.ranikhet","pb.nainital"});
	final List<String> allTenants = Arrays.asList(new String[]{"pb.agra","pb.delhi","pb.lucknow","pb.pune","pb.secunderabad","pb.testing"});
	final int collectionsBreakingLimit = 60;
	final int tlBreakingLimit = 60;
	final int offset = 0;
	final int limit = 50;
	
	public String migrate(SearchCriteria criteria, RequestInfoWrapper requestInfo) {
		
		migratePayments(criteria,requestInfo);
		
		migrateTLIndex(criteria, requestInfo);
		
		//createTargetCurlCalls();
		
		return "success";
	}
	public String migratePayments(SearchCriteria criteria, RequestInfoWrapper requestInfo) {

		
		for(int j=offset; j<collectionsBreakingLimit; j+=limit)
		{
			System.out.println("Fetching records from range: "+j+" - "+(j+limit));
			String collectionUrl =  "http://localhost:8090/collection-services/payments/_search?&offset="+j+"&limit="+limit;
			
			System.out.println(requestInfo);
			ResponseEntity<String> response = rest.postForEntity(collectionUrl, requestInfo, String.class);
			String responseStr = response.getBody();
			Gson gson = new Gson();
			
			//JsonObject jObj = gson.toJsonTree(responseStr).getAsJsonObject();
			
			final JsonParser parser = new JsonParser();
			String responseJson = "{\"hello\":0,\"world\":\"0\"}";
			JsonElement json = parser.parse(responseStr); 
			System.out.println(json.getAsJsonObject().get("Payments").getAsJsonArray().size());
			
			int i=0;
			if(json!=null && json.getAsJsonObject().get("Payments")!=null &&
					json.getAsJsonObject().get("Payments").getAsJsonArray()!=null)
			{
				for (JsonElement jsonElement : json.getAsJsonObject().get("Payments").getAsJsonArray()) {
					//System.out.println(jsonElement);
					JsonObject domainObject = null;
					
					String identifier = null; 
							
					if(jsonElement.getAsJsonObject().get("paymentDetails")!=null && 
							jsonElement.getAsJsonObject().get("paymentDetails").getAsJsonArray() != null)
					{
						JsonArray paymentDetails = jsonElement.getAsJsonObject().get("paymentDetails").getAsJsonArray();
						//System.out.println("Payment Details : "+paymentDetails);;
						for (JsonElement jsonElement2 : paymentDetails) {
							//System.out.println("BusinessService : "+jsonElement2.getAsJsonObject().get("businessService").getAsString());
	
							if(jsonElement2.getAsJsonObject().get("bill") != null) 
							{		
								if(jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("consumerCode") != null
										&& jsonElement2.getAsJsonObject().get("businessService").getAsString().equalsIgnoreCase("TL"))  // Build domain object only for TL
								{
									String consumerCode  =jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("consumerCode").getAsString();
									String tenantId  =jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("tenantId").getAsString();
									
									domainObject = getTLDetails(consumerCode, tenantId, requestInfo);
								}
								
								if(jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("billDetails")!=null &&
										jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("billDetails").getAsJsonArray().size()>0)
								{
									JsonObject billDetail = (JsonObject) jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("billDetails").getAsJsonArray().get(0);
									identifier = billDetail.getAsJsonObject().get("id").getAsString();
									
									if(billDetail.getAsJsonObject().get("id") != null && !billDetail.getAsJsonObject().get("id").isJsonNull())
									{
										identifier = billDetail.getAsJsonObject().get("id").getAsString();
									}
									i++;
								}
								else
								{
									System.out.println("Hey bill not there ");
								}
								
							}
						}
					}
					
					JsonObject enrichedObject = new JsonObject();
					JsonObject paymentsIndexObject = new JsonObject();
					paymentsIndexObject.add("Data", jsonElement.deepCopy().getAsJsonObject());
					
					JsonObject dataObject = transformDataObject(jsonElement);
					
					
					enrichedObject.add("dataObject", dataObject);
					enrichedObject.add("dataContext", new JsonPrimitive("collection"));
					enrichedObject.add("dataContextVersion", new JsonPrimitive("v1"));
					enrichedObject.add("identifier", new JsonPrimitive(identifier));
					
					if(domainObject != null)
					{
						enrichedObject.add("domainObject", domainObject);
						
					}
					
					putToElasticSearch( "dss-payment_v2_migration", "general", identifier, enrichedObject);
					putToElasticSearch( "paymentsindex-v1_migration", "payments", identifier, paymentsIndexObject);
					//System.out.println("Check the enriched Object: "+enrichedObject);
				}
				
			}
			
			System.out.println("Total Entities Recieved: "+json.getAsJsonObject().get("Payments").getAsJsonArray().size());
			System.out.println("Total Entities Created:  "+i);;
		}
		//System.out.println("The response is "+responseStr);
		return "success";
	}
	
	public String migrateTLIndex (SearchCriteria criteria, RequestInfoWrapper requestInfo)
	{
//		String mdmsUrl = "http://localhost:8081/egov-mdms-service/v1/_search?tenantId=pb";
//		System.out.println("Request info is "+requestInfo.getRequestInfo());
//		ResponseEntity<String> mdmsResponse = rest.postForEntity(mdmsUrl, requestInfo, String.class);
//		String mdmsResponseStr = mdmsResponse.getBody();
//		final JsonParser parser = new JsonParser();
//		JsonElement mdmsObj = parser.parse(mdmsResponseStr);
//		List<String> allTenants = new ArrayList<String>();
//		if(mdmsObj!=null && !mdmsObj.getAsJsonObject().get("MdmsRes").isJsonNull() &&
//				!mdmsObj.getAsJsonObject().get("MdmsRes").getAsJsonObject().get("tenant").isJsonNull() &&
//				!mdmsObj.getAsJsonObject().get("MdmsRes").getAsJsonObject().get("tenant").getAsJsonObject().get("tenants").getAsJsonArray().isJsonNull() &&
//				mdmsObj.getAsJsonObject().get("MdmsRes").getAsJsonObject().get("tenant").getAsJsonObject().get("tenants").getAsJsonArray().size() > 0)
//		{
//			JsonArray tenants = mdmsObj.getAsJsonObject().get("MdmsRes").getAsJsonObject().get("tenant").getAsJsonObject().get("tenants").getAsJsonArray();
//			for (JsonElement jsonElement : tenants) {
//				allTenants.add(jsonElement.getAsJsonObject().get("code").getAsString());
//			}
//		}
		
		final JsonParser parser = new JsonParser();
		System.out.println("Migrating TL Data of "+allTenants.size() + " tenants");
		int totalCounter = 0;
		for (String tenantId : allTenants) {
			
			for(int j=0; j<tlBreakingLimit; j+=limit)
			{
				System.out.println("Fetching records from range: "+j+" - "+(j+limit));
				String tlUrl = "http://localhost:8091/tl-services/v1/_search?tenantId="+tenantId+
						"&fromDate=1576468207000&toDate=1608090607000&offset="+j+"&limit="+limit;
				ResponseEntity<String> tlResponse = rest.postForEntity(tlUrl, requestInfo, String.class);
				String tlResponseStr = tlResponse.getBody();
				JsonElement tlObj = parser.parse(tlResponseStr);
				if(!tlObj.getAsJsonObject().get("Licenses").isJsonNull() &&
						!tlObj.getAsJsonObject().get("Licenses").getAsJsonArray().isJsonNull() &&
						tlObj.getAsJsonObject().get("Licenses").getAsJsonArray().size() > 0)
				{
					String identifier = null;
					JsonObject enrichedTlObject = new JsonObject();
					enrichedTlObject.add("Data", new JsonObject());
					for (JsonElement license : tlObj.getAsJsonObject().get("Licenses").getAsJsonArray()) {
						enrichedTlObject.get("Data").getAsJsonObject().add("tradelicense", license.getAsJsonObject());
						identifier = license.getAsJsonObject().get("id").getAsString();
						if(!license.getAsJsonObject().get("tradeLicenseDetail").isJsonNull() && 
								!license.getAsJsonObject().get("tradeLicenseDetail").getAsJsonObject().get("address").isJsonNull() &&
								!license.getAsJsonObject().get("tradeLicenseDetail").getAsJsonObject().get("address").getAsJsonObject().get("locality").isJsonNull())
						{
							enrichedTlObject.get("Data").getAsJsonObject().add("ward",
									license.getAsJsonObject().get("tradeLicenseDetail").getAsJsonObject().get("address").getAsJsonObject().get("locality"));
						}
						JsonObject tenantData = new JsonObject();
						tenantData.add("code", new JsonPrimitive(tenantId));
						enrichedTlObject.get("Data").getAsJsonObject().add("tenantData",tenantData);
						putToElasticSearch( "tlindex-v1_migration", "licenses", identifier+tenantId, enrichedTlObject);
						totalCounter++;

					}
					
					System.out.println("Migrated "+ tlObj.getAsJsonObject().get("Licenses").getAsJsonArray().size() +" tl records - "+tenantId);
				}
				else
				{
					System.out.println("No TL Records for "+tenantId);
				}
			}
		}
		System.out.println("Migrated "+totalCounter+" for all tenants");	
		
		return "success";
	}
	
	public String createTargetCurlCalls() {
		
		/*
		 * final JsonParser parser = new JsonParser();
		 * 
		 * String header =
		 * "[{'_index':'dss-target_v1','_type':'_doc','_id':1},{'_index':'dss-target_v1','_type':'_doc','_id':2},{'_index':'dss-target_v1','_type':'_doc','_id':3},{'_index':'dss-target_v1','_type':'_doc','_id':4},{'_index':'dss-target_v1','_type':'_doc','_id':5},{'_index':'dss-target_v1','_type':'_doc','_id':6},{'_index':'dss-target_v1','_type':'_doc','_id':7},{'_index':'dss-target_v1','_type':'_doc','_id':8},{'_index':'dss-target_v1','_type':'_doc','_id':9},{'_index':'dss-target_v1','_type':'_doc','_id':10},{'_index':'dss-target_v1','_type':'_doc','_id':11},{'_index':'dss-target_v1','_type':'_doc','_id':12},{'_index':'dss-target_v1','_type':'_doc','_id':13},{'_index':'dss-target_v1','_type':'_doc','_id':14},{'_index':'dss-target_v1','_type':'_doc','_id':15},{'_index':'dss-target_v1','_type':'_doc','_id':16},{'_index':'dss-target_v1','_type':'_doc','_id':17},{'_index':'dss-target_v1','_type':'_doc','_id':18},{'_index':'dss-target_v1','_type':'_doc','_id':19},{'_index':'dss-target_v1','_type':'_doc','_id':20},{'_index':'dss-target_v1','_type':'_doc','_id':21},{'_index':'dss-target_v1','_type':'_doc','_id':22},{'_index':'dss-target_v1','_type':'_doc','_id':23},{'_index':'dss-target_v1','_type':'_doc','_id':24},{'_index':'dss-target_v1','_type':'_doc','_id':25},{'_index':'dss-target_v1','_type':'_doc','_id':26},{'_index':'dss-target_v1','_type':'_doc','_id':27},{'_index':'dss-target_v1','_type':'_doc','_id':28},{'_index':'dss-target_v1','_type':'_doc','_id':29},{'_index':'dss-target_v1','_type':'_doc','_id':30},{'_index':'dss-target_v1','_type':'_doc','_id':31},{'_index':'dss-target_v1','_type':'_doc','_id':32},{'_index':'dss-target_v1','_type':'_doc','_id':33},{'_index':'dss-target_v1','_type':'_doc','_id':34},{'_index':'dss-target_v1','_type':'_doc','_id':35},{'_index':'dss-target_v1','_type':'_doc','_id':36},{'_index':'dss-target_v1','_type':'_doc','_id':37},{'_index':'dss-target_v1','_type':'_doc','_id':38},{'_index':'dss-target_v1','_type':'_doc','_id':39},{'_index':'dss-target_v1','_type':'_doc','_id':40},{'_index':'dss-target_v1','_type':'_doc','_id':41},{'_index':'dss-target_v1','_type':'_doc','_id':42},{'_index':'dss-target_v1','_type':'_doc','_id':43},{'_index':'dss-target_v1','_type':'_doc','_id':44},{'_index':'dss-target_v1','_type':'_doc','_id':45},{'_index':'dss-target_v1','_type':'_doc','_id':46},{'_index':'dss-target_v1','_type':'_doc','_id':47},{'_index':'dss-target_v1','_type':'_doc','_id':48},{'_index':'dss-target_v1','_type':'_doc','_id':49},{'_index':'dss-target_v1','_type':'_doc','_id':50},{'_index':'dss-target_v1','_type':'_doc','_id':51},{'_index':'dss-target_v1','_type':'_doc','_id':52},{'_index':'dss-target_v1','_type':'_doc','_id':53},{'_index':'dss-target_v1','_type':'_doc','_id':54},{'_index':'dss-target_v1','_type':'_doc','_id':55},{'_index':'dss-target_v1','_type':'_doc','_id':56},{'_index':'dss-target_v1','_type':'_doc','_id':57},{'_index':'dss-target_v1','_type':'_doc','_id':58},{'_index':'dss-target_v1','_type':'_doc','_id':59},{'_index':'dss-target_v1','_type':'_doc','_id':60},{'_index':'dss-target_v1','_type':'_doc','_id':61},{'_index':'dss-target_v1','_type':'_doc','_id':62},{'_index':'dss-target_v1','_type':'_doc','_id':63},{'_index':'dss-target_v1','_type':'_doc','_id':64},{'_index':'dss-target_v1','_type':'_doc','_id':65},{'_index':'dss-target_v1','_type':'_doc','_id':66},{'_index':'dss-target_v1','_type':'_doc','_id':67},{'_index':'dss-target_v1','_type':'_doc','_id':68},{'_index':'dss-target_v1','_type':'_doc','_id':69},{'_index':'dss-target_v1','_type':'_doc','_id':70},{'_index':'dss-target_v1','_type':'_doc','_id':71},{'_index':'dss-target_v1','_type':'_doc','_id':72},{'_index':'dss-target_v1','_type':'_doc','_id':73},{'_index':'dss-target_v1','_type':'_doc','_id':74},{'_index':'dss-target_v1','_type':'_doc','_id':75},{'_index':'dss-target_v1','_type':'_doc','_id':76},{'_index':'dss-target_v1','_type':'_doc','_id':77},{'_index':'dss-target_v1','_type':'_doc','_id':78},{'_index':'dss-target_v1','_type':'_doc','_id':79},{'_index':'dss-target_v1','_type':'_doc','_id':80},{'_index':'dss-target_v1','_type':'_doc','_id':81},{'_index':'dss-target_v1','_type':'_doc','_id':82},{'_index':'dss-target_v1','_type':'_doc','_id':83},{'_index':'dss-target_v1','_type':'_doc','_id':84},{'_index':'dss-target_v1','_type':'_doc','_id':85},{'_index':'dss-target_v1','_type':'_doc','_id':86},{'_index':'dss-target_v1','_type':'_doc','_id':87},{'_index':'dss-target_v1','_type':'_doc','_id':88},{'_index':'dss-target_v1','_type':'_doc','_id':89},{'_index':'dss-target_v1','_type':'_doc','_id':90},{'_index':'dss-target_v1','_type':'_doc','_id':91},{'_index':'dss-target_v1','_type':'_doc','_id':92},{'_index':'dss-target_v1','_type':'_doc','_id':93},{'_index':'dss-target_v1','_type':'_doc','_id':94},{'_index':'dss-target_v1','_type':'_doc','_id':95},{'_index':'dss-target_v1','_type':'_doc','_id':96},{'_index':'dss-target_v1','_type':'_doc','_id':97},{'_index':'dss-target_v1','_type':'_doc','_id':98},{'_index':'dss-target_v1','_type':'_doc','_id':99},{'_index':'dss-target_v1','_type':'_doc','_id':100},{'_index':'dss-target_v1','_type':'_doc','_id':101},{'_index':'dss-target_v1','_type':'_doc','_id':102},{'_index':'dss-target_v1','_type':'_doc','_id':103},{'_index':'dss-target_v1','_type':'_doc','_id':104},{'_index':'dss-target_v1','_type':'_doc','_id':105},{'_index':'dss-target_v1','_type':'_doc','_id':106},{'_index':'dss-target_v1','_type':'_doc','_id':107},{'_index':'dss-target_v1','_type':'_doc','_id':108},{'_index':'dss-target_v1','_type':'_doc','_id':109},{'_index':'dss-target_v1','_type':'_doc','_id':110},{'_index':'dss-target_v1','_type':'_doc','_id':111},{'_index':'dss-target_v1','_type':'_doc','_id':112},{'_index':'dss-target_v1','_type':'_doc','_id':113},{'_index':'dss-target_v1','_type':'_doc','_id':114},{'_index':'dss-target_v1','_type':'_doc','_id':115},{'_index':'dss-target_v1','_type':'_doc','_id':116},{'_index':'dss-target_v1','_type':'_doc','_id':117},{'_index':'dss-target_v1','_type':'_doc','_id':118},{'_index':'dss-target_v1','_type':'_doc','_id':119},{'_index':'dss-target_v1','_type':'_doc','_id':120},{'_index':'dss-target_v1','_type':'_doc','_id':121},{'_index':'dss-target_v1','_type':'_doc','_id':122},{'_index':'dss-target_v1','_type':'_doc','_id':123},{'_index':'dss-target_v1','_type':'_doc','_id':124},{'_index':'dss-target_v1','_type':'_doc','_id':125},{'_index':'dss-target_v1','_type':'_doc','_id':126},{'_index':'dss-target_v1','_type':'_doc','_id':127},{'_index':'dss-target_v1','_type':'_doc','_id':128},{'_index':'dss-target_v1','_type':'_doc','_id':129},{'_index':'dss-target_v1','_type':'_doc','_id':130},{'_index':'dss-target_v1','_type':'_doc','_id':131},{'_index':'dss-target_v1','_type':'_doc','_id':132},{'_index':'dss-target_v1','_type':'_doc','_id':133},{'_index':'dss-target_v1','_type':'_doc','_id':134},{'_index':'dss-target_v1','_type':'_doc','_id':135},{'_index':'dss-target_v1','_type':'_doc','_id':136},{'_index':'dss-target_v1','_type':'_doc','_id':137},{'_index':'dss-target_v1','_type':'_doc','_id':138},{'_index':'dss-target_v1','_type':'_doc','_id':139},{'_index':'dss-target_v1','_type':'_doc','_id':140},{'_index':'dss-target_v1','_type':'_doc','_id':141},{'_index':'dss-target_v1','_type':'_doc','_id':142},{'_index':'dss-target_v1','_type':'_doc','_id':143},{'_index':'dss-target_v1','_type':'_doc','_id':144},{'_index':'dss-target_v1','_type':'_doc','_id':145},{'_index':'dss-target_v1','_type':'_doc','_id':146},{'_index':'dss-target_v1','_type':'_doc','_id':147},{'_index':'dss-target_v1','_type':'_doc','_id':148},{'_index':'dss-target_v1','_type':'_doc','_id':149},{'_index':'dss-target_v1','_type':'_doc','_id':150},{'_index':'dss-target_v1','_type':'_doc','_id':151},{'_index':'dss-target_v1','_type':'_doc','_id':152},{'_index':'dss-target_v1','_type':'_doc','_id':153},{'_index':'dss-target_v1','_type':'_doc','_id':154},{'_index':'dss-target_v1','_type':'_doc','_id':155},{'_index':'dss-target_v1','_type':'_doc','_id':156},{'_index':'dss-target_v1','_type':'_doc','_id':157},{'_index':'dss-target_v1','_type':'_doc','_id':158},{'_index':'dss-target_v1','_type':'_doc','_id':159},{'_index':'dss-target_v1','_type':'_doc','_id':160},{'_index':'dss-target_v1','_type':'_doc','_id':161},{'_index':'dss-target_v1','_type':'_doc','_id':162},{'_index':'dss-target_v1','_type':'_doc','_id':163},{'_index':'dss-target_v1','_type':'_doc','_id':164},{'_index':'dss-target_v1','_type':'_doc','_id':165},{'_index':'dss-target_v1','_type':'_doc','_id':166},{'_index':'dss-target_v1','_type':'_doc','_id':167},{'_index':'dss-target_v1','_type':'_doc','_id':168},{'_index':'dss-target_v1','_type':'_doc','_id':169},{'_index':'dss-target_v1','_type':'_doc','_id':170},{'_index':'dss-target_v1','_type':'_doc','_id':171},{'_index':'dss-target_v1','_type':'_doc','_id':172},{'_index':'dss-target_v1','_type':'_doc','_id':173},{'_index':'dss-target_v1','_type':'_doc','_id':174},{'_index':'dss-target_v1','_type':'_doc','_id':175},{'_index':'dss-target_v1','_type':'_doc','_id':176},{'_index':'dss-target_v1','_type':'_doc','_id':177},{'_index':'dss-target_v1','_type':'_doc','_id':178},{'_index':'dss-target_v1','_type':'_doc','_id':179},{'_index':'dss-target_v1','_type':'_doc','_id':180},{'_index':'dss-target_v1','_type':'_doc','_id':181},{'_index':'dss-target_v1','_type':'_doc','_id':182},{'_index':'dss-target_v1','_type':'_doc','_id':183},{'_index':'dss-target_v1','_type':'_doc','_id':184},{'_index':'dss-target_v1','_type':'_doc','_id':185},{'_index':'dss-target_v1','_type':'_doc','_id':186},{'_index':'dss-target_v1','_type':'_doc','_id':187},{'_index':'dss-target_v1','_type':'_doc','_id':188},{'_index':'dss-target_v1','_type':'_doc','_id':189},{'_index':'dss-target_v1','_type':'_doc','_id':190},{'_index':'dss-target_v1','_type':'_doc','_id':191},{'_index':'dss-target_v1','_type':'_doc','_id':192},{'_index':'dss-target_v1','_type':'_doc','_id':193},{'_index':'dss-target_v1','_type':'_doc','_id':194},{'_index':'dss-target_v1','_type':'_doc','_id':195},{'_index':'dss-target_v1','_type':'_doc','_id':196},{'_index':'dss-target_v1','_type':'_doc','_id':197},{'_index':'dss-target_v1','_type':'_doc','_id':198},{'_index':'dss-target_v1','_type':'_doc','_id':199},{'_index':'dss-target_v1','_type':'_doc','_id':200},{'_index':'dss-target_v1','_type':'_doc','_id':201},{'_index':'dss-target_v1','_type':'_doc','_id':202},{'_index':'dss-target_v1','_type':'_doc','_id':203},{'_index':'dss-target_v1','_type':'_doc','_id':204},{'_index':'dss-target_v1','_type':'_doc','_id':205},{'_index':'dss-target_v1','_type':'_doc','_id':206},{'_index':'dss-target_v1','_type':'_doc','_id':207},{'_index':'dss-target_v1','_type':'_doc','_id':208},{'_index':'dss-target_v1','_type':'_doc','_id':209},{'_index':'dss-target_v1','_type':'_doc','_id':210},{'_index':'dss-target_v1','_type':'_doc','_id':211},{'_index':'dss-target_v1','_type':'_doc','_id':212},{'_index':'dss-target_v1','_type':'_doc','_id':213},{'_index':'dss-target_v1','_type':'_doc','_id':214},{'_index':'dss-target_v1','_type':'_doc','_id':215},{'_index':'dss-target_v1','_type':'_doc','_id':216},{'_index':'dss-target_v1','_type':'_doc','_id':217},{'_index':'dss-target_v1','_type':'_doc','_id':218},{'_index':'dss-target_v1','_type':'_doc','_id':219},{'_index':'dss-target_v1','_type':'_doc','_id':220},{'_index':'dss-target_v1','_type':'_doc','_id':221},{'_index':'dss-target_v1','_type':'_doc','_id':222},{'_index':'dss-target_v1','_type':'_doc','_id':223},{'_index':'dss-target_v1','_type':'_doc','_id':224},{'_index':'dss-target_v1','_type':'_doc','_id':225},{'_index':'dss-target_v1','_type':'_doc','_id':226},{'_index':'dss-target_v1','_type':'_doc','_id':227},{'_index':'dss-target_v1','_type':'_doc','_id':228},{'_index':'dss-target_v1','_type':'_doc','_id':229},{'_index':'dss-target_v1','_type':'_doc','_id':230},{'_index':'dss-target_v1','_type':'_doc','_id':231},{'_index':'dss-target_v1','_type':'_doc','_id':232},{'_index':'dss-target_v1','_type':'_doc','_id':233},{'_index':'dss-target_v1','_type':'_doc','_id':234},{'_index':'dss-target_v1','_type':'_doc','_id':235},{'_index':'dss-target_v1','_type':'_doc','_id':236},{'_index':'dss-target_v1','_type':'_doc','_id':237},{'_index':'dss-target_v1','_type':'_doc','_id':238},{'_index':'dss-target_v1','_type':'_doc','_id':239},{'_index':'dss-target_v1','_type':'_doc','_id':240},{'_index':'dss-target_v1','_type':'_doc','_id':241},{'_index':'dss-target_v1','_type':'_doc','_id':242},{'_index':'dss-target_v1','_type':'_doc','_id':243},{'_index':'dss-target_v1','_type':'_doc','_id':244},{'_index':'dss-target_v1','_type':'_doc','_id':245},{'_index':'dss-target_v1','_type':'_doc','_id':246},{'_index':'dss-target_v1','_type':'_doc','_id':247},{'_index':'dss-target_v1','_type':'_doc','_id':248},{'_index':'dss-target_v1','_type':'_doc','_id':249},{'_index':'dss-target_v1','_type':'_doc','_id':250},{'_index':'dss-target_v1','_type':'_doc','_id':251},{'_index':'dss-target_v1','_type':'_doc','_id':252}]";
		 * JsonElement headerJson = parser.parse(header);
		 * 
		 * String target =
		 * "[{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':1,'ulbName':'Agra','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.agra','id':'0001'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':2,'ulbName':'Delhi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.delhi','id':'0002'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':3,'ulbName':'Lucknow','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lucknow','id':'0003'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':4,'ulbName':'Pune','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.pune','id':'0004'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':5,'ulbName':'Secunderabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.secunderabad','id':'0005'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':6,'ulbName':'Testing','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.testing','id':'0006'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':7,'ulbName':'Ambala','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ambala','id':'0007'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':8,'ulbName':'Bareilly','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.bareilly','id':'0008'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':9,'ulbName':'Mathura','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.mathura','id':'0009'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':10,'ulbName':'Allahabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.allahabad','id':'0010'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':11,'ulbName':'Meerut','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.meerut','id':'0011'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':12,'ulbName':'Mhow','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.mhow','id':'0012'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':13,'ulbName':'Kasauli','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kasauli','id':'0013'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':14,'ulbName':'Lebong','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lebong','id':'0014'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':15,'ulbName':'Jammu','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jammu','id':'0015'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':16,'ulbName':'Jalandhar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jalandhar','id':'0016'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':17,'ulbName':'Danapur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.danapur','id':'0017'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':18,'ulbName':'Dagshai','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dagshai','id':'0018'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':19,'ulbName':'Roorkee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.roorkee','id':'0019'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':20,'ulbName':'Pachmarhi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.pachmarhi','id':'0020'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':21,'ulbName':'Nasirabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.nasirabad','id':'0021'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':22,'ulbName':'Deolali','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.deolali','id':'0022'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':23,'ulbName':'Dehuroad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dehuroad','id':'0023'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':24,'ulbName':'Ahmednagar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ahmednagar','id':'0024'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':25,'ulbName':'Amritsar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.amritsar','id':'0025'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':26,'ulbName':'Ramgarh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ramgarh','id':'0026'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':27,'ulbName':'Jalapahar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jalapahar','id':'0027'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':28,'ulbName':'Wellington','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.wellington','id':'0028'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':29,'ulbName':'Subathu','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.subathu','id':'0029'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':30,'ulbName':'Almora','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.almora','id':'0030'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':31,'ulbName':'Chakrata','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.chakrata','id':'0031'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':32,'ulbName':'ClementTown','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.clementtown','id':'0032'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':33,'ulbName':'Dehradun','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dehradun','id':'0033'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':34,'ulbName':'Faizabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.faizabad','id':'0034'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':35,'ulbName':'Fatehgarh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.fatehgarh','id':'0035'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':36,'ulbName':'Jabalpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jabalpur','id':'0036'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':37,'ulbName':'Kanpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kanpur','id':'0037'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':38,'ulbName':'Landour','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.landour','id':'0038'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':39,'ulbName':'Lansdowne','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lansdowne','id':'0039'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':40,'ulbName':'Khasyol','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.khasyol','id':'0040'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':41,'ulbName':'Jutogh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jutogh','id':'0041'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':42,'ulbName':'Shahjahanpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.shahjahanpur','id':'0042'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':43,'ulbName':'Varanasi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.varanasi','id':'0043'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':44,'ulbName':'Ferozepur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ferozepur','id':'0044'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':45,'ulbName':'Dalhousie','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dalhousie','id':'0045'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':46,'ulbName':'Shillong','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.shillong','id':'0046'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':47,'ulbName':'Badamibagh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.badamibagh','id':'0047'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':48,'ulbName':'Ajmer','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ajmer','id':'0048'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':49,'ulbName':'Aurangabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.aurangabad','id':'0049'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':50,'ulbName':'Babina','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.babina','id':'0050'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':51,'ulbName':'Belgaum','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.belgaum','id':'0051'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':52,'ulbName':'Cannanore','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.cannanore','id':'0052'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':53,'ulbName':'Bakloh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.bakloh','id':'0053'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':54,'ulbName':'stm','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.stm','id':'0054'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':55,'ulbName':'Saugor','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.saugor','id':'0055'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':56,'ulbName':'Jhansi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jhansi','id':'0056'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':57,'ulbName':'Kamptee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kamptee','id':'0057'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':58,'ulbName':'Kirkee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kirkee','id':'0058'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':59,'ulbName':'Morar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.morar','id':'0059'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':60,'ulbName':'Ahmedabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ahmedabad','id':'0060'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':61,'ulbName':'Barrackpore','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.barrackpore','id':'0061'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':62,'ulbName':'Ranikhet','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ranikhet','id':'0062'},{'financialYear':'2020-2021','businessService':'TL','snoForMunicipalCorporation':63,'ulbName':'Nainital','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.nainital','id':'0063'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':64,'ulbName':'Agra','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.agra','id':'0064'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':65,'ulbName':'Delhi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.delhi','id':'0065'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':66,'ulbName':'Lucknow','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lucknow','id':'0066'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':67,'ulbName':'Pune','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.pune','id':'0067'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':68,'ulbName':'Secunderabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.secunderabad','id':'0068'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':69,'ulbName':'Testing','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.testing','id':'0069'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':70,'ulbName':'Ambala','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ambala','id':'0070'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':71,'ulbName':'Bareilly','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.bareilly','id':'0071'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':72,'ulbName':'Mathura','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.mathura','id':'0072'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':73,'ulbName':'Allahabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.allahabad','id':'0073'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':74,'ulbName':'Meerut','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.meerut','id':'0074'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':75,'ulbName':'Mhow','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.mhow','id':'0075'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':76,'ulbName':'Kasauli','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kasauli','id':'0076'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':77,'ulbName':'Lebong','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lebong','id':'0077'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':78,'ulbName':'Jammu','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jammu','id':'0078'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':79,'ulbName':'Jalandhar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jalandhar','id':'0079'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':80,'ulbName':'Danapur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.danapur','id':'0080'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':81,'ulbName':'Dagshai','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dagshai','id':'0081'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':82,'ulbName':'Roorkee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.roorkee','id':'0082'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':83,'ulbName':'Pachmarhi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.pachmarhi','id':'0083'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':84,'ulbName':'Nasirabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.nasirabad','id':'0084'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':85,'ulbName':'Deolali','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.deolali','id':'0085'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':86,'ulbName':'Dehuroad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dehuroad','id':'0086'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':87,'ulbName':'Ahmednagar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ahmednagar','id':'0087'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':88,'ulbName':'Amritsar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.amritsar','id':'0088'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':89,'ulbName':'Ramgarh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ramgarh','id':'0089'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':90,'ulbName':'Jalapahar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jalapahar','id':'0090'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':91,'ulbName':'Wellington','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.wellington','id':'0091'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':92,'ulbName':'Subathu','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.subathu','id':'0092'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':93,'ulbName':'Almora','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.almora','id':'0093'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':94,'ulbName':'Chakrata','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.chakrata','id':'0094'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':95,'ulbName':'ClementTown','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.clementtown','id':'0095'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':96,'ulbName':'Dehradun','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dehradun','id':'0096'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':97,'ulbName':'Faizabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.faizabad','id':'0097'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':98,'ulbName':'Fatehgarh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.fatehgarh','id':'0098'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':99,'ulbName':'Jabalpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jabalpur','id':'0099'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':100,'ulbName':'Kanpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kanpur','id':'0100'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':101,'ulbName':'Landour','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.landour','id':'0101'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':102,'ulbName':'Lansdowne','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lansdowne','id':'0102'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':103,'ulbName':'Khasyol','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.khasyol','id':'0103'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':104,'ulbName':'Jutogh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jutogh','id':'0104'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':105,'ulbName':'Shahjahanpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.shahjahanpur','id':'0105'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':106,'ulbName':'Varanasi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.varanasi','id':'0106'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':107,'ulbName':'Ferozepur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ferozepur','id':'0107'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':108,'ulbName':'Dalhousie','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dalhousie','id':'0108'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':109,'ulbName':'Shillong','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.shillong','id':'0109'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':110,'ulbName':'Badamibagh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.badamibagh','id':'0110'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':111,'ulbName':'Ajmer','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ajmer','id':'0111'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':112,'ulbName':'Aurangabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.aurangabad','id':'0112'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':113,'ulbName':'Babina','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.babina','id':'0113'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':114,'ulbName':'Belgaum','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.belgaum','id':'0114'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':115,'ulbName':'Cannanore','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.cannanore','id':'0115'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':116,'ulbName':'Bakloh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.bakloh','id':'0116'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':117,'ulbName':'stm','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.stm','id':'0117'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':118,'ulbName':'Saugor','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.saugor','id':'0118'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':119,'ulbName':'Jhansi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jhansi','id':'0119'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':120,'ulbName':'Kamptee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kamptee','id':'0120'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':121,'ulbName':'Kirkee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kirkee','id':'0121'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':122,'ulbName':'Morar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.morar','id':'0122'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':123,'ulbName':'Ahmedabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ahmedabad','id':'0123'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':124,'ulbName':'Barrackpore','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.barrackpore','id':'0124'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':125,'ulbName':'Ranikhet','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ranikhet','id':'0125'},{'financialYear':'2020-2021','businessService':'MC','snoForMunicipalCorporation':126,'ulbName':'Nainital','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.nainital','id':'0126'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':127,'ulbName':'Agra','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.agra','id':'0127'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':128,'ulbName':'Delhi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.delhi','id':'0128'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':129,'ulbName':'Lucknow','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lucknow','id':'0129'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':130,'ulbName':'Pune','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.pune','id':'0130'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':131,'ulbName':'Secunderabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.secunderabad','id':'0131'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':132,'ulbName':'Testing','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.testing','id':'0132'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':133,'ulbName':'Ambala','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ambala','id':'0133'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':134,'ulbName':'Bareilly','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.bareilly','id':'0134'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':135,'ulbName':'Mathura','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.mathura','id':'0135'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':136,'ulbName':'Allahabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.allahabad','id':'0136'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':137,'ulbName':'Meerut','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.meerut','id':'0137'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':138,'ulbName':'Mhow','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.mhow','id':'0138'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':139,'ulbName':'Kasauli','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kasauli','id':'0139'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':140,'ulbName':'Lebong','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lebong','id':'0140'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':141,'ulbName':'Jammu','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jammu','id':'0141'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':142,'ulbName':'Jalandhar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jalandhar','id':'0142'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':143,'ulbName':'Danapur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.danapur','id':'0143'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':144,'ulbName':'Dagshai','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dagshai','id':'0144'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':145,'ulbName':'Roorkee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.roorkee','id':'0145'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':146,'ulbName':'Pachmarhi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.pachmarhi','id':'0146'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':147,'ulbName':'Nasirabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.nasirabad','id':'0147'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':148,'ulbName':'Deolali','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.deolali','id':'0148'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':149,'ulbName':'Dehuroad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dehuroad','id':'0149'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':150,'ulbName':'Ahmednagar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ahmednagar','id':'0150'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':151,'ulbName':'Amritsar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.amritsar','id':'0151'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':152,'ulbName':'Ramgarh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ramgarh','id':'0152'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':153,'ulbName':'Jalapahar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jalapahar','id':'0153'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':154,'ulbName':'Wellington','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.wellington','id':'0154'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':155,'ulbName':'Subathu','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.subathu','id':'0155'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':156,'ulbName':'Almora','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.almora','id':'0156'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':157,'ulbName':'Chakrata','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.chakrata','id':'0157'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':158,'ulbName':'ClementTown','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.clementtown','id':'0158'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':159,'ulbName':'Dehradun','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dehradun','id':'0159'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':160,'ulbName':'Faizabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.faizabad','id':'0160'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':161,'ulbName':'Fatehgarh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.fatehgarh','id':'0161'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':162,'ulbName':'Jabalpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jabalpur','id':'0162'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':163,'ulbName':'Kanpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kanpur','id':'0163'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':164,'ulbName':'Landour','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.landour','id':'0164'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':165,'ulbName':'Lansdowne','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lansdowne','id':'0165'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':166,'ulbName':'Khasyol','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.khasyol','id':'0166'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':167,'ulbName':'Jutogh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jutogh','id':'0167'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':168,'ulbName':'Shahjahanpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.shahjahanpur','id':'0168'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':169,'ulbName':'Varanasi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.varanasi','id':'0169'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':170,'ulbName':'Ferozepur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ferozepur','id':'0170'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':171,'ulbName':'Dalhousie','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dalhousie','id':'0171'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':172,'ulbName':'Shillong','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.shillong','id':'0172'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':173,'ulbName':'Badamibagh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.badamibagh','id':'0173'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':174,'ulbName':'Ajmer','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ajmer','id':'0174'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':175,'ulbName':'Aurangabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.aurangabad','id':'0175'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':176,'ulbName':'Babina','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.babina','id':'0176'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':177,'ulbName':'Belgaum','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.belgaum','id':'0177'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':178,'ulbName':'Cannanore','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.cannanore','id':'0178'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':179,'ulbName':'Bakloh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.bakloh','id':'0179'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':180,'ulbName':'stm','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.stm','id':'0180'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':181,'ulbName':'Saugor','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.saugor','id':'0181'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':182,'ulbName':'Jhansi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jhansi','id':'0182'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':183,'ulbName':'Kamptee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kamptee','id':'0183'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':184,'ulbName':'Kirkee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kirkee','id':'0184'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':185,'ulbName':'Morar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.morar','id':'0185'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':186,'ulbName':'Ahmedabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ahmedabad','id':'0186'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':187,'ulbName':'Barrackpore','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.barrackpore','id':'0187'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':188,'ulbName':'Ranikhet','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ranikhet','id':'0188'},{'financialYear':'2020-2021','businessService':'WS','snoForMunicipalCorporation':189,'ulbName':'Nainital','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.nainital','id':'0189'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':190,'ulbName':'Agra','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.agra','id':'0190'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':191,'ulbName':'Delhi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.delhi','id':'0191'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':192,'ulbName':'Lucknow','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lucknow','id':'0192'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':193,'ulbName':'Pune','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.pune','id':'0193'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':194,'ulbName':'Secunderabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.secunderabad','id':'0194'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':195,'ulbName':'Testing','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.testing','id':'0195'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':196,'ulbName':'Ambala','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ambala','id':'0196'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':197,'ulbName':'Bareilly','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.bareilly','id':'0197'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':198,'ulbName':'Mathura','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.mathura','id':'0198'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':199,'ulbName':'Allahabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.allahabad','id':'0199'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':200,'ulbName':'Meerut','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.meerut','id':'0200'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':201,'ulbName':'Mhow','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.mhow','id':'0201'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':202,'ulbName':'Kasauli','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kasauli','id':'0202'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':203,'ulbName':'Lebong','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lebong','id':'0203'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':204,'ulbName':'Jammu','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jammu','id':'0204'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':205,'ulbName':'Jalandhar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jalandhar','id':'0205'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':206,'ulbName':'Danapur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.danapur','id':'0206'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':207,'ulbName':'Dagshai','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dagshai','id':'0207'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':208,'ulbName':'Roorkee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.roorkee','id':'0208'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':209,'ulbName':'Pachmarhi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.pachmarhi','id':'0209'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':210,'ulbName':'Nasirabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.nasirabad','id':'0210'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':211,'ulbName':'Deolali','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.deolali','id':'0211'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':212,'ulbName':'Dehuroad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dehuroad','id':'0212'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':213,'ulbName':'Ahmednagar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ahmednagar','id':'0213'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':214,'ulbName':'Amritsar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.amritsar','id':'0214'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':215,'ulbName':'Ramgarh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ramgarh','id':'0215'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':216,'ulbName':'Jalapahar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jalapahar','id':'0216'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':217,'ulbName':'Wellington','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.wellington','id':'0217'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':218,'ulbName':'Subathu','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.subathu','id':'0218'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':219,'ulbName':'Almora','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.almora','id':'0219'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':220,'ulbName':'Chakrata','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.chakrata','id':'0220'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':221,'ulbName':'ClementTown','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.clementtown','id':'0221'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':222,'ulbName':'Dehradun','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dehradun','id':'0222'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':223,'ulbName':'Faizabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.faizabad','id':'0223'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':224,'ulbName':'Fatehgarh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.fatehgarh','id':'0224'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':225,'ulbName':'Jabalpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jabalpur','id':'0225'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':226,'ulbName':'Kanpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kanpur','id':'0226'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':227,'ulbName':'Landour','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.landour','id':'0227'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':228,'ulbName':'Lansdowne','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.lansdowne','id':'0228'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':229,'ulbName':'Khasyol','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.khasyol','id':'0229'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':230,'ulbName':'Jutogh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jutogh','id':'0230'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':231,'ulbName':'Shahjahanpur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.shahjahanpur','id':'0231'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':232,'ulbName':'Varanasi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.varanasi','id':'0232'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':233,'ulbName':'Ferozepur','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ferozepur','id':'0233'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':234,'ulbName':'Dalhousie','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.dalhousie','id':'0234'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':235,'ulbName':'Shillong','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.shillong','id':'0235'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':236,'ulbName':'Badamibagh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.badamibagh','id':'0236'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':237,'ulbName':'Ajmer','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ajmer','id':'0237'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':238,'ulbName':'Aurangabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.aurangabad','id':'0238'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':239,'ulbName':'Babina','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.babina','id':'0239'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':240,'ulbName':'Belgaum','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.belgaum','id':'0240'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':241,'ulbName':'Cannanore','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.cannanore','id':'0241'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':242,'ulbName':'Bakloh','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.bakloh','id':'0242'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':243,'ulbName':'stm','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.stm','id':'0243'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':244,'ulbName':'Saugor','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.saugor','id':'0244'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':245,'ulbName':'Jhansi','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.jhansi','id':'0245'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':246,'ulbName':'Kamptee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kamptee','id':'0246'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':247,'ulbName':'Kirkee','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.kirkee','id':'0247'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':248,'ulbName':'Morar','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.morar','id':'0248'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':249,'ulbName':'Ahmedabad','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ahmedabad','id':'0249'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':250,'ulbName':'Barrackpore','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.barrackpore','id':'0250'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':251,'ulbName':'Ranikhet','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.ranikhet','id':'0251'},{'financialYear':'2020-2021','businessService':'SW','snoForMunicipalCorporation':252,'ulbName':'Nainital','budgetProposedForMunicipalCorporation':850000,'tenantIdForMunicipalCorporation':'pb.nainital','id':'0252'}]";
		 * JsonElement targetJson = parser.parse(target);
		 * 
		 * for (int i=0; i< targetJson.getAsJsonArray().size(); i++) {
		 * System.out.println(headerJson.getAsJsonArray().get(i));
		 * System.out.println(targetJson.getAsJsonArray().get(i)); }
		 */
		return "Done";
	}
	
	public String migratePGRIndex(SearchCriteria criteria, RequestInfoWrapper requestInfo) {
		return "success";
	}
	
	public String putToElasticSearch(String indexName, String type, String identifier,  JsonObject jsonObject)
	{
		
		String url = "http://localhost:9200/"+indexName+"/"+type+"/"+identifier;
		try {
			
			
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
			headers.setContentType(MediaType.APPLICATION_JSON);
			
			HttpEntity<String> entityReq = new HttpEntity<String>(jsonObject.toString(), headers);

			RestTemplate template = new RestTemplate();

			ResponseEntity<String> response = template
			    .exchange(url, HttpMethod.POST, entityReq, String.class);
			
		
			if (response.getStatusCode() == HttpStatus.OK) {
				  System.out.println("Post success to ES : "+identifier);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return "failed";
		}
		return "success";
	}
	
	public JsonObject transformDataObject(JsonElement jsonElement)
	{
		//Make billDetails array as object
		JsonObject transObj = jsonElement.deepCopy().getAsJsonObject();
		if(transObj.get("paymentDetails")!=null && 
				transObj.get("paymentDetails").getAsJsonArray() != null)
		{
			JsonArray paymentDetails = transObj.get("paymentDetails").getAsJsonArray();
			//System.out.println("Payment Details : "+paymentDetails);;
			for (JsonElement jsonElement2 : paymentDetails) {
				//System.out.println("BusinessService : "+jsonElement2.getAsJsonObject().get("businessService").getAsString());
				if(jsonElement2.getAsJsonObject().get("bill") != null && 
						jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("billDetails")!=null &&
							jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("billDetails").getAsJsonArray().size()>0)
				{
					JsonObject billDetail = (JsonObject) jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().get("billDetails").getAsJsonArray().get(0);
					JsonObject newObject = billDetail.deepCopy();
					jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().remove("billDetails");
					jsonElement2.getAsJsonObject().get("bill").getAsJsonObject().add("billDetails", newObject);
				}
			}
		}
		return transObj;
	}
	
	public JsonObject getTLDetails(String applicationNumber, String tenantId , RequestInfoWrapper requestInfo)
	{
		String url = "http://localhost:8091/tl-services/v1/_search?tenantId="+tenantId+"&applicationNumber="+applicationNumber;
		ResponseEntity<String> response = rest.postForEntity(url, requestInfo, String.class);
		String responseStr = response.getBody();
		final JsonParser parser = new JsonParser();
		JsonElement json = parser.parse(responseStr); 
		
		JsonObject domainObject = new JsonObject();
		JsonObject tradeLicense = new JsonObject();
		domainObject.add("tradeLicense", tradeLicense);
		if(json.getAsJsonObject().get("Licenses") !=null && json.getAsJsonObject().get("Licenses").getAsJsonArray().size() > 0)
		{
			JsonObject jsonObject = json.getAsJsonObject().get("Licenses").getAsJsonArray().get(0).getAsJsonObject();
			if(jsonObject.get("tradeLicenseDetail")!= null )
			{
				if(jsonObject.get("tradeLicenseDetail").getAsJsonObject().get("address")!=null &&
						jsonObject.get("tradeLicenseDetail").getAsJsonObject().get("address").getAsJsonObject().get("locality")!=null)
				{
					JsonObject ward = jsonObject.get("tradeLicenseDetail").getAsJsonObject().get("address").getAsJsonObject().get("locality").getAsJsonObject();
					domainObject.add("ward", ward);
					
				}
				
				if(jsonObject.get("tradeLicenseDetail").getAsJsonObject().get("tradeUnits") !=null)
				{
					JsonArray tradeUnits = jsonObject.get("tradeLicenseDetail").getAsJsonObject().get("tradeUnits").getAsJsonArray();
					domainObject.get("tradeLicense").getAsJsonObject().add("tradeUnits", tradeUnits);
				}
					
			}
			
			if(jsonObject.get("licenseNumber") != null && !jsonObject.get("licenseNumber").isJsonNull())
			{
				String licenseNumber = jsonObject.get("licenseNumber").getAsString();
				domainObject.get("tradeLicense").getAsJsonObject().add("licenseNumber", new JsonPrimitive(licenseNumber));
			}
			
		}
		
		return domainObject;
		
	}
	
	public JsonObject enirch(JsonObject dataObject, JsonObject domainObject)
	{
		return null;
	}
	
}
