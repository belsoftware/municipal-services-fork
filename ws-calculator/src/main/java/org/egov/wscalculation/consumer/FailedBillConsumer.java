package org.egov.wscalculation.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.producer.WSCalculationProducer;
import org.egov.wscalculation.repository.builder.WSCalculatorQueryBuilder;
import org.egov.wscalculation.service.EnrichmentService;
import org.egov.wscalculation.web.models.AuditDetails;
import org.egov.wscalculation.web.models.BillFailureNotificationObj;
import org.egov.wscalculation.web.models.CalculationReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
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

	
	
	@KafkaListener(topics = { "${persister.demand.based.dead.letter.topic.single}" })
	public void listen(final HashMap<String, Object> request, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {	
		
		
		CalculationReq notificationObj;
		BillFailureNotificationObj billFailureNotificationObj;
		try {
			notificationObj = mapper.convertValue(request, CalculationReq.class);			
			billFailureNotificationObj = mapper.convertValue(notificationObj.getCalculationCriteria().get(0),BillFailureNotificationObj.class);
			billFailureNotificationObj.setReason(notificationObj.getReason());
			billFailureNotificationObj.setStatus(WSCalculationConstant.WS_BILL_STATUS_FAIL);		        
			billFailureNotificationObj.setId(UUID.randomUUID().toString());
			billFailureNotificationObj.setCreatedBy(notificationObj.getRequestInfo().getUserInfo().getName());
			
			Long time = System.currentTimeMillis();
			billFailureNotificationObj.setCreatedTime(time);
			
			String myQuery = "SELECT count(*) FROM eg_ws_failed_bill WHERE connectionno='"+billFailureNotificationObj.getConnectionNo()+"' and assessmentyear  ='"+billFailureNotificationObj.getAssessmentYear() +"'";
		
			int result = jdbcTemplate.queryForObject(myQuery, Integer.class);		
		
			if(result == 1) {				
				billFailureNotificationObj.setLastModifiedBy(notificationObj.getRequestInfo().getUserInfo().getName());
				billFailureNotificationObj.setLastModifiedTime(time);
				log.info("Send update msg to ws-failedBill-topic"+billFailureNotificationObj);
				producer.push(config.getWsFailedBillTopic(), billFailureNotificationObj);
			}
			else {
				log.info("Send msg to ws-failedBill-topic"+billFailureNotificationObj);
				producer.push(config.getWsFailedBillTopic(), billFailureNotificationObj);
			}
			
		
		} catch (final Exception e) {
			log.error("Error while listening to value: " + request + " on topic: " + topic + ": " + e);
		}
 	}

}
