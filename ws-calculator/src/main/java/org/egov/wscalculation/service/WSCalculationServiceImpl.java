package org.egov.wscalculation.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.tracer.model.CustomException;
import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.producer.WSCalculationProducer;
import org.egov.wscalculation.repository.ServiceRequestRepository;
import org.egov.wscalculation.repository.WSCalculationDao;
import org.egov.wscalculation.util.CalculatorUtil;
import org.egov.wscalculation.util.WSCalculationUtil;
import org.egov.wscalculation.web.models.AdhocTaxReq;
import org.egov.wscalculation.web.models.BillEstimation;
import org.egov.wscalculation.web.models.BillFailureNotificationObj;
import org.egov.wscalculation.web.models.BillFailureNotificationRequest;
import org.egov.wscalculation.web.models.BillingSlab;
import org.egov.wscalculation.web.models.Calculation;
import org.egov.wscalculation.web.models.CalculationCriteria;
import org.egov.wscalculation.web.models.CalculationReq;
import org.egov.wscalculation.web.models.Demand;
import org.egov.wscalculation.web.models.Property;
import org.egov.wscalculation.web.models.Slab;
import org.egov.wscalculation.web.models.TaxHeadCategory;
import org.egov.wscalculation.web.models.TaxHeadEstimate;
import org.egov.wscalculation.web.models.TaxHeadMaster;
import org.egov.wscalculation.web.models.WaterConnection;
import org.egov.wscalculation.web.models.WaterConnectionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Service
@Slf4j
public class WSCalculationServiceImpl implements WSCalculationService {

	@Autowired
	private PayService payService;

	@Autowired
	private EstimationService estimationService;
	
	@Autowired
	private CalculatorUtil calculatorUtil;
	
	@Autowired
	private DemandService demandService;
	
	@Autowired
	private MasterDataService masterDataService; 

	@Autowired
	private WSCalculationDao wSCalculationDao;
	
	@Autowired
	private ServiceRequestRepository repository;
	
	@Autowired
	private WSCalculationUtil wSCalculationUtil;
	
	@Autowired
	private WSCalculationProducer producer;
	
	@Autowired
	private WSCalculationConfiguration config;
	
	
	  @Autowired
	    private CalculatorUtil calculatorUtils;
	  
	   @Autowired
	    private WSCalculationProducer wsCalculationProducer;
	    

		@Autowired
		private MasterDataService mDataService;
		

	/**
	 * Get CalculationReq and Calculate the Tax Head on Water Charge And Estimation Charge
	 */
	public List<Calculation> getCalculation(CalculationReq request) {
		List<Calculation> calculations;

		Map<String, Object> masterMap;
		if (request.getIsconnectionCalculation()) {
			//Calculate and create demand for connection
			masterMap = masterDataService.loadMasterData(request.getRequestInfo(),
					request.getCalculationCriteria().get(0).getTenantId());
			calculations = getCalculations(request, masterMap);
		} else {
			//Calculate and create demand for application
			masterMap = masterDataService.loadExemptionMaster(request.getRequestInfo(),
					request.getCalculationCriteria().get(0).getTenantId());
			calculations = getFeeCalculation(request, masterMap);
		}
		demandService.generateDemand(request.getRequestInfo(), calculations, masterMap, request.getIsconnectionCalculation());
		unsetWaterConnection(calculations);
		return calculations;
	}
	
