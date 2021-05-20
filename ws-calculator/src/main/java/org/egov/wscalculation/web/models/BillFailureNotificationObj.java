package org.egov.wscalculation.web.models;

import java.util.Set;

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
public class BillFailureNotificationObj {
	
	@JsonProperty("id")
	private String id = null;
	
	@JsonProperty("connectionNo")
	private String connectionNo = null;

	@JsonProperty("applicationNo")
	private String applicationNo = null;
	
	@JsonProperty("assessmentYear")
	private String assessmentYear = null;
	
	@JsonProperty("lastReading")
	private Double lastReading = null;
	
	
	@JsonProperty("currentReading")
	private Double currentReading = null;
	
	@JsonProperty("fromDate")
	private Long fromDate = null;
	
	@JsonProperty("toDate")
	private Long toDate = null;
	
	@JsonProperty("consumption")
	private Double consumption = null;
	

	@JsonProperty("createdBy")
	private String createdBy;

	@JsonProperty("lastModifiedBy")
	private String lastModifiedBy;

	@JsonProperty("createdTime")
	private Long createdTime;

	@JsonProperty("lastModifiedTime")
	private Long lastModifiedTime;
	
	@JsonProperty("tenantId")
	private String tenantId = null;
	
	@JsonProperty("reason")
	private String reason = null;
	
	@JsonProperty("status")
	private String status = null;
	
}


