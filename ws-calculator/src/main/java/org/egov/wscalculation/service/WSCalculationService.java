package org.egov.wscalculation.service;

import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.web.models.Calculation;
import org.egov.wscalculation.web.models.CalculationReq;

public interface WSCalculationService {

	List<Calculation> getCalculation(CalculationReq calculationReq);

	void jobScheduler();

	void generateDemandBasedOnTimePeriod(RequestInfo requestInfo);
	
	public void generateDemandBasedOnTimePeriod_manual(RequestInfo requestInfo,String tenantId, String connectionno);
	
	public void checkFailedBills(RequestInfo requestInfo,Long fromDateSearch , Long toDateSearch , String tenantId, String connectionno);
	 
}