	public double getBillMonthsToCharge(Map<String, Object>startAndEndDate ) {
		    Long billingCycleEndDate =  (Long) startAndEndDate.get("endingDay");		   
		    LocalDate billingPeriodEndDate = LocalDate.ofEpochDay(billingCycleEndDate / 86400000L);		    
		    LocalDate toDay = LocalDate.now();		   
		    Period difference = Period.between(toDay, billingPeriodEndDate);
		    double monthsToCharge = difference.getMonths()+1; 		    
		    return monthsToCharge;
	}

	
	public BillEstimation getBillEstimate(CalculationReq request) {
		String tenantId = request.getCalculationCriteria().get(0).getTenantId();
		BillEstimation billEstimation = new BillEstimation();
		
		Map<String, Object> billingMasterData = new HashMap<String, Object>();
		
		if(request.getCalculationCriteria().get(0).getWaterConnection().getConnectionType().equalsIgnoreCase(WSCalculationConstant.meteredConnectionType)) {
			 billingMasterData = calculatorUtils.loadBillingFrequencyMasterDataMeterConnection(request.getRequestInfo(), tenantId);	
			
		}
		else {
		
		    billingMasterData = calculatorUtils.loadBillingFrequencyMasterData(request.getRequestInfo(), tenantId);	
		}
		
		
		
		if(billingMasterData!=null) {
				String billingCycle = billingMasterData.get("billingCycle").toString();
				String assessmentYear = estimationService.getAssessmentYear();
		
				CalculationCriteria calculationCriteria = CalculationCriteria.builder().tenantId(tenantId).assessmentYear(assessmentYear).build();
				List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
				calculationCriteriaList.add(calculationCriteria);
				CalculationReq calculationReq = CalculationReq.builder().calculationCriteria(calculationCriteriaList)
						.requestInfo(request.getRequestInfo()).isconnectionCalculation(true).build();
			
				Map<String, Object> masterMap = mDataService.loadMasterData(calculationReq.getRequestInfo(),tenantId);		
				for (CalculationCriteria criteria : request.getCalculationCriteria()) {
					
					BillingSlab billingSlab = estimationService.getEstimationMapForApplicationNo(criteria, request.getRequestInfo(),masterMap);
					double billAmountForBillingPeriod=0;
					double fianlBillAmount = 0;
					double monthsToCharge = 0;
					if(billingSlab != null) {
						billEstimation.setBillingSlab(billingSlab);
						
						Map<String, Object> billingPeriod = new HashMap<>();
						  Map<String, Object>startAndEndDate = new HashMap<String, Object>();
						  
						  if(billingSlab.getSlabs().isEmpty()) {
								
								billAmountForBillingPeriod = billingSlab.getMinimumCharge();														
							}
							else {
								
								//Only tap count condition
								if ((criteria.getWaterConnection().getConnectionType().equalsIgnoreCase(WSCalculationConstant.nonMeterdConnection) && 
												billingSlab.getCalculationAttribute().equalsIgnoreCase(WSCalculationConstant.noOfTapsConst)
												)) {
									for (Slab slab : billingSlab.getSlabs()) {
										if (criteria.getWaterConnection().getNoOfTaps() > slab.getTo()) {
											billAmountForBillingPeriod += (((slab.getTo()) - (slab.getFrom())) * slab.getCharge());
										} else if (criteria.getWaterConnection().getNoOfTaps() <= slab.getTo()) {
											billAmountForBillingPeriod += ((criteria.getWaterConnection().getNoOfTaps()-slab.getFrom()) * slab.getCharge());
											break;
										}
									}		
								}
								else if(criteria.getWaterConnection().getConnectionType().equalsIgnoreCase(WSCalculationConstant.meteredConnectionType)) {
									
									billAmountForBillingPeriod = billingSlab.getMinimumCharge();
									billEstimation.setPayableBillAmount(billAmountForBillingPeriod);
									break;
								}
								else {
									
									billAmountForBillingPeriod = billingSlab.getSlabs().get(0).getCharge();
								}
								
								
							}
						  
						switch(billingCycle) {
						case(WSCalculationConstant.Monthly_Billing_Period) :
							 
						  	//billAmountForBillingPeriod = billingSlab.getMinimumCharge();
							fianlBillAmount = billAmountForBillingPeriod + billingSlab.getMotorCharge()+billingSlab.getMaintenanceCharge();	
							
						  break;
						case(WSCalculationConstant.Quaterly_Billing_Period) :
							
						    startAndEndDate = estimationService.getQuarterStartAndEndDate(billingPeriod);
						    monthsToCharge = getBillMonthsToCharge(startAndEndDate);
						    billAmountForBillingPeriod = (billAmountForBillingPeriod/3.0)*monthsToCharge;
							fianlBillAmount = billAmountForBillingPeriod + billingSlab.getMotorCharge()+billingSlab.getMaintenanceCharge();
											
						   break;
						case(WSCalculationConstant.Yearly_Billing_Period) :
							
						     startAndEndDate = estimationService.getYearStartAndEndDate(billingPeriod);
						    monthsToCharge = getBillMonthsToCharge(startAndEndDate);
						//	billAmountForBillingPeriod = (billingSlab.getMinimumCharge()/12 )*monthsToCharge;
						    billAmountForBillingPeriod = (billAmountForBillingPeriod/12.0)*monthsToCharge;
							fianlBillAmount = billAmountForBillingPeriod + billingSlab.getMotorCharge()+billingSlab.getMaintenanceCharge();						
						
						   break;
						   
						case(WSCalculationConstant.Half_Yearly_Billing_Period) :							
						    startAndEndDate = estimationService.getHalfYearStartAndEndDate(billingPeriod);						  
						    monthsToCharge = getBillMonthsToCharge(startAndEndDate);
						    billAmountForBillingPeriod = (billAmountForBillingPeriod/6.0)*monthsToCharge;
							fianlBillAmount = billAmountForBillingPeriod + billingSlab.getMotorCharge()+billingSlab.getMaintenanceCharge();						
			
						
						    break;
						   
						case(WSCalculationConstant.Bi_Monthly_Billing_Period) :
						
						    startAndEndDate = estimationService.getBiMonthStartAndEndDate(billingPeriod);
						    monthsToCharge = getBillMonthsToCharge(startAndEndDate);
						    billAmountForBillingPeriod = (billAmountForBillingPeriod/12.0)*monthsToCharge;
							fianlBillAmount = billAmountForBillingPeriod + billingSlab.getMotorCharge()+billingSlab.getMaintenanceCharge();						
						
						
						   break;
						  default :
							  Map<String, String> errorMap = new HashMap<>();
								errorMap.put("FEE_SLAB_NOT_FOUND", "Fee slab master data not found!!");
						 
						}
						
					
						billEstimation.setBillAmount(billAmountForBillingPeriod);
						billEstimation.setPayableBillAmount(fianlBillAmount);
					}
					

				}
						
		}
		else {
			
			Map<String, String> errorMap = new HashMap<>();
			errorMap.put("FEE_SLAB_NOT_FOUND", "Fee slab master data not found!!");
		
		}
		
		
		return billEstimation;
	}
	
