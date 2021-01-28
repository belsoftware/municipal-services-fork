package org.egov.waterconnection.web.models;



import java.math.BigDecimal;

import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class WsTaxHeads {
	
	@Size(max=64)
    @JsonProperty("id")
    private String id;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("taxHeadCode")
    private String taxHeadCode = null;

    @JsonProperty("amount")
    private BigDecimal amount = null;

    
    @JsonProperty("additionalDetails")
	private Object additionalDetails = null;
    
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails = null;
}
