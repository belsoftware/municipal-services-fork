package org.egov.wscalculation.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.producer.WSCalculationProducer;
import org.egov.wscalculation.service.MasterDataService;
import org.egov.wscalculation.web.models.BillFailureNotificationObj;
import org.egov.wscalculation.web.models.BillFailureNotificationRequest;
import org.egov.wscalculation.web.models.CalculationCriteria;
import org.egov.wscalculation.web.models.CalculationReq;
import org.egov.wscalculation.web.models.WaterConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FailedBillConsumer {
	
	@Autowired
	private ObjectMapper mapper;
	

	@Autowired
	private WSCalculationProducer producer;
	
	@Autowired
	private WSCalculationConfiguration config;
	

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MasterDataService mDataService;

	@KafkaListener(topics = { "${persister.demand.based.dead.letter.topic.single}" })
	public void listen(final HashMap<String, Object> request, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {	
		
		
		CalculationReq calcReq=null;
		BillFailureNotificationObj notificationObj=null;
		BillFailureNotificationRequest billFailureNotificationRequest = new BillFailureNotificationRequest();
		try {
			
			
			calcReq = mapper.convertValue(request, CalculationReq.class);	
			CalculationCriteria criteria  = calcReq.getCalculationCriteria().get(0);
			WaterConnection conn = criteria.getWaterConnection();
			notificationObj = mapper.convertValue(calcReq.getCalculationCriteria().get(0),BillFailureNotificationObj.class);
			if(!ObjectUtils.isEmpty(conn)) {
				if(conn.getConnectionType().equals(WSCalculationConstant.nonMeterdConnection)) {
					Map<String, Object> masterMap = new HashMap<>();
					mDataService.loadBillingFrequencyMasterData(calcReq.getRequestInfo(), calcReq.getCalculationCriteria().get(0).getTenantId(), masterMap);
					ArrayList<?> billingFrequencyMap = (ArrayList<?>) masterMap
							.get(WSCalculationConstant.Billing_Period_Master);
					
					mDataService.enrichBillingPeriod(calcReq.getCalculationCriteria().get(0), billingFrequencyMap, masterMap);
					Map<String, Object> financialYearMaster =  (Map<String, Object>) masterMap
							.get(WSCalculationConstant.BILLING_PERIOD);
					notificationObj.setFromDate( (Long) financialYearMaster.get(WSCalculationConstant.STARTING_DATE_APPLICABLES));
					notificationObj.setToDate((Long) financialYearMaster.get(WSCalculationConstant.ENDING_DATE_APPLICABLES));
					
				}
			}
			
			notificationObj.setReason(calcReq.getReason());
			notificationObj.setStatus(WSCalculationConstant.WS_BILL_STATUS_FAIL);		        
			notificationObj.setId(UUID.randomUUID().toString());
			notificationObj.setCreatedBy(calcReq.getRequestInfo().getUserInfo().getName());
 
			notificationObj.setCreatedTime(System.currentTimeMillis());
			
			String myQuery = "SELECT count(*) FROM eg_ws_failed_bill WHERE connectionno='"+notificationObj.getConnectionNo()+"' and assessmentyear  ='"+notificationObj.getAssessmentYear() +"' and fromdate="+notificationObj.getFromDate() +" and  todate="+notificationObj.getToDate();
		
			int result = jdbcTemplate.queryForObject(myQuery, Integer.class);
			
			billFailureNotificationRequest.setRequestInfo(calcReq.getRequestInfo());
			//log.info("No of previous failed bills = "+result);
		
			if(result >= 1) {				
				notificationObj.setLastModifiedBy(calcReq.getRequestInfo().getUserInfo().getName());
				notificationObj.setLastModifiedTime(System.currentTimeMillis());
				
				billFailureNotificationRequest.setBillFailureNotificationObj(notificationObj);
				//log.info("Send update msg to ws-failedBill-topic  :"+billFailureNotificationRequest);
				producer.push(config.getUpdatewsFailedBillTopic(), billFailureNotificationRequest);
			}
			else {
				billFailureNotificationRequest.setBillFailureNotificationObj(notificationObj);
				//log.info("Send msg to ws-failedBill-topic   : "+billFailureNotificationRequest);
				producer.push(config.getWsFailedBillTopic(), billFailureNotificationRequest);
			}
			
			
			
		
		} catch (final Exception e) {
			log.error("Error while listening to value: " + request + " on topic: " + topic + ": " + e);
		}
 	}



}