	/**
	 * 
	 * 
	 * @param request - Calculation Request Object
	 * @return List of calculation.
	 */
	public List<Calculation> bulkDemandGeneration(CalculationReq request, Map<String, Object> masterMap) {
		List<Calculation> calculations = getCalculations(request, masterMap);
		demandService.generateDemand(request.getRequestInfo(), calculations, masterMap, true);
		return calculations;
	}

	/**
	 * 
	 * @param request - Calculation Request Object
	 * @return list of calculation based on request
	 */
	public List<Calculation> getEstimation(CalculationReq request) {
		Map<String, Object> masterData = masterDataService.loadExemptionMaster(request.getRequestInfo(),
				request.getCalculationCriteria().get(0).getTenantId());
		List<Calculation> calculations = getFeeCalculation(request, masterData);
		unsetWaterConnection(calculations);
		return calculations;
	}
	/**
	 * It will take calculation and return calculation with tax head code 
	 * 
	 * @param requestInfo Request Info Object
	 * @param criteria Calculation criteria on meter charge
	 * @param estimatesAndBillingSlabs Billing Slabs
	 * @param masterMap Master MDMS Data
	 * @return Calculation With Tax head
	 */
	public Calculation getCalculation(RequestInfo requestInfo, CalculationCriteria criteria,
			Map<String, List> estimatesAndBillingSlabs, Map<String, Object> masterMap, boolean isConnectionFee) {

		@SuppressWarnings("unchecked")
		List<TaxHeadEstimate> estimates = estimatesAndBillingSlabs.get("estimates");
		@SuppressWarnings("unchecked")
		List<String> billingSlabIds = estimatesAndBillingSlabs.get("billingSlabIds");
		WaterConnection waterConnection = criteria.getWaterConnection();
		String tenantId = criteria.getTenantId();

		if(StringUtils.isEmpty(tenantId)) {
			Property property = wSCalculationUtil.getProperty(
				WaterConnectionRequest.builder().waterConnection(waterConnection).requestInfo(requestInfo).build());
			tenantId =   property.getTenantId();
		}
		@SuppressWarnings("unchecked")
		Map<String, TaxHeadCategory> taxHeadCategoryMap = ((List<TaxHeadMaster>) masterMap
				.get(WSCalculationConstant.TAXHEADMASTER_MASTER_KEY)).stream()
						.collect(Collectors.toMap(TaxHeadMaster::getCode, TaxHeadMaster::getCategory, (OldValue, NewValue) -> NewValue));

		BigDecimal taxAmt = BigDecimal.ZERO;
		BigDecimal waterCharge = BigDecimal.ZERO;
		BigDecimal penalty = BigDecimal.ZERO;
		BigDecimal exemption = BigDecimal.ZERO;
		BigDecimal rebate = BigDecimal.ZERO;
		BigDecimal fee = BigDecimal.ZERO;

		for (TaxHeadEstimate estimate : estimates) {

			TaxHeadCategory category = taxHeadCategoryMap.get(estimate.getTaxHeadCode());
			estimate.setCategory(category);

			switch (category) {

			case CHARGES:
				waterCharge = waterCharge.add(estimate.getEstimateAmount());
				break;

			case PENALTY:
				penalty = penalty.add(estimate.getEstimateAmount());
				break;

			case REBATE:
				rebate = rebate.add(estimate.getEstimateAmount());
				break;

			case EXEMPTION:
				exemption = exemption.add(estimate.getEstimateAmount());
				break;
			case FEE:
				fee = fee.add(estimate.getEstimateAmount());
				break;
			default:
				taxAmt = taxAmt.add(estimate.getEstimateAmount());
				break;
			}
		}
		TaxHeadEstimate decimalEstimate = payService.roundOfDecimals(taxAmt.add(penalty).add(waterCharge).add(fee),
				rebate.add(exemption), isConnectionFee);
		if (null != decimalEstimate) {
			decimalEstimate.setCategory(taxHeadCategoryMap.get(decimalEstimate.getTaxHeadCode()));
			estimates.add(decimalEstimate);
			if (decimalEstimate.getEstimateAmount().compareTo(BigDecimal.ZERO) >= 0)
				taxAmt = taxAmt.add(decimalEstimate.getEstimateAmount());
			else
				rebate = rebate.add(decimalEstimate.getEstimateAmount());
		}

		BigDecimal totalAmount = taxAmt.add(penalty).add(rebate).add(exemption).add(waterCharge).add(fee);
		return Calculation.builder().totalAmount(totalAmount).taxAmount(taxAmt).penalty(penalty).exemption(exemption)
				.charge(waterCharge).fee(fee).waterConnection(waterConnection).rebate(rebate).tenantId(tenantId)
				.taxHeadEstimates(estimates).billingSlabIds(billingSlabIds).connectionNo(criteria.getConnectionNo()).applicationNO(criteria.getApplicationNo())
				.build();
	}
	
