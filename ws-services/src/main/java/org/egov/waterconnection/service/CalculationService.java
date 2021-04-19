package org.egov.waterconnection.service;

import java.util.Arrays;

import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.web.models.CalculationCriteria;
import org.egov.waterconnection.web.models.CalculationReq;
import org.egov.waterconnection.web.models.CalculationRes;
import org.egov.waterconnection.web.models.Property;
import org.egov.waterconnection.web.models.RequestInfoWrapper;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.repository.ServiceRequestRepository;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CalculationService {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
    
	@Autowired
	private WaterServicesUtil waterServiceUtil;

	@Autowired
	private WSConfiguration config;
	/**
	 * 
	 * @param request
	 * 
	 * If action would be APPROVE_FOR_CONNECTION then
	 * 
	 *Estimate the fee for water application and generate the demand
	 * 
	 */
	public void calculateFeeAndGenerateDemand(WaterConnectionRequest request, Property property) {
		if(WCConstants.APPROVE_CONNECTION_CONST.equalsIgnoreCase(request.getWaterConnection().getProcessInstance().getAction())) {
			CalculationCriteria criteria = CalculationCriteria.builder()
					.applicationNo(request.getWaterConnection().getApplicationNo())
					.waterConnection(request.getWaterConnection())
					.tenantId(property.getTenantId()).build();
			CalculationReq calRequest = CalculationReq.builder().calculationCriteria(Arrays.asList(criteria))
					.requestInfo(request.getRequestInfo()).isconnectionCalculation(false).build();
			try {
				Object response = serviceRequestRepository.fetchResult(waterServiceUtil.getCalculatorURL(), calRequest);
				CalculationRes calResponse = mapper.convertValue(response, CalculationRes.class);
				log.info(mapper.writeValueAsString(calResponse));
			} catch (Exception ex) {
				log.error("Calculation response error!!", ex);
				throw new CustomException("WATER_CALCULATION_EXCEPTION", "Calculation response can not parsed!!!");
			}
		}

	}
	
	public void generateDemandForNonMeterConnection(WaterConnectionRequest request ) {
		if(config.getGenerateDemandForTesting()!=null &&  config.getGenerateDemandForTesting() &&
				WCConstants.ACTIVATE_CONNECTION.equalsIgnoreCase(request.getWaterConnection().getApplicationStatus() )) {
 			try {
 				RequestInfoWrapper requestInfo = RequestInfoWrapper.builder().requestInfo(request.getRequestInfo()).build();
 				String actionLink = waterServiceUtil.getJobSchedularURL().toString()
						.replace("$consumerCode", request.getWaterConnection().getConnectionNo())
						.replace("$tenantId", request.getWaterConnection().getTenantId());
 				log.info("ACTION LINK "+actionLink);
 				serviceRequestRepository.fetchResult(new StringBuilder(actionLink), requestInfo);
			} catch (Exception ex) {
				log.error("generateDemandForNonMeterConnection response error!!", ex);
			}
		}
	}
}
