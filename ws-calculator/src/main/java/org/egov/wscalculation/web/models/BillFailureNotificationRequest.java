package org.egov.wscalculation.web.models;

import org.egov.common.contract.request.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillFailureNotificationRequest {
	
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo = null;

	@JsonProperty("BillFailureNotificationObj")
	private BillFailureNotificationObj  billFailureNotificationObj = null;
	
	

}