	/**
	 * 
	 * @param request would be calculations request
	 * @param masterMap master data
	 * @return all calculations including water charge and taxhead on that
	 */
	List<Calculation> getCalculations(CalculationReq request, Map<String, Object> masterMap) {
		List<Calculation> calculations = new ArrayList<>(request.getCalculationCriteria().size());
		for (CalculationCriteria criteria : request.getCalculationCriteria()) {
			Map<String, List> estimationMap = estimationService.getEstimationMap(criteria, request.getRequestInfo(),
					masterMap);
			ArrayList<?> billingFrequencyMap = (ArrayList<?>) masterMap
					.get(WSCalculationConstant.Billing_Period_Master);
			masterDataService.enrichBillingPeriod(criteria, billingFrequencyMap, masterMap);
			Calculation calculation = getCalculation(request.getRequestInfo(), criteria, estimationMap, masterMap, true);
			calculations.add(calculation);
		}
		return calculations;
	}


	@Override
	public void jobScheduler() {
		// TODO Auto-generated method stub
		ArrayList<String> tenantIds = wSCalculationDao.searchTenantIds();

		for (String tenantId : tenantIds) {
			RequestInfo requestInfo = new RequestInfo();
			User user = new User();
			user.setTenantId(tenantId);
			requestInfo.setUserInfo(user);
			String jsonPath = WSCalculationConstant.JSONPATH_ROOT_FOR_BilingPeriod;
			MdmsCriteriaReq mdmsCriteriaReq = calculatorUtil.getBillingFrequency(requestInfo, tenantId);
			StringBuilder url = calculatorUtil.getMdmsSearchUrl();
			Object res = repository.fetchResult(url, mdmsCriteriaReq);
			if (res == null) {
				throw new CustomException("MDMS_ERROR_FOR_BILLING_FREQUENCY",
						"ERROR IN FETCHING THE BILLING FREQUENCY");
			}
			ArrayList<?> mdmsResponse = JsonPath.read(res, jsonPath);
			getBillingPeriod(mdmsResponse, requestInfo, tenantId);
		}
	}
	

