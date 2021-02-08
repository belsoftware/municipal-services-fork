package org.egov.lams.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.egov.common.contract.request.RequestInfo;
import org.egov.lams.model.MigrationParams;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestInfoWrapper {

	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;
	
	@JsonProperty("migrationParams")
	private MigrationParams migrationParams;
	
}
