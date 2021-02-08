package org.egov.lams.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MigrationParams {


	//Parameters for DSS sycning.
	@JsonProperty("tenantIds")
	private List<String> tenantIds;
	
	@JsonProperty("modules")
	private List<String> modules;
	
	@JsonProperty("fromDate")
    private Long fromDate = null;

    @JsonProperty("toDate")
    private Long toDate = null;
    
	@JsonProperty("indexSuffix")
	private String indexSuffix;
	
	@JsonProperty("useRealIndex")
	private Boolean useRealIndex;
    //Parameters for DSS syncing end.
    

}