	@SuppressWarnings("unchecked")
	public void getBillingPeriod(ArrayList<?> mdmsResponse, RequestInfo requestInfo, String tenantId) {
		log.info("Billing Frequency Map" + mdmsResponse.toString());
		Map<String, Object> master = (Map<String, Object>) mdmsResponse.get(0);
		LocalDateTime demandStartingDate = LocalDateTime.now();
		Long demandGenerateDateMillis = (Long) master.get(WSCalculationConstant.Demand_Generate_Date_String);

		String connectionType = "Non-metred";

		if (demandStartingDate.getDayOfMonth() == (demandGenerateDateMillis) / 86400) {

			ArrayList<String> connectionNos = wSCalculationDao.searchConnectionNos(connectionType, tenantId);
			for (String connectionNo : connectionNos) {

				CalculationReq calculationReq = new CalculationReq();
				CalculationCriteria calculationCriteria = new CalculationCriteria();
				calculationCriteria.setTenantId(tenantId);
				calculationCriteria.setConnectionNo(connectionNo);

				List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
				calculationCriteriaList.add(calculationCriteria);

				calculationReq.setRequestInfo(requestInfo);
				calculationReq.setCalculationCriteria(calculationCriteriaList);
				calculationReq.setIsconnectionCalculation(true);
				getCalculation(calculationReq);

			}
		}
	}

