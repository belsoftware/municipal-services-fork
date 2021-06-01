package org.egov.wscalculation.web.controller;


import java.util.List;

import javax.validation.Valid;

import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.service.DemandService;
import org.egov.wscalculation.service.WSCalculationService;
import org.egov.wscalculation.service.WSCalculationServiceImpl;
import org.egov.wscalculation.util.ResponseInfoFactory;
import org.egov.wscalculation.web.models.AdhocTaxReq;
import org.egov.wscalculation.web.models.BillEstimateResponse;
import org.egov.wscalculation.web.models.BillEstimation;
import org.egov.wscalculation.web.models.Calculation;
import org.egov.wscalculation.web.models.CalculationReq;
import org.egov.wscalculation.web.models.CalculationRes;
import org.egov.wscalculation.web.models.Demand;
import org.egov.wscalculation.web.models.DemandResponse;
import org.egov.wscalculation.web.models.GetBillCriteria;
import org.egov.wscalculation.web.models.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
@Builder
@RestController
@RequestMapping("/waterCalculator")
@Slf4j
public class CalculatorController {
	
	
	@Autowired
	private DemandService demandService;
	
	@Autowired
	private WSCalculationService wSCalculationService;
	
	@Autowired
	private WSCalculationServiceImpl wSCalculationServiceImpl;
	
	@Autowired
	private final ResponseInfoFactory responseInfoFactory;
	@Autowired
	private WSCalculationConfiguration config;
	
	@PostMapping("/_estimate")
	public ResponseEntity<CalculationRes> getTaxEstimation(@RequestBody @Valid CalculationReq calculationReq) {
		List<Calculation> calculations = wSCalculationServiceImpl.getEstimation(calculationReq);
		CalculationRes response = CalculationRes.builder().calculation(calculations)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(calculationReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping("/_estimateBillForConnection")
	public ResponseEntity<BillEstimateResponse>estimateBillForConnection(@RequestBody @Valid CalculationReq calculationReq) {	
		BillEstimation billEstmationResult = wSCalculationServiceImpl.getBillEstimate(calculationReq);	
		BillEstimateResponse response = BillEstimateResponse.builder().billEstimation(billEstmationResult)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(calculationReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	
	@PostMapping("/_calculate")
	public ResponseEntity<CalculationRes> calculate(@RequestBody @Valid CalculationReq calculationReq) {
		List<Calculation> calculations = wSCalculationService.getCalculation(calculationReq);
		CalculationRes response = CalculationRes.builder().calculation(calculations)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(calculationReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping("/_updateDemand")
	public ResponseEntity<DemandResponse> updateDemands(@RequestBody @Valid RequestInfoWrapper requestInfoWrapper,
			@ModelAttribute @Valid GetBillCriteria getBillCriteria) {
		List<Demand> demands = demandService.updateDemands(getBillCriteria, requestInfoWrapper);
		DemandResponse response = DemandResponse.builder().demands(demands)
				.responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@PostMapping("/_jobscheduler")
	public void jobscheduler(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper ) {
		
 		log.info("Water billing job automated schedular started ");
		wSCalculationService.generateDemandBasedOnTimePeriod(requestInfoWrapper.getRequestInfo());
 
	}
	
	@PostMapping("/_jobscheduler_manual")
	public void _jobscheduler_manual(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@RequestParam String   tenantId,
			@RequestParam(required = false) List<String>  connectionnos
			) {
 
			wSCalculationService.generateDemandBasedOnTimePeriod_manual(requestInfoWrapper.getRequestInfo(),tenantId,connectionnos);
	}
	
	@PostMapping("/_jobscheduler_checkFailedBill")
	public void _jobscheduler_checkFailedBill(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@RequestParam String   tenantId,
			@RequestParam(required = false) String  connectionno,
			@RequestParam(required = false) Long  fromDateSearch,
			@RequestParam(required = false) Long  toDateSearch
			) {
		   	//List<Demand> demands = demandService.getDemandForFailedBills( requestInfoWrapper.getRequestInfo() ,  fromDateSearch, toDateSearch, tenantId , connectionno);
			wSCalculationService.checkFailedBills(requestInfoWrapper.getRequestInfo() ,  fromDateSearch, toDateSearch, tenantId , connectionno);
	}
	
	@PostMapping("/_applyAdhocTax")
	public ResponseEntity<CalculationRes> applyAdhocTax(@Valid @RequestBody AdhocTaxReq adhocTaxReq) {
		List<Calculation> calculations = wSCalculationServiceImpl.applyAdhocTax(adhocTaxReq);
		CalculationRes response = CalculationRes.builder().calculation(calculations)
				.responseInfo(responseInfoFactory.createResponseInfoFromRequestInfo(adhocTaxReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);

	}
}