	/**
	 * Generate Demand Based on Time (Monthly, Quarterly, Yearly)
	 */
	public void generateDemandBasedOnTimePeriod(RequestInfo requestInfo) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime date = LocalDateTime.now();
		log.info("Time schedule start for water demand generation on : " + date.format(dateTimeFormatter));
		List<String> tenantIds = wSCalculationDao.getTenantId();
		if (tenantIds.isEmpty())
			return;
		tenantIds.forEach(tenantId -> {
			demandService.generateDemandForTenantId(tenantId, requestInfo,null,true);
		});
	}
	
	
	/**
	 * Generate Demand Manually
	 */
	public void generateDemandBasedOnTimePeriod_manual(RequestInfo requestInfo,String tenantId, List<String> connectionnos) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime date = LocalDateTime.now();
		log.info("Time schedule start for water demand generation on : " + date.format(dateTimeFormatter));
		
		demandService.generateDemandForTenantId(tenantId, requestInfo,connectionnos,false);
	}
	/**
	 * 
	 * @param request - Calculation Request Object
	 * @param masterMap - Master MDMS Data
	 * @return list of calculation based on estimation criteria
	 */
	List<Calculation> getFeeCalculation(CalculationReq request, Map<String, Object> masterMap) {
		List<Calculation> calculations = new ArrayList<>(request.getCalculationCriteria().size());
		for (CalculationCriteria criteria : request.getCalculationCriteria()) {
			Map<String, List> estimationMap = estimationService.getFeeEstimation(criteria, request.getRequestInfo(),
					masterMap);
			masterDataService.enrichBillingPeriodForFee(masterMap);
			Calculation calculation = getCalculation(request.getRequestInfo(), criteria, estimationMap, masterMap, false);
			calculations.add(calculation);
		}
		return calculations;
	}
	
	public void unsetWaterConnection(List<Calculation> calculation) {
		calculation.forEach(cal -> cal.setWaterConnection(null));
	}
	
	/**
	 * Add adhoc tax to demand
	 * @param adhocTaxReq - Adhox Tax Request Object
	 * @return List of Calculation
	 */
	public List<Calculation> applyAdhocTax(AdhocTaxReq adhocTaxReq) {
		List<TaxHeadEstimate> estimates = new ArrayList<>();
		if (!(adhocTaxReq.getAdhocpenalty().compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TIME_ADHOC_PENALTY)
					.estimateAmount(adhocTaxReq.getAdhocpenalty().setScale(2, 2)).build());
		if (!(adhocTaxReq.getAdhocrebate().compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TIME_ADHOC_REBATE)
					.estimateAmount(adhocTaxReq.getAdhocrebate().setScale(2, 2).negate()).build());
		Calculation calculation = Calculation.builder()
				.tenantId(adhocTaxReq.getRequestInfo().getUserInfo().getTenantId())
				.applicationNO(adhocTaxReq.getDemandId()).taxHeadEstimates(estimates).build();
		List<Calculation> calculations = Collections.singletonList(calculation);
		return demandService.updateDemandForAdhocTax(adhocTaxReq.getRequestInfo(), calculations);
	}


	@Override
	public void checkFailedBills(RequestInfo requestInfo,Long fromDateSearch , Long toDateSearch , String tenantId, String connectionno) {
		// TODO Auto-generated method stub
		List<Demand> demands = demandService.getDemandForFailedBills( requestInfo ,  fromDateSearch, toDateSearch, tenantId , connectionno);
		List<BillFailureNotificationObj>  billDtls =  wSCalculationDao.getFailedBillDtl(tenantId , connectionno);
		List<BillFailureNotificationObj> filterDemand = new ArrayList<BillFailureNotificationObj>();
		if(demands.size()>0 && billDtls.size()>0)
		{
			demands.forEach(passedDemand ->{
				//String passedConsumer = billDtls.stream().filter(d -> d.equals(passedDemand.getConsumerCode())).findAny().orElse(null);
				BillFailureNotificationObj passedConsumer = billDtls.stream().filter(d -> d.getConnectionNo().equals(passedDemand.getConsumerCode())).findAny().orElse(null);
				if(passedConsumer!=null)
				filterDemand.add(passedConsumer);
			}); 
		}
	
		  if(filterDemand.size()>0)
		 {
			 filterDemand.forEach(demandObj ->{
				 BillFailureNotificationRequest billFailureNotificationRequest = new BillFailureNotificationRequest();
				 demandObj.setStatus(WSCalculationConstant.WS_BILL_STATUS_SUCCESS);
				 demandObj.setLastModifiedTime( System.currentTimeMillis());
				 demandObj.setLastModifiedBy(requestInfo.getUserInfo().getName());
				 billFailureNotificationRequest.setBillFailureNotificationObj(demandObj);
				 log.info("Send update msg to ws-failedBill-topic  :"+billFailureNotificationRequest);
				 producer.push(config.getUpdatewsFailedBillTopic(), billFailureNotificationRequest);
			 });
		 } 
	}
	
}
